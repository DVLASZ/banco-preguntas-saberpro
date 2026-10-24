package co.unicauca.bancopreguntas.platform.security;

/** Roles del sistema: administrador, autor y revisor (banco de preguntas), y docente y estudiante (simulacros y seguimiento). */
public final class Roles {

    public static final String ADMIN = "ADMIN";
    public static final String AUTHOR = "AUTHOR";
    public static final String REVIEWER = "REVIEWER";
    public static final String TEACHER = "TEACHER";
    public static final String STUDENT = "STUDENT";

    private Roles() {
    }
}
