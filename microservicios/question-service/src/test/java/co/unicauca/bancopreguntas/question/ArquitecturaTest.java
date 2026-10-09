package co.unicauca.bancopreguntas.question;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Reglas de arquitectura: protegen que el dominio siga limpio, de modo que el paso a arquitectura hexagonal
 * del tercer corte sea un refinamiento y no una reescritura.
 */
@AnalyzeClasses(packagesOf = QuestionServiceApplication.class, importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaTest {

    private static final String DOMINIO = "co.unicauca.bancopreguntas.question.domain..";
    private static final String APLICACION = "co.unicauca.bancopreguntas.question.application..";
    private static final String INFRAESTRUCTURA = "co.unicauca.bancopreguntas.question.infrastructure..";
    private static final String API = "co.unicauca.bancopreguntas.question.api..";

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
    static final ArchRule el_dominio_no_depende_de_las_otras_capas =
            noClasses().that().resideInAPackage(DOMINIO)
                    .should().dependOnClassesThat().resideInAnyPackage(APLICACION, INFRAESTRUCTURA, API);

    @ArchTest
    static final ArchRule la_aplicacion_no_depende_de_los_adaptadores =
            noClasses().that().resideInAPackage(APLICACION)
                    .should().dependOnClassesThat().resideInAnyPackage(INFRAESTRUCTURA, API)
                    .because("la aplicación solo conoce los puertos");

    @ArchTest
    static final ArchRule la_aplicacion_no_usa_la_persistencia_ni_la_mensajeria =
            noClasses().that().resideInAPackage(APLICACION)
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "jakarta.persistence..", "org.springframework.data..", "org.springframework.amqp..",
                            "org.springframework.web..");

    @ArchTest
    static final ArchRule la_infraestructura_no_depende_del_api =
            noClasses().that().resideInAPackage(INFRAESTRUCTURA)
                    .should().dependOnClassesThat().resideInAPackage(API);

    @ArchTest
    static final ArchRule el_api_no_toca_la_infraestructura =
            noClasses().that().resideInAPackage(API)
                    .should().dependOnClassesThat().resideInAPackage(INFRAESTRUCTURA)
                    .because("el API habla con los casos de uso, no con los adaptadores");
}
