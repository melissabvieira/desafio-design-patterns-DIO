# Sistema de Notificações

Projeto acadêmico em Java puro que simula o envio de notificações por e-mail, SMS ou WhatsApp usando um menu interativo no terminal.

## Objetivo

Demonstrar o padrão de projeto **Strategy (Estratégia)**: a aplicação escolhe um comportamento de envio em tempo de execução sem acoplar o serviço de notificação a um canal específico.

## Tecnologias

- Java 17 ou superior
- `Scanner` para leitura das opções e dos dados no terminal
- Nenhum framework, banco de dados, serviço externo ou biblioteca adicional

## Aplicação do padrão Strategy

`Notificacao` define o contrato comum `enviar(destinatario, mensagem)`. `EmailNotificacao`, `SmsNotificacao` e `WhatsAppNotificacao` implementam esse contrato e apresentam a confirmação do canal escolhido.

`ServicoNotificacao` recebe uma implementação de `Notificacao` pelo construtor e delega a ela o envio. A classe `Main` escolhe a estratégia conforme a opção do menu. Assim, o serviço não precisa conhecer os detalhes de cada canal e novas estratégias podem ser adicionadas sem alterar sua lógica.

## Estrutura dos arquivos

```text
.
├── Notificacao.java
├── EmailNotificacao.java
├── SmsNotificacao.java
├── WhatsAppNotificacao.java
├── ServicoNotificacao.java
├── Main.java
└── README.md
```

## Compilar e executar

Abra o terminal na raiz do projeto e execute:

```powershell
New-Item -ItemType Directory -Force out
javac -encoding UTF-8 -d out *.java
java -cp out Main
```

O projeto pode também ser aberto no VS Code com suporte a Java 17 ou superior instalado.

## Exemplo de uso

1. Execute o programa e escolha `1`, `2` ou `3`.
2. Informe o destinatário e o conteúdo da mensagem.
3. Confira a confirmação exibida com o canal usado, o destinatário e a mensagem.
4. O programa retorna ao menu; escolha `4` para sair.

Opções inexistentes são informadas sem encerrar o programa, e campos vazios são solicitados novamente.

> Os envios são apenas simulados. Nenhuma mensagem é transmitida por e-mail, SMS ou WhatsApp.
