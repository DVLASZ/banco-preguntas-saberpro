package co.unicauca.bancopreguntas.notification.domain;

/** Un correo electrónico listo para enviar. */
public record Correo(String para, String asunto, String cuerpo) {

    public Correo {
        if (para == null || para.isBlank()) {
            throw new IllegalArgumentException("El destinatario del correo es obligatorio");
        }
    }
}
