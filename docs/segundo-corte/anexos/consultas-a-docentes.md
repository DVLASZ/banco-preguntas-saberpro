# Consultas a los docentes: respuestas y decisiones

Las 32 preguntas de alcance del corte 2 y del corte 3 se hicieron a los dos docentes de la materia:

- **Mage** (profesor Pablo Magé), primera consulta.
- **Libardo** (profesor W. Libardo Pantoja), segunda consulta. Es el tutor y coordinador de la materia: **cuando ambos difieren, manda lo que dijo Libardo**, y así quedó aclarado en la segunda sesión.

Cada pregunta trae las dos respuestas y la decisión que toma el equipo. «Sin respuesta» significa que el docente no se pronunció sobre ese punto.

## Resumen: dónde difieren y qué decidimos

| Tema | Mage | Libardo (criterio vigente) | Decisión del equipo |
|---|---|---|---|
| Frontend | Web con Angular o React | Swing o JavaFX vale; no hay que complicarse con el frontend, el foco es el backend | Web con Angular, que además cumple lo de Mage. El monolito Swing se conserva como referencia |
| Correo | Real (ejemplo Outlook) | **Simulado**, con eventos publicador/suscriptor, para evitar bloqueos de cuenta | Correo simulado por eventos; el correo queda registrado y visible en la bandeja del sistema |
| Escalabilidad | Hasta lo que dé la máquina | Fuera de alcance de evaluación | Se mide igualmente y se documenta como valor agregado |
| Tolerancia a fallos | Fuera de alcance por ahora, corte 3 | Fuera de alcance | Valor agregado opcional, sin prioridad |
| API Gateway y Eureka | El gateway está dentro de la propuesta | Corte 2: la aplicación se conecta directo a los servicios. Corte 3: gateway como redireccionador sencillo. Eureka opcional | Se construyen como valor agregado; no condicionan la entrega |
| Auditoría | Sin respuesta | **No se hace** | No es requisito; el historial de revisiones sí (ver pregunta 17) |
| Hexagonal con DDD | Dominio limpio en los microservicios | **Un solo servicio** (revisión o gestión de preguntas) con agregados, objetos de valor, servicios de dominio y repositorios; los demás en capas | Se aplica en un servicio en el corte 3; los demás conservan capas con el dominio sin dependencias de marcos |
| Patrones GoF | 1 o 2 por categoría | Mínimo 6 acumulados, de los laboratorios, en cualquier escenario (también el frontend) | Seis o más; ya se cumplen (ver [patrones](../05-patrones.md)) |
| Seguridad en el corte 2 | Mínima, simulada | Inicio de sesión sencillo con datos quemados; en el corte 3, Keycloak con pantalla de login y token en el cliente | Usuarios precargados |
| Autorización | Centralizada en el gateway | En el gateway **y** en cada microservicio | En ambos lugares |
| Reasignación tras mejorar la pregunta | Sin respuesta | Se asignan los mismos revisores | Los mismos revisores |
| Asignación de revisores | Exclusiva del administrador, 1 a 3 | 1 a 3, pero si queda abierta a más no hay problema | 1 a 3, configurable |

## A. Frontend y monolito

1. ¿El frontend debe ser una aplicación web (React, Angular, Vue u otra) o vale JavaFX o Swing consumiendo la API REST? Si es web, ¿hay alguna tecnología exigida o prohibida?
   - **Mage:** Aplicación web en Angular (o React), por ser tecnología moderna y popular.
   - **Libardo:** Vale Java Swing o JavaFX; no hace falta focalizarse en el frontend, el enfoque principal es el backend. Una web sencilla conectada a las API REST también sirve.
   - **Decisión:** aplicación web en Angular, con las pantallas de los prototipos. Es válida con ambos criterios.
2. (Relacionada con la 1) El monolito Swing del corte 1: ¿se deja solo referenciado como la iteración 1, o se descarta y reutilizamos únicamente lo que sirva?
   - **Mage:** Reutilizar todo lo que sirva del proyecto en Swing. El monolito se mantiene en el repositorio como primera iteración (ramas `corte-1` y `corte-2`).
   - **Libardo:** Ya está modular, hay que reutilizarlo. Las entregas se manejan como iteraciones en el mismo repositorio.
   - **Decisión:** se reutiliza el dominio y se conserva el monolito. Ver [reutilización del monolito](reutilizacion-del-monolito.md).
3. (Relacionada con la 1) ¿El frontend debe ser responsive y ejecutarse dentro del Docker Compose del corte 3?
   - **Mage:** Responsive no se evalúa como requisito estricto. Poner el frontend en el mismo Docker Compose es valor agregado (ver 25).
   - **Libardo:** No hay necesidad de focalizar el frontend.
   - **Decisión:** el frontend no es prioridad; se incluye en el Compose por ser valor agregado.

## B. Arquitectura de microservicios

4. ¿Hay un mínimo o un número esperado de microservicios? Proponemos: gateway, usuarios, preguntas (con validación y ciclo de vida juntos), revisión, notificaciones y auditoría opcional. En el corte 2, ¿podemos dejar Simulacros y Seguimiento Académico sin implementar y hacerlos en el corte 3?
   - **Mage:** La propuesta de servicios es adecuada. Simulacros y Seguimiento Académico se dejan para el corte 3. No habló de un número mínimo.
   - **Libardo:** Dos microservicios en el corte 2: preguntas y revisores (gestión de preguntas y revisiones con su historial). Usuarios y roles, en el corte 3.
   - **Decisión:** preguntas y revisión son el núcleo obligatorio. Notificaciones, historial y usuarios se construyen además, como valor agregado. Simulacros y Seguimiento Académico quedan para el corte 3.
5. Eventos: ¿se exige un broker real? ¿Está bien RabbitMQ? ¿Esperan coreografía de eventos u orquestador, y patrones como Saga, Outbox, CQRS o Event Sourcing?
   - **Mage:** RabbitMQ es correcto. No se exigen orquestadores ni Saga, CQRS o Event Sourcing para esta entrega.
   - **Libardo:** RabbitMQ para comunicación asíncrona con publicador y suscriptor.
   - **Decisión:** RabbitMQ con coreografía de eventos, sin Saga, CQRS ni Event Sourcing.
6. ¿Es obligatorio el API Gateway? ¿Se espera service discovery o config server (Eureka, Spring Cloud), o podemos prescindir? ¿Java con Spring Boot es obligatorio para los servicios?
   - **Mage:** Java con Spring Boot es obligatorio. Otros frameworks o lenguajes cuentan solo como valor agregado. El gateway quedó dentro de la propuesta aprobada. Sin respuesta sobre service discovery o config server.
   - **Libardo:** En el corte 2 la aplicación se conecta directamente a los microservicios. En el corte 3 se usa el gateway como redireccionador sencillo. Eureka es opcional.
   - **Decisión:** Spring Boot en todos los servicios. Gateway y Eureka son valor agregado desde el corte 2.
7. Bases de datos (con PostgreSQL ya decidido): ¿una instancia por microservicio o basta un servidor con esquemas separados?
   - **Mage:** Una instancia independiente de base de datos por cada microservicio.
   - **Libardo:** Cada microservicio debe tener su base de datos; sirve cualquier motor (PostgreSQL).
   - **Decisión:** una instancia de PostgreSQL por servicio.
8. Escalabilidad: ¿cómo se espera demostrarla, con réplicas (`docker compose --scale`) detrás de un balanceador, con una prueba de carga como k6, o con ambas? RNF-05 pide 100 usuarios y RNF-17 pide 500: ¿cuál vale, y el tiempo es promedio o percentil 95?
   - **Mage:** Se demuestra sometiendo la arquitectura hasta el límite de la infraestructura («lo que dé la máquina»). Sin respuesta sobre réplicas, herramienta, 100 contra 500 ni promedio contra percentil 95.
   - **Libardo:** Fuera de alcance.
   - **Decisión:** se mide con k6 de todos modos, con 500 o más usuarios y percentil 95, como valor agregado.
9. ¿Se espera tolerancia a fallos (reintentos, circuit breaker) o queda fuera de alcance?
   - **Mage:** Fuera de alcance por ahora; se aborda en el corte 3 junto con seguridad avanzada.
   - **Libardo:** Fuera de alcance.
   - **Decisión:** no es requisito; se agrega como extra.
10. ¿Podemos copiar y adaptar el dominio del corte 1 dentro de cada servicio, sin una librería compartida entre ellos?
    - **Mage:** Sin respuesta directa (implícito: reutilizar lo que sirva del monolito, ver 2).
    - **Libardo:** Reutilizar el dominio todo lo que se pueda.
    - **Decisión:** el dominio se copia y adapta dentro de cada servicio.
11. Swagger: ¿en cada servicio, o basta el agregado en el gateway?
    - **Mage:** Construir la documentación y las colecciones de prueba por partes o módulos, a medida que se desarrolla cada servicio.
    - **Libardo:** Un Swagger por cada API REST.
    - **Decisión:** Swagger propio en cada servicio, más una colección de Postman por módulo.

## C. Historias de usuario

12. Asignación de revisores: el mínimo es 1 y el máximo 3, y asigna el administrador. ¿Conviene que el diseño permita otro rol asignador en el futuro (coordinador)? ¿O lo dejamos fijo para el administrador?
    - **Mage:** La asignación (de 1 a 3 revisores) es responsabilidad exclusiva del administrador. Sin respuesta sobre roles futuros.
    - **Libardo:** De 1 a 3 revisores; si queda abierto a más no hay problema.
    - **Decisión:** asigna el administrador; el máximo es un parámetro de configuración (3 por defecto).
13. Decisión con varios revisores: ¿unanimidad, mayoría, o basta un rechazo para rechazar la pregunta?
    - **Mage:** Unanimidad. Con un solo rechazo la pregunta es devuelta.
    - **Libardo:** Unanimidad. Con un rechazo la pregunta se devuelve.
    - **Decisión:** unanimidad; un rechazo devuelve la pregunta al autor.
14. (Relacionada con la 13) ¿Cuándo pasa la pregunta a «En revisión»: al asignar los revisores o cuando el primero evalúa? Tras una versión mejorada (HU-06), ¿se reasignan los mismos revisores o se eligen nuevos?
    - **Mage:** Pasa automáticamente a «En revisión» cuando el administrador asigna los revisores. Sin respuesta sobre la reasignación.
    - **Libardo:** Al enviar la pregunta de nuevo a revisión se asignan los mismos revisores.
    - **Decisión:** pasa a «En revisión» al asignar; las nuevas rondas conservan a los mismos revisores.
15. Formato de evaluación (RF-17): ¿hay criterios y puntaje definidos o los definimos nosotros? ¿Las observaciones son obligatorias al rechazar y opcionales al aprobar? ¿El revisor puede editar su evaluación después de enviarla? ¿Los revisores ven las evaluaciones de los demás?
    - **Mage:** No hay formato rígido de puntaje; se aplica el criterio libre del comité. Todos los revisores ven las observaciones de sus colegas para generar consenso.
    - **Libardo:** Las observaciones son obligatorias al rechazar y opcionales al aprobar. Una vez enviada la evaluación, queda así. No se tienen criterios ni puntaje definidos: se deja el formulario que ya existe.
    - **Decisión:** observaciones obligatorias solo al rechazar; la evaluación no se edita; los revisores ven las observaciones de sus colegas; sin puntaje.
16. Notificaciones: ¿quiénes son «los implicados» (autor, revisores, administrador)? ¿Una bandeja dentro del sistema más un correo simulado es suficiente, o se espera correo real?
    - **Mage:** Solo los involucrados: administrador, revisores y autor. Se exige integración con un servicio de correo real (ejemplo citado: Microsoft/Outlook).
    - **Libardo:** Correo **simulado**, mediante eventos de publicador y suscriptor: se genera un evento y se simula el envío en el microservicio. Rectificó la indicación de correo real porque enviar correos reales en pruebas bloquea cuentas y complica la evaluación.
    - **Decisión:** correo simulado por eventos, a administrador, revisores y autor, con bandeja dentro del sistema. Si más adelante se pidiera correo real, solo cambia el adaptador de envío.
17. Edición y versiones (HU-03 y HU-06): RF-06 solo permite editar en Borrador. Tras un rechazo, ¿la pregunta vuelve a Borrador o se crea una versión nueva con historial? ¿A qué estado pasa al reenviarla?
    - **Mage:** Tras un rechazo existen estados a los que no se puede regresar, según la máquina de estados vista en el laboratorio. Sin respuesta sobre versiones ni estado al reenviar.
    - **Libardo:** Se puede editar mientras está en borrador y también si se rechaza. Un microservicio lleva el historial de revisiones, con fecha y quién la hizo.
    - **Decisión:** edición en Borrador y tras un rechazo, con versiones; `history-service` guarda el historial con fecha y responsable.
18. (Relacionada con la 17) Estados Publicada y Archivada: ¿quién publica una pregunta aprobada y entra en el alcance del corte 2 o 3? ¿Podemos definir y justificar nosotros la tabla de transiciones entre estados?
    - **Mage:** Consultarlo con el profesor Libardo, por ser el experto en el negocio de pruebas ICFES y Saber.
    - **Libardo:** La pregunta aprobada la publica el administrador.
    - **Decisión:** publica el administrador, de forma manual. Archivada queda sin definir y fuera del alcance por ahora.
19. Usuarios y roles: ¿se espera un CRUD de usuarios y roles en el corte 2, o bastan usuarios precargados? ¿Qué roles deben funcionar en la demo?
    - **Mage:** Usuarios precargados únicamente. No hace falta implementar funcionalidades para Docente ni Estudiante en este corte.
    - **Libardo:** Así como lo tenemos: usuarios y roles precargados. Gestión completa de usuarios y roles en el corte 3.
    - **Decisión:** usuarios precargados con los roles administrador, autor y revisor.

## D. Seguridad

20. En el corte 2, ¿basta una autenticación mínima con usuario y rol simulados hasta llegar a Keycloak en el corte 3?
    - **Mage:** Sí: seguridad mínima y básica (simulada o simple). Keycloak se integra en el corte 3.
    - **Libardo:** Inicio de sesión sencillo, quemando los datos.
    - **Decisión:** sí. Usuarios precargados; Keycloak puede adelantarse porque ya está funcionando.
21. (Relacionada con la 20) Keycloak: ¿debe haber pantalla de login en el frontend o vale obtener el token por Postman? ¿Debemos versionar el realm exportado en el repositorio?
    - **Mage:** Sin respuesta (solo confirmó que Keycloak va en el corte 3).
    - **Libardo:** Debe haber pantalla de login en el frontend, y el token se almacena en el cliente. Sin respuesta sobre el realm.
    - **Decisión:** pantalla de login en el frontend; el realm exportado se versiona en el repositorio.
22. Autorización: ¿se valida en el gateway, en cada microservicio o en ambos? ¿El token se propaga entre servicios? ¿Se exige HTTPS?
    - **Mage:** La autorización se valida de forma centralizada en el API Gateway. Sin respuesta sobre propagación del token ni HTTPS.
    - **Libardo:** Se valida en el gateway y también en cada microservicio.
    - **Decisión:** validación en ambos lugares (autorización redundante).
23. RNF-08 (hash de contraseñas) y RNF-09 (auditoría de inicios de sesión y cambios de estado): ¿los cubre Keycloak o debemos implementarlos nosotros, por ejemplo con un servicio de auditoría?
    - **Mage:** Sin respuesta.
    - **Libardo:** No se hace auditoría.
    - **Decisión:** la auditoría de accesos no es requisito. El hash de contraseñas lo cubre Keycloak. El historial de cambios de estado se mantiene por la pregunta 17.

## E. Hexagonal, DDD, Docker y patrones

24. Hexagonal con DDD: ¿basta un solo microservicio (el de revisión) y los demás quedan en capas? ¿Qué elementos de DDD se esperan: agregados, objetos de valor, eventos de dominio, repositorios?
    - **Mage:** El objetivo es que el dominio permanezca limpio e independiente de los frameworks en los microservicios. Sin respuesta sobre cuántos servicios ni qué elementos.
    - **Libardo:** Lógica de negocio en el centro y lo demás en capas. Basta **un** microservicio (por ejemplo revisión o gestión de preguntas), con agregados, objetos de valor, servicios de dominio y repositorios. Si el servicio es sencillo, no amerita arquitectura hexagonal. En el corte 3 solo se modifica ese servicio y el resto queda como en el corte 2.
    - **Decisión:** hexagonal con DDD en un solo servicio, en el corte 3. Los demás siguen en capas con el dominio independiente de los marcos.
25. Docker Compose: ¿debe levantar todo con un solo comando (servicios, bases, broker, Keycloak, gateway y frontend)? ¿Hace falta publicar imágenes en un registro? ¿Kubernetes queda fuera de alcance?
    - **Mage:** El mínimo es levantar el backend y las bases de datos en contenedores. Automatizar todo con un solo comando (incluido frontend y broker) es valor agregado.
    - **Libardo:** En el corte 3 la solución se empaqueta para levantarse con un solo comando, microservicios y servicios externos. Kubernetes queda fuera de alcance.
    - **Decisión:** un solo comando para todo; no se publica en registro y no se usa Kubernetes.
26. Patrones GoF: ¿pueden venir del corte 1 o deben ser nuevos? ¿Deben repartirse entre servicios? ¿Cuentan el gateway como Facade y el correo simulado como Adapter? ¿Los patrones arquitectónicos (Gateway, Event-Driven, Hexagonal) cuentan aparte? En el corte 3, ¿cuántos patrones adicionales se esperan?
    - **Mage:** Depende de la necesidad de cada microservicio. En el corte 3 no se piden patrones adicionales; se mantiene la exigencia de 1 o 2 por categoría (creacionales, estructurales y de comportamiento).
    - **Libardo:** Se acumulan mínimo 6 patrones GoF, los de los laboratorios, aplicables en cualquier escenario, también en el frontend (por ejemplo gateway como Facade y notificador como Adapter). No se piden más patrones en el corte 3.
    - **Decisión:** seis patrones acumulados, con al menos dos por categoría. Detalle en [patrones](../05-patrones.md).

## F. Pruebas y restricciones

27. Pruebas: ¿hay un porcentaje mínimo de cobertura? ¿Cuentan las pruebas de integración, de contrato entre servicios y las de carga, o solo las unitarias de dominio? ¿Se aceptan bases en memoria (H2) o contenedores (Testcontainers)?
    - **Mage:** Garantizar los mínimos exigidos: pruebas unitarias y de integración sobre el dominio. Sin respuesta sobre cobertura, contrato, carga, H2 ni Testcontainers.
    - **Libardo:** Pruebas unitarias y de integración, las necesarias, sobre el dominio.
    - **Decisión:** unitarias e integración en cada servicio, con PostgreSQL real embebido en las pruebas de persistencia.
28. ¿Hay librerías o frameworks restringidos o recomendados (Spring Cloud, Lombok, MapStruct)?
    - **Mage:** El docente fija las librerías básicas y el equipo puede agregar las que requiera (Lombok, MapStruct) como valor agregado.
    - **Libardo:** Lombok es bueno; Spring Cloud y las demás librerías se pueden usar. Hay que hacer DTO.
    - **Decisión:** libertad de librerías; las API usan DTO.

## G. Aclaraciones adicionales

29. Rechazo: «con un solo rechazo la pregunta es devuelta». ¿La revisión se cierra al primer rechazo, o se espera a que todos los revisores evalúen? ¿«Devuelta» significa que pasa al estado Rechazada, y el autor luego la mejora y la reenvía (HU-06)?
    - **Mage:** Sin respuesta.
    - **Libardo:** Con un rechazo la pregunta se devuelve; se puede editar y volver a enviar a revisión.
    - **Decisión:** la pregunta pasa a Rechazada, el autor la corrige y se abre una nueva ronda con los mismos revisores. Las observaciones que lleguen después del rechazo se guardan marcadas como tardías.
30. Correo real: ¿sirve una cuenta Gmail con contraseña de aplicación vía SMTP? ¿En la demo se envía a correos reales de los integrantes?
    - **Mage:** Sin respuesta (pidió correo real).
    - **Libardo:** No aplica: el correo es simulado (ver 16).
    - **Decisión:** no se usa ninguna cuenta de correo.
31. Login del corte 2: el frontend necesita iniciar sesión. ¿Es aceptable que el servicio de usuarios tenga usuarios precargados y emita un token firmado que valida el gateway, para sustituir el emisor por Keycloak en el corte 3?
    - **Mage:** Sin respuesta (pidió seguridad mínima, simulada o simple).
    - **Libardo:** Sí en esencia: inicio de sesión sencillo con datos quemados, y pantalla de login en el frontend.
    - **Decisión:** usuarios precargados; el emisor del token es Keycloak, que se adelantó.
32. Hexagonal y DDD: la respuesta habla de dominio limpio «en los microservicios», pero el documento del corte 3 dice «un microservicio que lo amerite». ¿Se aplica a todos o a uno?
    - **Mage:** Sin respuesta definitiva (ver 24).
    - **Libardo:** A uno solo.
    - **Decisión:** a uno, en el corte 3.

## Pendientes

| Tema | Estado |
|---|---|
| Estado Archivada: quién la archiva y cuándo | Sin respuesta de ninguno de los dos; fuera del alcance por ahora |
| Realm de Keycloak versionado, HTTPS y propagación del token entre servicios | Sin respuesta; se resolvió por criterio del equipo |
