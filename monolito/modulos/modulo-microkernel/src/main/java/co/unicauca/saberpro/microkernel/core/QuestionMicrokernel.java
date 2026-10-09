package co.unicauca.saberpro.microkernel.core;

import co.unicauca.saberpro.microkernel.common.entities.QuestionRequest;
import co.unicauca.saberpro.microkernel.common.interfaces.QuestionPlugin;
import co.unicauca.saberpro.preguntas.domain.Question;
import co.unicauca.saberpro.preguntas.domain.QuestionService;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Núcleo del patrón Microkernel (Taller 5): administra el banco de
 * preguntas en un {@code Map} (tal como pide la guía del taller) y
 * registra/ejecuta dinámicamente los plugins, cargados por
 * <b>Reflexión</b> desde {@code plugins.properties} — igual que el
 * ejemplo visto en clase del envío de paquetes a distintos países
 * ({@code DeliveryPluginManager}).
 * <p>
 * A diferencia del ejemplo del taller (que usa una entidad {@code Question}
 * de juguete), este núcleo usa la {@link Question} real del proyecto y
 * registra cada pregunta generada a través del {@link QuestionService}
 * compartido con el resto de la aplicación (Autor, Revisor, Docente...):
 * así no queda un banco de preguntas separado, y las vistas de
 * estadísticas/gráfica (patrón Observer) también se refrescan cuando un
 * plugin genera una pregunta.
 */
public class QuestionMicrokernel {

    private static final String ARCHIVO_PLUGINS = "plugins.properties";

    private final QuestionService questionService;
    private final Map<String, Question> questions = new LinkedHashMap<>();
    private final List<QuestionPlugin> plugins = new ArrayList<>();

    public QuestionMicrokernel(QuestionService questionService) {
        this.questionService = questionService;
        for (Question pregunta : questionService.listarPreguntas()) {
            questions.put(pregunta.getId(), pregunta);
        }
        cargarPluginsPorReflexion();
    }

    private void cargarPluginsPorReflexion() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(ARCHIVO_PLUGINS)) {
            if (input == null) {
                throw new IllegalStateException("No se encontró " + ARCHIVO_PLUGINS + " en el classpath");
            }
            Properties propiedades = new Properties();
            propiedades.load(input);

            for (String clave : propiedades.stringPropertyNames()) {
                String nombreClase = propiedades.getProperty(clave);
                try {
                    // Uso obligatorio de Reflexión para instanciar cada plugin dinámicamente.
                    Class<?> clase = Class.forName(nombreClase);
                    Object instancia = clase.getDeclaredConstructor().newInstance();
                    plugins.add((QuestionPlugin) instancia);
                } catch (ReflectiveOperationException | ClassCastException ex) {
                    throw new IllegalStateException(
                            "No se pudo cargar el plugin '" + clave + "' (" + nombreClase + ") vía reflexión", ex);
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Error al leer " + ARCHIVO_PLUGINS, ex);
        }
    }

    /** Nombres de los plugins actualmente registrados (para poblar, por ejemplo, un combo en la interfaz). */
    public List<String> listarPlugins() {
        List<String> nombres = new ArrayList<>();
        for (QuestionPlugin plugin : plugins) {
            nombres.add(plugin.getName());
        }
        return nombres;
    }

    /**
     * Busca el primer plugin registrado que soporte el tipo solicitado y le
     * pide generar la pregunta. Si la genera (pasó el pipeline de
     * validación), la agrega tanto al mapa del núcleo como al banco de
     * preguntas real.
     *
     * @return la pregunta generada, o {@code null} si la solicitud no pasó
     *         la validación del plugin.
     * @throws IllegalArgumentException si ningún plugin soporta ese tipo.
     */
    public Question executePlugin(String type, QuestionRequest request) {
        for (QuestionPlugin plugin : plugins) {
            if (plugin.supports(type)) {
                Question pregunta = plugin.generate(request);
                if (pregunta == null) {
                    return null;
                }
                questionService.registrarPreguntaGenerada(pregunta);
                questions.put(pregunta.getId(), pregunta);
                return pregunta;
            }
        }
        throw new IllegalArgumentException("No hay ningún plugin registrado que soporte el tipo: " + type);
    }

    public Map<String, Question> getQuestions() {
        return questions;
    }
}
