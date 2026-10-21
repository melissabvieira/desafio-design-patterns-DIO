public class WhatsAppNotificacao implements Notificacao {
    @Override
    public void enviar(String destinatario, String mensagem) {
        System.out.println();
        System.out.println("[WHATSAPP] Notificacao enviada com sucesso!");
    }
}
