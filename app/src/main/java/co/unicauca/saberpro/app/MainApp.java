package co.unicauca.saberpro.app;

import co.unicauca.saberpro.preguntas.access.QuestionImplRepository;
import co.unicauca.saberpro.preguntas.domain.QuestionRepository;
import co.unicauca.saberpro.preguntas.domain.QuestionService;
import co.unicauca.saberpro.preguntas.presentation.GUIObserver1;
import co.unicauca.saberpro.preguntas.presentation.GUIObserver2;
import co.unicauca.saberpro.preguntas.presentation.GUIQuestions;
import co.unicauca.saberpro.preguntas.presentation.GUIRevisor;
import co.unicauca.saberpro.preguntas.simulacro.access.SimulacroImplRepository;
import co.unicauca.saberpro.preguntas.simulacro.domain.SimulacroRepository;
import co.unicauca.saberpro.preguntas.simulacro.domain.SimulacroService;
import co.unicauca.saberpro.preguntas.simulacro.presentation.GUIDocente;
import co.unicauca.saberpro.preguntas.simulacro.presentation.GUIEstudiante;
import co.unicauca.saberpro.usuarios.domain.Role;
import co.unicauca.saberpro.usuarios.domain.UserStatus;
import co.unicauca.saberpro.usuarios.domain.access.IUserRepository;
import co.unicauca.saberpro.usuarios.domain.access.UserRepositoryFactory;
import co.unicauca.saberpro.usuarios.domain.menu.MenuProviderRegistry;
import co.unicauca.saberpro.usuarios.domain.security.IPasswordHasher;
import co.unicauca.saberpro.usuarios.domain.security.PasswordHasherFactory;
import co.unicauca.saberpro.usuarios.domain.service.UserService;
import co.unicauca.saberpro.usuarios.domain.validation.IPasswordPolicy;
import co.unicauca.saberpro.usuarios.domain.validation.PasswordPolicyFactory;
import co.unicauca.saberpro.usuarios.ui.DashboardFrame;
import co.unicauca.saberpro.usuarios.ui.LoginFrame;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.Color;

/**
 * Composition root de la aplicación fusionada Taller 2 (usuarios/login) +
 * Taller 4 (capas/MVC/Observer sobre Gestión de Preguntas): arma ambos
 * módulos, registra las vistas observadoras y — la pieza que los une —
 * define el {@code SesionRouter} que, tras un login exitoso, abre la
 * ventana Swing correspondiente al rol autenticado.
 */
public class MainApp {

    private static final String CONTRASENA_DEMO = "Saber2026!";

    public static void main(String[] args) {
        aplicarApariencia();
        SwingUtilities.invokeLater(() -> {
            // --- Módulo de preguntas (Taller 4) ---
            QuestionRepository repository = new QuestionImplRepository();
            QuestionService questionService = new QuestionService(repository);

            // --- Módulo de simulacros (Docente genera, Estudiante presenta) ---
            SimulacroRepository simulacroRepository = new SimulacroImplRepository();
            SimulacroService simulacroService = new SimulacroService(questionService, simulacroRepository);

            // Las vistas de estadísticas y gráfica son un reporte agregado de
            // todo el banco de preguntas (HU-17): eso es competencia del
            // Administrador, no de todos los roles — por eso se crean y se
            // suscriben como observadoras desde ya (para que no se pierdan
            // notificaciones), pero solo se muestran más abajo si el rol
            // autenticado es ADMINISTRADOR, en vez de abrirse siempre.
            GUIObserver1 vistaEstadisticas = new GUIObserver1(questionService);
            GUIObserver2 vistaGrafica = new GUIObserver2(questionService);
            questionService.agregarObservador(vistaEstadisticas);
            questionService.agregarObservador(vistaGrafica);
            vistaEstadisticas.setLocation(480, 40);
            vistaGrafica.setLocation(480, 280);

            // --- Módulo de usuarios (Taller 2) ---
            IUserRepository userRepository = UserRepositoryFactory.getInstance().getRepository("default");
            IPasswordHasher passwordHasher = PasswordHasherFactory.getInstance().getHasher("default");
            IPasswordPolicy passwordPolicy = PasswordPolicyFactory.getInstance().getPolicy("default");
            UserService userService = new UserService(userRepository, passwordHasher, passwordPolicy);
            MenuProviderRegistry menuProviderRegistry = MenuProviderRegistry.withDefaultProviders();
            sembrarUsuariosDemo(userService);

            // --- El puente entre ambos: qué ventana abrir según el rol ---
            new LoginFrame(userService, user -> {
                switch (user.getRole()) {
                    case AUTOR_PREGUNTAS -> new GUIQuestions(questionService).setVisible(true);
                    case REVISOR -> new GUIRevisor(questionService).setVisible(true);
                    case DOCENTE -> new GUIDocente(simulacroService).setVisible(true);
                    case ESTUDIANTE -> new GUIEstudiante(user, simulacroService).setVisible(true);
                    case ADMINISTRADOR -> {
                        new DashboardFrame(user, menuProviderRegistry).setVisible(true);
                        vistaEstadisticas.setVisible(true);
                        vistaGrafica.setVisible(true);
                    }
                    default -> new DashboardFrame(user, menuProviderRegistry).setVisible(true);
                }
            }).setVisible(true);
        });
    }

    /**
     * Crea un usuario de ejemplo por cada rol la primera vez que se
     * ejecuta la aplicación (base de datos vacía), para poder probar el
     * login sin tener que registrarse manualmente primero.
     */
    private static void sembrarUsuariosDemo(UserService userService) {
        if (!userService.listUsers().isEmpty()) {
            return;
        }
        userService.register("autor1", "Ana Autora", Role.AUTOR_PREGUNTAS, UserStatus.ACTIVO, CONTRASENA_DEMO);
        userService.register("revisor1", "Roberto Revisor", Role.REVISOR, UserStatus.ACTIVO, CONTRASENA_DEMO);
        userService.register("docente1", "Diana Docente", Role.DOCENTE, UserStatus.ACTIVO, CONTRASENA_DEMO);
        userService.register("estudiante1", "Esteban Estudiante", Role.ESTUDIANTE, UserStatus.ACTIVO, CONTRASENA_DEMO);
        userService.register("admin1", "Alicia Administradora", Role.ADMINISTRADOR, UserStatus.ACTIVO, CONTRASENA_DEMO);
    }

    /** Aplica un look and feel moderno (FlatLaf) con un acento propio del proyecto. */
    private static void aplicarApariencia() {
        UIManager.put("Component.accentColor", new Color(0x2563EB));
        UIManager.put("Component.focusColor", new Color(0x2563EB));
        UIManager.put("Component.arc", 8);
        UIManager.put("Button.arc", 10);
        UIManager.put("TextComponent.arc", 8);
        FlatLightLaf.setup();
    }
}
