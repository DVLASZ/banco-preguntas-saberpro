# Datos semilla para desarrollo y demostración

El docente confirmó que se trabaja solo con **usuarios precargados**. Los usuarios viven en Keycloak: al arrancar,
Keycloak importa el realm `bancopreguntas` desde
[`microservicios/infraestructura/keycloak/bancopreguntas-realm.json`](../../../microservicios/infraestructura/keycloak/bancopreguntas-realm.json)
y `user-service` copia sus perfiles a `user_db` para poder listar revisores y asignar (`POST /api/users/sync`, además
de la sincronización periódica). Las contraseñas no están en la base de los servicios: las guarda Keycloak con hash
(RNF-08). La clave de los usuarios de demostración está en el archivo del realm y **solo sirve en el entorno local**.

## Usuarios

| Usuario | Nombre | Rol | Correo (simulado) |
|---|---|---|---|
| `admin` | Ada Administradora | ADMIN | admin@bancopreguntas.local |
| `autor1` | Andrés Autor Uno | AUTHOR | autor1@bancopreguntas.local |
| `autor2` | Alicia Autora Dos | AUTHOR | autor2@bancopreguntas.local |
| `revisor1` | Rafael Revisor Uno | REVIEWER | revisor1@bancopreguntas.local |
| `revisor2` | Rocío Revisora Dos | REVIEWER | revisor2@bancopreguntas.local |
| `revisor3` | Ricardo Revisor Tres | REVIEWER | revisor3@bancopreguntas.local |
| `revisor4` | Renata Revisora Cuatro | REVIEWER | revisor4@bancopreguntas.local |
| `revisor5` | Rodrigo Revisor Cinco | REVIEWER | revisor5@bancopreguntas.local |
| `docente1` | Diana Docente Uno | TEACHER | docente1@bancopreguntas.local |
| `estudiante1` | Esteban Estudiante Uno | STUDENT | estudiante1@bancopreguntas.local |
| `estudiante2` | Elena Estudiante Dos | STUDENT | estudiante2@bancopreguntas.local |
| `estudiante3` | Emilio Estudiante Tres | STUDENT | estudiante3@bancopreguntas.local |

Los roles Docente y Estudiante pertenecen a los simulacros y al seguimiento académico (RF-21 a RF-28), que los docentes
ubicaron en el tercer corte. Los usuarios existen en el realm, pero esta entrega no los usa.

## Clientes de Keycloak

| Cliente | Para qué |
|---|---|
| `bancopreguntas-web` | El frontend Angular: *Authorization Code* con PKCE. El flujo de contraseña está habilitado solo para las pruebas locales (scripts y Postman) |
| `bancopreguntas-admin` | Cuenta de servicio (*client credentials*) que usa `user-service` para leer usuarios y eventos de acceso; su secreto es `KEYCLOAK_ADMIN_CLIENT_SECRET` |

## Preguntas

No se precargan preguntas. Los listados se prueban creando una con el formulario del frontend, con Swagger o con la
colección de Postman (`docs/corte-2/api/postman`), y recorriendo el flujo completo (enviar, asignar, evaluar,
publicar). Así el historial, las rondas y las notificaciones se generan por los mismos caminos que en producción y
no hay datos que se contradigan.

El recorrido funcional automatizado contra la pila de Docker
([`microservicios/pruebas-funcionales`](../../../microservicios/pruebas-funcionales/README.md)) crea sus propias preguntas
con estos usuarios y comprueba las historias de usuario de punta a punta.
