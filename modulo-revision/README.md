# Módulo de Revisión (HU-04)

Asignación de revisores a las preguntas que esperan revisión: el
Administrador elige al menos un revisor por pregunta, la pregunta pasa a
**En revisión** y el sistema avisa a cada revisor por correo (simulado, ya que
el proyecto permite simular las integraciones externas).

> Estado: en desarrollo. Las clases ya tienen su contrato definido y las
> pruebas de cada regla están descritas; falta implementarlas.

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

## Autores

- Kevin Yesid Castaño Herrera
