# Banco de Preguntas Saber Pro: primer corte

Esta rama (`corte-1`) conserva congelado el **primer corte** del proyecto: un monolito modular de escritorio en
Java Swing para gestionar y revisar preguntas del banco de preguntas Saber Pro. Proyecto de curso de Ingeniería de
Software II, Universidad del Cauca, periodo 2026.2.

El proyecto continúa en las ramas `main` y `corte-2`, con la solución distribuida (microservicios) del segundo corte.

## Estructura

```
├── docs/corte-1/   Documentación del primer corte (historias de usuario, C4, patrones, pruebas)
└── monolito/       Aplicación de escritorio
    ├── app/        Arranque de la aplicación
    └── modulos/    Módulos Maven: usuarios, preguntas, simulacros, microkernel, revisión y api-rest
```

## Cómo ejecutarlo

Con Java 17 y Maven:

```bash
cd monolito
mvn package
java -jar app/target/banco-preguntas-saberpro.jar
```

Más detalles en el [README del monolito](monolito/README.md) y en la [documentación](docs/corte-1/README.md).

## Autores

- Edward Esteban Dávila Salazar: edwarddavila@unicauca.edu.co
- Laura Isabel Sánchez Fernández
- Kevin Yesid Castaño Herrera
