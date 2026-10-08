## A. Frontend y monolito

1. ¿El frontend debe ser una aplicación web (React, Angular, Vue u otra) o vale JavaFX o Swing consumiendo la API REST? Si es web, ¿hay alguna tecnología exigida o prohibida?
   - **Mage:** Aplicación web en Angular (o React), por ser tecnología moderna y popular.
2. (Relacionada con la 1) El monolito Swing del Corte 1: ¿se deja solo referenciado como la iteración 1, o se descarta y reutilizamos únicamente lo que sirva?
   - **Mage:** Reutilizar todo lo que sirva del proyecto en Swing. El monolito se mantiene en el repositorio como primera iteración (ramas `corte-1` y `corte-2`).
3. (Relacionada con la 1) ¿El frontend debe ser responsive y ejecutarse dentro del Docker Compose del Corte 3?
   - **Mage:** Responsive no se evalúa como requisito estricto. Poner el frontend en el mismo Docker Compose es valor agregado (ver 25).

## B. Arquitectura de microservicios

4. ¿Hay un mínimo o un número esperado de microservicios? Proponemos: gateway, usuarios, preguntas (con validación y ciclo de vida juntos), revisión, notificaciones y auditoría opcional. En el Corte 2, ¿podemos dejar Simulacros y Seguimiento Académico sin implementar y hacerlos en el Corte 3?
   - **Mage:** La propuesta de servicios es adecuada. Simulacros y Seguimiento Académico se dejan para el Corte 3. No habló de un número mínimo.
5. Eventos: ¿se exige un broker real? ¿Está bien RabbitMQ? ¿Esperan coreografía de eventos u orquestador, y patrones como Saga, Outbox, CQRS o Event Sourcing?
   - **Mage:** RabbitMQ es correcto. No se exigen orquestadores ni Saga, CQRS o Event Sourcing para esta entrega.
6. ¿Es obligatorio el API Gateway? ¿Se espera service discovery o config server (Eureka, Spring Cloud), o podemos prescindir? ¿Java con Spring Boot es obligatorio para los servicios?
   - **Mage:** Java con Spring Boot es obligatorio. Otros frameworks o lenguajes cuentan solo como valor agregado. El gateway quedó dentro de la propuesta aprobada. Sin respuesta sobre service discovery o config server.
7. Bases de datos (con PostgreSQL ya decidido): ¿una instancia por microservicio o basta un servidor con esquemas separados?
   - **Mage:** Una instancia independiente de base de datos por cada microservicio.
8. Escalabilidad: ¿cómo se espera demostrarla, con réplicas (`docker compose --scale`) detrás de un balanceador, con una prueba de carga como k6, o con ambas? RNF-05 pide 100 usuarios y RNF-17 pide 500: ¿cuál vale, y el tiempo es promedio o percentil 95?
   - **Mage:** Se demuestra sometiendo la arquitectura hasta el límite de la infraestructura ("lo que dé la máquina"). Sin respuesta sobre réplicas, herramienta, 100 contra 500 ni promedio contra percentil 95.
9. ¿Se espera tolerancia a fallos (reintentos, circuit breaker) o queda fuera de alcance?
   - **Mage:** Fuera de alcance por ahora; se aborda en el Corte 3 junto con seguridad avanzada.
10. ¿Podemos copiar y adaptar el dominio del Corte 1 dentro de cada servicio, sin una librería compartida entre ellos?
    - **Mage:** Sin respuesta directa (implícito: reutilizar lo que sirva del monolito, ver 2).
11. Swagger: ¿en cada servicio, o basta el agregado en el gateway?
    - **Mage:** Construir la documentación y las colecciones de prueba por partes o módulos, a medida que se desarrolla cada servicio. No habló del agregado en el gateway.

## C. Historias de usuario

12. Asignación de revisores: el mínimo es 1 y el máximo 3, y asigna el administrador. ¿Conviene que el diseño permita otro rol asignador en el futuro (coordinador)? ¿O lo dejamos fijo para el administrador?
    - **Mage:** La asignación (de 1 a 3 revisores) es responsabilidad exclusiva del Administrador. Sin respuesta sobre roles futuros.
13. Decisión con varios revisores: ¿unanimidad, mayoría, o basta un rechazo para rechazar la pregunta?
    - **Mage:** Unanimidad. Con un solo rechazo la pregunta es devuelta.
14. (Relacionada con la 13) ¿Cuándo pasa la pregunta a "En revisión": al asignar los revisores o cuando el primero evalúa? Tras una versión mejorada (HU-06), ¿se reasignan los mismos revisores o se eligen nuevos?
    - **Mage:** Pasa automáticamente a "En revisión" cuando el administrador asigna los revisores. Sin respuesta sobre la reasignación tras la versión mejorada.
15. Formato de evaluación (RF-17): ¿hay criterios y puntaje definidos o los definimos nosotros? ¿Las observaciones son obligatorias al rechazar y opcionales al aprobar? ¿El revisor puede editar su evaluación después de enviarla? ¿Los revisores ven las evaluaciones de los demás?
    - **Mage:** No hay formato rígido de puntaje; se aplica el criterio libre del comité. Todos los revisores ven las observaciones de sus colegas para generar consenso. Sin respuesta sobre obligatoriedad de observaciones ni edición posterior.
16. Notificaciones: ¿quiénes son "los implicados" (autor, revisores, administrador)? ¿Una bandeja dentro del sistema más un correo simulado es suficiente, o se espera correo real?
    - **Mage:** Solo los involucrados: administrador, revisores y autor. Se exige integración con un servicio de correo real (ejemplo citado: Microsoft/Outlook). Sin respuesta sobre la bandeja interna. Nota: el documento del proyecto dice que el correo puede simularse.
17. Edición y versiones (HU-03 y HU-06): RF-06 solo permite editar en Borrador. Tras un rechazo, ¿la pregunta vuelve a Borrador o se crea una versión nueva con historial? ¿A qué estado pasa al reenviarla?
    - **Mage:** Tras un rechazo existen estados a los que no se puede regresar, según la máquina de estados vista en el laboratorio. Sin respuesta sobre versiones ni estado al reenviar.
18. (Relacionada con la 17) Estados Publicada y Archivada: ¿quién publica una pregunta aprobada y entra en el alcance del Corte 2 o 3? ¿Podemos definir y justificar nosotros la tabla de transiciones entre estados?
    - **Mage:** Consultarlo puntualmente con el profesor Libardo, por ser el experto en el negocio de pruebas ICFES/Saber.
19. Usuarios y roles: ¿se espera un CRUD de usuarios y roles en el Corte 2, o bastan usuarios precargados? ¿Qué roles deben funcionar en la demo?
    - **Mage:** Usuarios precargados únicamente. No hace falta implementar funcionalidades para Docente ni Estudiante en este corte.

## D. Seguridad

20. En el Corte 2, ¿basta una autenticación mínima con usuario y rol simulados hasta llegar a Keycloak en el Corte 3?
    - **Mage:** Sí: seguridad mínima y básica (simulada o simple). Keycloak se integra en el Corte 3.
21. (Relacionada con la 20) Keycloak: ¿debe haber pantalla de login en el frontend o vale obtener el token por Postman? ¿Debemos versionar el realm exportado en el repositorio?
    - **Mage:** Sin respuesta (solo confirmó que Keycloak va en el Corte 3).
22. Autorización: ¿se valida en el gateway, en cada microservicio o en ambos? ¿El token se propaga entre servicios? ¿Se exige HTTPS?
    - **Mage:** La autorización se valida de forma centralizada en el API Gateway. Sin respuesta sobre propagación del token ni HTTPS.
23. RNF-08 (hash de contraseñas) y RNF-09 (auditoría de inicios de sesión y cambios de estado): ¿los cubre Keycloak o debemos implementarlos nosotros, por ejemplo con un servicio de auditoría?
    - **Mage:** Sin respuesta.

## E. Hexagonal, DDD, Docker y patrones

24. Hexagonal con DDD: ¿basta un solo microservicio (el de revisión) y los demás quedan en capas? ¿Qué elementos de DDD se esperan: agregados, objetos de valor, eventos de dominio, repositorios?
    - **Mage:** El objetivo es que el dominio permanezca limpio e independiente de los frameworks en los microservicios. Sin respuesta sobre cuántos servicios ni qué elementos de DDD.
25. Docker Compose: ¿debe levantar todo con un solo comando (servicios, bases, broker, Keycloak, gateway y frontend)? ¿Hace falta publicar imágenes en un registro? ¿Kubernetes queda fuera de alcance?
    - **Mage:** El mínimo es levantar el backend y las bases de datos en contenedores. Automatizar todo con un solo comando (incluido frontend y broker) es valor agregado. Sin respuesta sobre registro de imágenes ni Kubernetes.
26. Patrones GoF: ¿pueden venir del Corte 1 o deben ser nuevos? ¿Deben repartirse entre servicios? ¿Cuentan el gateway como Facade y el correo simulado como Adapter? ¿Los patrones arquitectónicos (Gateway, Event-Driven, Hexagonal) cuentan aparte? En el Corte 3, ¿cuántos patrones adicionales se esperan?
    - **Mage:** La aplicación de patrones depende de la necesidad de cada microservicio. En el Corte 3 no se piden patrones adicionales; se mantiene la exigencia de 1 o 2 por categoría (creacionales, estructurales, de comportamiento). Sin respuesta sobre el resto.

## F. Pruebas y restricciones

27. Pruebas: ¿hay un porcentaje mínimo de cobertura? ¿Cuentan las pruebas de integración, de contrato entre servicios y las de carga, o solo las unitarias de dominio? ¿Se aceptan bases en memoria (H2) o contenedores (Testcontainers)?
    - **Mage:** Garantizar los mínimos exigidos: pruebas unitarias y de integración sobre el dominio. Sin respuesta sobre cobertura, contrato, carga, H2 ni Testcontainers.
28. ¿Hay librerías o frameworks restringidos o recomendados (Spring Cloud, Lombok, MapStruct)?
    - **Mage:** El docente fija las librerías básicas y el equipo puede agregar las que requiera (Lombok, MapStruct) como valor agregado.

## G. Aclaraciones adicionales detectadas al cruzar las respuestas

29. Rechazo: "con un solo rechazo la pregunta es devuelta". ¿La revisión se cierra al primer rechazo, o se espera a que todos los revisores evalúen? ¿"Devuelta" significa que pasa al estado Rechazada, y el autor luego la mejora y la reenvía (HU-06)?
    - **Mage:** Sin respuesta.
30. Correo real: ¿sirve una cuenta Gmail con contraseña de aplicación vía SMTP? ¿En la demo se envía a correos reales de los integrantes?
    - **Mage:** Sin respuesta (pidió correo real, ejemplo Microsoft/Outlook).
31. Login del Corte 2: el frontend necesita iniciar sesión. ¿Es aceptable que el servicio de usuarios tenga usuarios precargados y emita un token firmado que valida el gateway, para sustituir el emisor por Keycloak en el Corte 3?
    - **Mage:** Sin respuesta (pidió seguridad mínima, simulada o simple).
32. Hexagonal y DDD: la respuesta habla de dominio limpio "en los microservicios", pero el documento del Corte 3 dice "un microservicio que lo amerite". ¿Se aplica a todos o a uno?
    - **Mage:** Sin respuesta definitiva (ver 24).
