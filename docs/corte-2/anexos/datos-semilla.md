# Datos semilla para desarrollo y demostración

El docente confirmó que se trabaja solo con **usuarios precargados**. Estos datos se cargan al iniciar
`user-service` en un entorno local. Las contraseñas se guardan con hash BCrypt (RNF-08); los valores de esta
página son solo de demostración local y no sirven fuera de ese entorno.

## Usuarios

| Rol | Nombre | Correo de demostración |
|---|---|---|
| ADMIN | Administrador del banco | admin@bancopreguntas.test |
| AUTHOR | Autor Uno | autor1@bancopreguntas.test |
| AUTHOR | Autor Dos | autor2@bancopreguntas.test |
| REVIEWER | Revisor Uno | revisor1@bancopreguntas.test |
| REVIEWER | Revisor Dos | revisor2@bancopreguntas.test |
| REVIEWER | Revisor Tres | revisor3@bancopreguntas.test |
| REVIEWER | Revisor Cuatro | revisor4@bancopreguntas.test |

Para la demostración con correo real, los correos de estos usuarios se reemplazan por direcciones reales de
los integrantes mediante variables de entorno, sin tocar el repositorio.

La contraseña de demostración se define en el archivo de configuración local de cada integrante.

## Preguntas de ejemplo

Se precargan unas pocas preguntas en distintos estados para poder probar los listados sin crearlas a mano:

| Nombre | Competencia | Dificultad | Estado | Autor |
|---|---|---|---|---|
| Interpretación de una gráfica de barras | Razonamiento cuantitativo | Básico | Borrador | Autor Uno |
| Idea principal de un texto argumentativo | Lectura crítica | Intermedio | Pendiente de revisión | Autor Uno |
| Derechos y deberes ciudadanos | Competencias ciudadanas | Básico | Pendiente de revisión | Autor Dos |
| Conectores en un párrafo | Comunicación escrita | Intermedio | Aprobada | Autor Dos |
| Comprensión de un correo electrónico | Inglés | Avanzado | Rechazada | Autor Uno |

Las preguntas en estado "En revisión" y "Aprobada" necesitan el historial de revisión correspondiente, por eso
se generan ejecutando el flujo (asignar y evaluar) y no como datos fijos.
