# Sistema de Notificações

Projeto acadêmico em Java puro que disponibiliza uma interface web para simular notificações por e-mail, SMS ou WhatsApp. A aplicação usa o servidor HTTP nativo do Java e não exige frameworks, banco de dados, Node.js ou bibliotecas externas.

## Objetivo acadêmico

Demonstrar o padrão de projeto **Strategy (Estratégia)** em uma aplicação integrada: o canal é selecionado durante a requisição, e o serviço delega o envio à estratégia correspondente sem conhecer os detalhes do canal.

## Tecnologias

- Java 17 ou superior, incluindo `com.sun.net.httpserver.HttpServer`
- HTML5, CSS3 e JavaScript puro com Fetch API
- Sem banco de dados, frameworks ou dependências externas

## Padrão Strategy

`Notificacao` define o contrato `enviar(destinatario, mensagem)`. `EmailNotificacao`, `SmsNotificacao` e `WhatsAppNotificacao` implementam esse contrato. O endpoint seleciona uma dessas estratégias e injeta a implementação em `ServicoNotificacao`, que delega o envio. Assim, a camada HTTP não duplica a lógica de envio, e o serviço permanece desacoplado dos canais.

## Estrutura

```text
.
├── EmailNotificacao.java
├── Main.java
├── Notificacao.java
├── ServicoNotificacao.java
├── ServidorNotificacoes.java
├── SmsNotificacao.java
├── WhatsAppNotificacao.java
├── web/
│   ├── index.html
│   ├── script.js
│   └── style.css
└── README.md
```

## Compilar e executar

Requisitos: JDK 17 ou superior instalado. Abra PowerShell ou o terminal integrado do VS Code na raiz do projeto e execute:

```powershell
New-Item -ItemType Directory -Force out
javac -encoding UTF-8 -d out *.java
java -cp out Main
```

Quando o terminal indicar que o servidor iniciou, acesse <http://localhost:8080>. O processo permanece ativo enquanto o servidor estiver em execução; use `Ctrl+C` para encerrá-lo. A porta padrão é `8080` e o servidor atende apenas conexões locais.

O menu de terminal original também pode ser executado com `java -cp out Main --cli`.

## Endpoints

### `GET /api/health`

Verifica se o servidor está disponível. Retorna `200` e, por exemplo:

```json
{"sucesso":true,"status":"ok"}
```

### `POST /api/notificacoes`

Aceita `canal` (`EMAIL`, `SMS` ou `WHATSAPP`), `destinatario` e `mensagem` em JSON. Campos obrigatórios não podem estar vazios e a mensagem aceita até 1.000 caracteres.

Exemplo no PowerShell:

```powershell
$body = @{
  canal = "EMAIL"
  destinatario = "cliente@exemplo.com"
  mensagem = "Seu chamado foi atualizado."
} | ConvertTo-Json

Invoke-RestMethod -Method Post `
  -Uri http://localhost:8080/api/notificacoes `
  -ContentType "application/json; charset=UTF-8" `
  -Body $body
```

Sucesso retorna `200`:

```json
{"sucesso":true,"canal":"EMAIL","destinatario":"cliente@exemplo.com","mensagem":"Notificacao enviada com sucesso."}
```

JSON malformado, campos vazios ou canal não suportado retornam `400`; métodos não permitidos retornam `405`; falhas internas retornam `500`. A API e os arquivos da interface são servidos na mesma origem.

> Todos os envios são simulados. Nenhum e-mail, SMS ou WhatsApp real é transmitido.
