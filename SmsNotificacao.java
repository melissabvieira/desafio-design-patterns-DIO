public class SmsNotificacao implements Notificacao {
    @Override
    public void enviar(String destinatario, String mensagem) {
        System.out.println();
        System.out.println("[SMS] Notificacao enviada com sucesso!");
        System.out.println("Destinatario: " + destinatario);
        System.out.println("Mensagem: " + mensagem);
    }
}
