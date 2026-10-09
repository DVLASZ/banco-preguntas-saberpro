package co.unicauca.bancopreguntas.platform.security;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.web.method.HandlerMethod;

import java.util.Arrays;

/**
 * En Swagger, las operaciones que reciben un {@link AuthenticatedUser} muestran las cabeceras de identidad
 * como parámetros, para poder probarlas desde la interfaz mientras no pasen por el gateway.
 */
public class UserHeadersOpenApiCustomizer implements OperationCustomizer {

    private final boolean mostrarCabeceras;

    public UserHeadersOpenApiCustomizer() {
        this(true);
    }

    /** @param mostrarCabeceras si es {@code false} (modo jwt) el usuario sale del token y no se piden cabeceras */
    public UserHeadersOpenApiCustomizer(boolean mostrarCabeceras) {
        this.mostrarCabeceras = mostrarCabeceras;
    }

    static {
        SpringDocUtils.getConfig().addRequestWrapperToIgnore(AuthenticatedUser.class);
    }

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        boolean usaUsuario = Arrays.stream(handlerMethod.getMethodParameters())
                .anyMatch(p -> AuthenticatedUser.class.equals(p.getParameterType()));
        if (usaUsuario && mostrarCabeceras) {
            operation.addParametersItem(new HeaderParameter()
                    .name(HeaderUserArgumentResolver.HEADER_USER)
                    .description("Identificador del usuario (lo propaga el gateway tras validar el token)")
                    .required(true));
            operation.addParametersItem(new HeaderParameter()
                    .name(HeaderUserArgumentResolver.HEADER_ROLES)
                    .description("Roles separados por comas: ADMIN, AUTHOR, REVIEWER")
                    .required(false));
        }
        return operation;
    }
}
