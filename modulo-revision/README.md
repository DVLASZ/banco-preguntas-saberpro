# Módulo de Revisión (HU-04)

Asignación de revisores a las preguntas que esperan revisión: el
Administrador elige al menos un revisor por pregunta, la pregunta pasa a
**En revisión** y el sistema avisa a cada revisor por correo (simulado, ya que
el proyecto permite simular las integraciones externas).

> Estado: implementado.

## Historia de usuario

*Como Administrador necesito asignar al menos un revisor de las preguntas en
estado "Pendiente de revisión" para que las preguntas sean revisadas por otros
docentes.*

Criterios de aceptación:
1. Al entrar a "Asignación de Revisores" se muestran las preguntas Pendientes
   de revisión con su autor.
2. Con una pregunta y al menos un revisor seleccionado, al hacer clic en
   Asignar se guarda la asignación, la pregunta pasa a En revisión y se
   notifica (simulado).
3. Si no se marcó ningún revisor, se muestra "Debe seleccionar al menos un
   revisor".
4. El autor de la pregunta no se ofrece como revisor: no aparece en la lista de revisores
   disponibles.

## Estructura

Sigue la arquitectura en capas del resto del proyecto:

```
co.unicauca.saberpro.revision
├── domain/         AsignacionRevision, AsignacionRevisionService y los puertos
│                   AsignacionRevisionRepository, NotificadorAsignacion, DirectorioRevisores
├── access/         AsignacionRevisionImplRepository (en memoria), NotificadorCorreoSimulado
└── presentation/   GUIAsignacionRevisores (pantalla del Administrador)
```

El servicio depende de las abstracciones (puertos), no de sus implementaciones,
de modo que el correo simulado se puede cambiar por uno real sin tocar la lógica.

## Ejecutar las pruebas

```bash
mvn test -pl modulo-revision -am
```

## Cómo probarlo

```bash
mvn -q package -DskipTests
java -jar app/target/banco-preguntas-saberpro.jar
```

Usuarios (contraseña `Saber2026!` para todos): `autor1`, `revisor1`, `admin1`.
Las preguntas viven en memoria: se pierden al cerrar la app. Para cambiar de
usuario no reinicies: cierra la ventana principal del rol y vuelve el login.

1. `autor1` → en "Mis preguntas" abre **P-001** (Borrador) → **Enviar a
   revisión** → confirma. Queda "Pendiente de revisión".
2. `admin1` → ventana "Asignación de Revisores": aparecen **P-001 y P-003**
   con su autor `autor1` (criterio 1).
3. Elige una pregunta y pulsa **Asignar** sin marcar a nadie → sale
   "Debe seleccionar al menos un revisor" (criterio 3).
4. Elige P-001, marca a `revisor1` y pulsa **Asignar** → mensaje de éxito, el
   "correo" sale en la consola y P-001 ya no aparece como pendiente
   (criterio 2).
5. `revisor1` → ve solo P-001 y puede aprobarla o rechazarla.
6. El criterio 4 (el autor no se ofrece como revisor) lo cubre la prueba
   `revisoresDisponibles_noIncluyeAlAutorDeLaPregunta`.

## Autores

- Kevin Yesid Castaño Herrera
