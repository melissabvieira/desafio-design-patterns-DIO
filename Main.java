import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            boolean executando = true;

            while (executando) {
                exibirMenu();
                System.out.print("Escolha uma opcao: ");
                if (!scanner.hasNextLine()) {
                    System.out.println();
                    System.out.println("Programa encerrado.");
                    break;
                }
                String opcao = scanner.nextLine().trim();

                switch (opcao) {
                    case "1":
                        enviarNotificacao(scanner, new EmailNotificacao());
                        break;
                    case "2":
                        enviarNotificacao(scanner, new SmsNotificacao());
                        break;
                    case "3":
                        enviarNotificacao(scanner, new WhatsAppNotificacao());
                        break;
                    case "4":
                        executando = false;
                        System.out.println("Programa encerrado.");
                        break;
                    default:
                        System.out.println("Opcao invalida. Escolha uma opcao de 1 a 4.");
                }

                if (executando && !opcao.equals("4")) {
                    System.out.println();
                }
            }
        }
    }

    private static void exibirMenu() {
        System.out.println("=== SISTEMA DE NOTIFICACOES ===");
        System.out.println();
        System.out.println("1 - Enviar por E-mail");
        System.out.println("2 - Enviar por SMS");
        System.out.println("3 - Enviar por WhatsApp");
        System.out.println("4 - Sair");
        System.out.println();
    }

    private static void enviarNotificacao(Scanner scanner, Notificacao estrategia) {
        String destinatario = lerCampoObrigatorio(scanner, "Digite o destinatario: ");
        if (destinatario == null) {
            return;
        }

        String mensagem = lerCampoObrigatorio(scanner, "Digite a mensagem: ");
        if (mensagem == null) {
            return;
        }

        ServicoNotificacao servico = new ServicoNotificacao(estrategia);
        servico.enviar(destinatario, mensagem);
        System.out.println();
        System.out.println("Voltando ao menu principal...");
    }

    private static String lerCampoObrigatorio(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            if (!scanner.hasNextLine()) {
                System.out.println();
                return null;
            }
            String valor = scanner.nextLine();

            if (!valor.isBlank()) {
                return valor.trim();
            }

            System.out.println("Este campo nao pode ficar vazio. Tente novamente.");
        }
    }
}
