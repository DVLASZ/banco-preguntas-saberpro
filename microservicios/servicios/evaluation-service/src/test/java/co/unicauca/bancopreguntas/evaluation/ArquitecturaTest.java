package co.unicauca.bancopreguntas.evaluation;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.onionArchitecture;

/**
 * Reglas de la arquitectura hexagonal (puertos y adaptadores) con la que se organiza este servicio:
 *
 * <ul>
 *   <li>El dominio (agregado, objetos de valor, política de decisión y eventos) no depende de nada de afuera.</li>
 *   <li>La aplicación implementa los puertos de entrada y usa los de salida; no conoce a los adaptadores.</li>
 *   <li>Los adaptadores de entrada (web y mensajería) llaman a los puertos de entrada, y los de salida (persistencia
 *       y mensajería) implementan los puertos de salida. Ningún adaptador depende de otro.</li>
 * </ul>
 */
@AnalyzeClasses(packagesOf = EvaluationServiceApplication.class, importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaTest {

    private static final String RAIZ = "co.unicauca.bancopreguntas.evaluation";
    private static final String DOMINIO = RAIZ + ".domain..";
    private static final String APLICACION = RAIZ + ".application..";
    private static final String PUERTOS_DE_ENTRADA = RAIZ + ".application.port.in..";
    private static final String PUERTOS_DE_SALIDA = RAIZ + ".application.port.out..";
    private static final String ADAPTADORES_DE_ENTRADA = RAIZ + ".adapter.in..";
    private static final String ADAPTADORES_DE_SALIDA = RAIZ + ".adapter.out..";

    @ArchTest
    static final ArchRule arquitectura_hexagonal =
            onionArchitecture()
                    .domainModels(DOMINIO)
                    .applicationServices(APLICACION)
                    .adapter("web", RAIZ + ".adapter.in.web..")
                    .adapter("mensajeria_de_entrada", RAIZ + ".adapter.in.messaging..")
                    .adapter("mensajeria_de_salida", RAIZ + ".adapter.out.messaging..")
                    .adapter("persistencia", RAIZ + ".adapter.out.persistence..")
                    .withOptionalLayers(true)
                    // la configuración es la raíz de composición: arma los objetos y por eso conoce todas las capas
                    .ignoreDependency(JavaClass.Predicates.resideInAPackage(RAIZ + ".config.."), DescribedPredicate.alwaysTrue())
                    .because("el dominio queda en el centro y los adaptadores solo se conectan por puertos");

    @ArchTest
    static final ArchRule el_dominio_no_depende_de_Spring =
            noClasses().that().resideInAPackage(DOMINIO)
                    .should().dependOnClassesThat().resideInAnyPackage("org.springframework..")
                    .because("el dominio es lógica de negocio pura");

    @ArchTest
    static final ArchRule el_dominio_no_depende_de_la_persistencia_ni_de_la_serializacion =
            noClasses().that().resideInAPackage(DOMINIO)
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "jakarta.persistence..", "jakarta.validation..", "com.fasterxml..", "org.hibernate..",
                            "org.springframework.amqp..")
                    .because("la persistencia y los formatos externos son detalles de los adaptadores");

    @ArchTest
    static final ArchRule la_aplicacion_no_usa_la_persistencia_ni_la_mensajeria_ni_la_web =
            noClasses().that().resideInAPackage(APLICACION)
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "jakarta.persistence..", "org.springframework.data..", "org.springframework.amqp..",
                            "org.springframework.web..");

    @ArchTest
    static final ArchRule los_puertos_son_interfaces =
            classes().that().resideInAnyPackage(PUERTOS_DE_ENTRADA, PUERTOS_DE_SALIDA)
                    .should().beInterfaces()
                    .because("un puerto es un contrato, no una implementación");

    @ArchTest
    static final ArchRule los_adaptadores_de_salida_no_dependen_de_los_de_entrada =
            noClasses().that().resideInAPackage(ADAPTADORES_DE_SALIDA)
                    .should().dependOnClassesThat().resideInAPackage(ADAPTADORES_DE_ENTRADA);
}
