import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

public final class ServidorNotificacoes {
    private static final int PORTA = 8080;
    private static final int TAMANHO_MAXIMO_CORPO = 16_384;
    private static final int TAMANHO_MAXIMO_MENSAGEM = 1_000;
    private static final Path DIRETORIO_WEB = Paths.get("web").toAbsolutePath().normalize();

    private ServidorNotificacoes() {
    }

    public static void iniciar() throws IOException {
        if (!Files.isDirectory(DIRETORIO_WEB)) {
            throw new IOException("A pasta web nao foi encontrada em " + DIRETORIO_WEB);
        }

        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", PORTA), 0);
        servidor.createContext("/api/health", ServidorNotificacoes::tratarSaude);
        servidor.createContext("/api/notificacoes", ServidorNotificacoes::tratarNotificacao);
        servidor.createContext("/", ServidorNotificacoes::servirArquivo);
        servidor.setExecutor(Executors.newFixedThreadPool(8));
        servidor.start();
        System.out.println("Servidor iniciado em http://localhost:" + PORTA);
    }

    private static void tratarSaude(HttpExchange exchange) throws IOException {
        try {
            if (!"/api/health".equals(exchange.getRequestURI().getPath())) {
                responderJson(exchange, 404, "{\"sucesso\":false,\"mensagem\":\"Endpoint nao encontrado.\"}");
            } else if (!"GET".equals(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Allow", "GET");
                responderJson(exchange, 405, "{\"sucesso\":false,\"mensagem\":\"Metodo nao permitido.\"}");
            } else {
                responderJson(exchange, 200, "{\"sucesso\":true,\"status\":\"ok\"}");
            }
        } finally {
            exchange.close();
        }
    }

    private static void tratarNotificacao(HttpExchange exchange) throws IOException {
        try {
            if (!"/api/notificacoes".equals(exchange.getRequestURI().getPath())) {
                responderJson(exchange, 404, "{\"sucesso\":false,\"mensagem\":\"Endpoint nao encontrado.\"}");
                return;
            }
            if (!"POST".equals(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Allow", "POST");
                responderJson(exchange, 405, "{\"sucesso\":false,\"mensagem\":\"Metodo nao permitido.\"}");
                return;
            }

            Map<String, String> dados;
            try {
                dados = JsonObjetoStringParser.parse(exchange.getRequestBody().readNBytes(TAMANHO_MAXIMO_CORPO + 1));
            } catch (IllegalArgumentException e) {
                responderJson(exchange, 400, respostaErro(e.getMessage()));
                return;
            }

            if (dados.size() > 3) {
                responderJson(exchange, 400, respostaErro("A requisicao contem campos nao suportados."));
                return;
            }

            String canal = campoObrigatorio(dados, "canal");
            String destinatario = campoObrigatorio(dados, "destinatario").trim();
            String mensagem = campoObrigatorio(dados, "mensagem");
            if (mensagem.length() > TAMANHO_MAXIMO_MENSAGEM) {
                responderJson(exchange, 400, respostaErro("A mensagem deve ter no maximo 1000 caracteres."));
                return;
            }

            Notificacao estrategia = estrategiaPara(canal);
            if (estrategia == null) {
                responderJson(exchange, 400, respostaErro("Canal nao suportado. Use EMAIL, SMS ou WHATSAPP."));
                return;
            }

            new ServicoNotificacao(estrategia).enviar(destinatario, mensagem);
            responderJson(exchange, 200, "{\"sucesso\":true,\"canal\":\""
                    + escaparJson(canal.trim().toUpperCase(Locale.ROOT))
                    + "\",\"destinatario\":\"" + escaparJson(destinatario)
                    + "\",\"mensagem\":\"Notificacao enviada com sucesso.\"}");
        } catch (IllegalArgumentException e) {
            responderJson(exchange, 400, respostaErro(e.getMessage()));
        } catch (Exception e) {
            System.err.println("Erro interno ao processar requisicao de notificacao.");
            responderJson(exchange, 500, respostaErro("Ocorreu um erro interno ao processar a notificacao."));
        } finally {
            exchange.close();
        }
    }

    private static void servirArquivo(HttpExchange exchange) throws IOException {
        try {
            if (!"GET".equals(exchange.getRequestMethod()) && !"HEAD".equals(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Allow", "GET, HEAD");
                responderTexto(exchange, 405, "Metodo nao permitido.", "text/plain; charset=UTF-8");
                return;
            }

            String caminho = exchange.getRequestURI().getPath();
            if ("/".equals(caminho)) {
                caminho = "/index.html";
            }

            Path arquivo;
            try {
                arquivo = DIRETORIO_WEB.resolve(caminho.substring(1)).normalize();
            } catch (InvalidPathException e) {
                responderTexto(exchange, 400, "Caminho invalido.", "text/plain; charset=UTF-8");
                return;
            }
            if (!arquivo.startsWith(DIRETORIO_WEB) || !Files.isRegularFile(arquivo)) {
                responderTexto(exchange, 404, "Arquivo nao encontrado.", "text/plain; charset=UTF-8");
                return;
            }

            byte[] conteudo = Files.readAllBytes(arquivo);
            exchange.getResponseHeaders().set("Content-Type", tipoConteudo(arquivo));
            exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
            if ("HEAD".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
            } else {
                exchange.sendResponseHeaders(200, conteudo.length);
                exchange.getResponseBody().write(conteudo);
            }
        } finally {
            exchange.close();
        }
    }

    private static String campoObrigatorio(Map<String, String> dados, String nome) {
        String valor = dados.get(nome);
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("O campo " + nome + " e obrigatorio.");
        }
        return valor;
    }

    private static Notificacao estrategiaPara(String canal) {
        switch (canal.trim().toUpperCase(Locale.ROOT)) {
            case "EMAIL":
                return new EmailNotificacao();
            case "SMS":
                return new SmsNotificacao();
            case "WHATSAPP":
                return new WhatsAppNotificacao();
            default:
                return null;
        }
    }

    private static String respostaErro(String mensagem) {
        return "{\"sucesso\":false,\"mensagem\":\"" + escaparJson(mensagem) + "\"}";
    }

    private static String escaparJson(String valor) {
        StringBuilder resultado = new StringBuilder();
        for (char caractere : valor.toCharArray()) {
            switch (caractere) {
                case '"':
                    resultado.append("\\\"");
                    break;
                case '\\':
                    resultado.append("\\\\");
                    break;
                case '\b':
                    resultado.append("\\b");
                    break;
                case '\f':
                    resultado.append("\\f");
                    break;
                case '\n':
                    resultado.append("\\n");
                    break;
                case '\r':
                    resultado.append("\\r");
                    break;
                case '\t':
                    resultado.append("\\t");
                    break;
                default:
                    if (caractere < 0x20) {
                        resultado.append(String.format("\\u%04x", (int) caractere));
                    } else {
                        resultado.append(caractere);
                    }
            }
        }
        return resultado.toString();
    }

    private static void responderJson(HttpExchange exchange, int status, String json) throws IOException {
        responder(exchange, status, json.getBytes(StandardCharsets.UTF_8), "application/json; charset=UTF-8");
    }

    private static void responderTexto(HttpExchange exchange, int status, String texto, String tipo) throws IOException {
        responder(exchange, status, texto.getBytes(StandardCharsets.UTF_8), tipo);
    }

    private static void responder(HttpExchange exchange, int status, byte[] corpo, String tipo) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", tipo);
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.sendResponseHeaders(status, corpo.length);
        exchange.getResponseBody().write(corpo);
    }

    private static String tipoConteudo(Path arquivo) {
        String nome = arquivo.getFileName().toString().toLowerCase(Locale.ROOT);
        if (nome.endsWith(".html")) {
            return "text/html; charset=UTF-8";
        }
        if (nome.endsWith(".css")) {
            return "text/css; charset=UTF-8";
        }
        if (nome.endsWith(".js")) {
            return "text/javascript; charset=UTF-8";
        }
        if (nome.endsWith(".svg")) {
            return "image/svg+xml";
        }
        return "application/octet-stream";
    }

    private static final class JsonObjetoStringParser {
        private final String texto;
        private int indice;

        private JsonObjetoStringParser(String texto) {
            this.texto = texto;
        }

        static Map<String, String> parse(byte[] corpo) {
            if (corpo.length > TAMANHO_MAXIMO_CORPO) {
                throw new IllegalArgumentException("A requisicao excede o tamanho permitido.");
            }
            String texto = new String(corpo, StandardCharsets.UTF_8);
            JsonObjetoStringParser parser = new JsonObjetoStringParser(texto);
            return parser.parseObjeto();
        }

        private Map<String, String> parseObjeto() {
            Map<String, String> resultado = new HashMap<>();
            ignorarEspacos();
            exigir('{');
            ignorarEspacos();
            if (consumir('}')) {
                exigirFim();
                return resultado;
            }

            while (true) {
                ignorarEspacos();
                String chave = lerString();
                ignorarEspacos();
                exigir(':');
                ignorarEspacos();
                String valor = lerString();
                if (resultado.putIfAbsent(chave, valor) != null) {
                    throw erro("Campo JSON duplicado.");
                }
                ignorarEspacos();
                if (consumir('}')) {
                    exigirFim();
                    return resultado;
                }
                exigir(',');
            }
        }

        private String lerString() {
            exigir('"');
            StringBuilder resultado = new StringBuilder();
            while (indice < texto.length()) {
                char caractere = texto.charAt(indice++);
                if (caractere == '"') {
                    return resultado.toString();
                }
                if (caractere < 0x20) {
                    throw erro("Caractere de controle invalido em string JSON.");
                }
                if (caractere != '\\') {
                    resultado.append(caractere);
                    continue;
                }
                if (indice >= texto.length()) {
                    throw erro("Escape JSON incompleto.");
                }
                char escape = texto.charAt(indice++);
                switch (escape) {
                    case '"':
                    case '\\':
                    case '/':
                        resultado.append(escape);
                        break;
                    case 'b':
                        resultado.append('\b');
                        break;
                    case 'f':
                        resultado.append('\f');
                        break;
                    case 'n':
                        resultado.append('\n');
                        break;
                    case 'r':
                        resultado.append('\r');
                        break;
                    case 't':
                        resultado.append('\t');
                        break;
                    case 'u':
                        resultado.append(lerUnicode());
                        break;
                    default:
                        throw erro("Escape JSON invalido.");
                }
            }
            throw erro("String JSON nao terminada.");
        }

        private char lerUnicode() {
            if (indice + 4 > texto.length()) {
                throw erro("Escape Unicode incompleto.");
            }
            int valor = 0;
            for (int i = 0; i < 4; i++) {
                int digito = Character.digit(texto.charAt(indice++), 16);
                if (digito < 0) {
                    throw erro("Escape Unicode invalido.");
                }
                valor = valor * 16 + digito;
            }
            return (char) valor;
        }

        private void exigir(char esperado) {
            if (!consumir(esperado)) {
                throw erro("JSON malformado.");
            }
        }

        private boolean consumir(char esperado) {
            if (indice < texto.length() && texto.charAt(indice) == esperado) {
                indice++;
                return true;
            }
            return false;
        }

        private void ignorarEspacos() {
            while (indice < texto.length()) {
                char caractere = texto.charAt(indice);
                if (caractere != ' ' && caractere != '\t' && caractere != '\r' && caractere != '\n') {
                    return;
                }
                indice++;
            }
        }

        private void exigirFim() {
            ignorarEspacos();
            if (indice != texto.length()) {
                throw erro("Conteudo extra apos o objeto JSON.");
            }
        }

        private IllegalArgumentException erro(String mensagem) {
            return new IllegalArgumentException(mensagem);
        }
    }
}
