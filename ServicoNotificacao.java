public class ServicoNotificacao {
    private final Notificacao notificacao;

    public ServicoNotificacao(Notificacao notificacao) {
        if (notificacao == null) {
            throw new IllegalArgumentException("A estrategia de notificacao nao pode ser nula.");
        }
        this.notificacao = notificacao;
    }

    public void enviar(String destinatario, String mensagem) {
        notificacao.enviar(destinatario, mensagem);
    }
}
