# infraestructura Specification

## Purpose
TBD - created by archiving change 2026-09-08-add-infraestructura. Update Purpose after archive.

## Requirements

### Requirement: La suite de tests corre sin dependencias externas

`mvnw test` SHALL ejecutarse por completo sin ningún servicio externo levantado: sin
Docker, sin un PostgreSQL escuchando y sin acceso a internet. La base de datos de la
fase de test SHALL ser una base en memoria en modo de compatibilidad PostgreSQL,
creada y destruida por la propia corrida.

La configuración de test SHALL vivir en el classpath de test y aplicarse por
precedencia, de modo que **ninguna clase de test necesite anotarse** para usarla: un
test nuevo hereda el comportamiento sin hacer nada. La corrida de tests MUST NOT
escribir en la base de datos de desarrollo bajo ninguna circunstancia.

#### Scenario: Tests con PostgreSQL apagado

- **WHEN** se ejecuta la suite completa sin ningún contenedor ni servicio de base de
  datos corriendo
- **THEN** todos los tests se ejecutan y el resultado es el mismo que con PostgreSQL
  levantado

#### Scenario: Un test nuevo sin anotaciones

- **WHEN** se agrega una clase de test de integración sin ninguna anotación de perfil
  ni de origen de datos
- **THEN** usa la base en memoria igual que las demás
- **AND** no toca la base de desarrollo

#### Scenario: La base de desarrollo queda intacta

- **WHEN** se ejecuta la suite completa con la base de desarrollo levantada y con
  datos
- **THEN** esos datos no se modifican ni se borran

### Requirement: CORS configurable sin afectar el desarrollo

El sistema SHALL leer los orígenes permitidos de la propiedad
`cors.allowed-origins`. Cuando la propiedad está vacía o ausente —el caso de
desarrollo, donde el proxy del servidor de desarrollo evita la petición cruzada— el
sistema MUST NOT registrar ninguna configuración de CORS, y el comportamiento SHALL
ser idéntico al de no tenerla.

Cuando la propiedad trae uno o más orígenes, el sistema SHALL permitir para esos
orígenes los métodos `GET`, `POST`, `PUT`, `DELETE` y `OPTIONS`, y las cabeceras
`Authorization` y `Content-Type`. El sistema MUST NOT permitir cualquier origen con
un comodín.

#### Scenario: Desarrollo sin la propiedad configurada

- **WHEN** el backend arranca sin `cors.allowed-origins`
- **THEN** no se registra ninguna configuración de CORS
- **AND** las peticiones del entorno de desarrollo funcionan igual que antes

#### Scenario: Producción con un origen configurado

- **WHEN** el backend arranca con `cors.allowed-origins` apuntando al dominio del
  frontend
- **THEN** una petición cruzada desde ese origen con la cabecera `Authorization` es
  aceptada
- **AND** una petición cruzada desde otro origen no lo es

### Requirement: Imágenes de contenedor para backend y frontend

El proyecto SHALL incluir una imagen de contenedor por módulo, ambas multi-etapa,
de modo que la imagen final no contenga herramientas de compilación.

La imagen del backend SHALL compilar el proyecto y ejecutar únicamente el artefacto
resultante sobre un entorno de ejecución de Java 21. La imagen del frontend SHALL
construir los archivos estáticos y servirlos con un servidor web, que SHALL devolver
`index.html` para cualquier ruta no encontrada, porque el enrutamiento es del lado
del cliente.

Ninguna de las dos imágenes SHALL fijar en su interior la dirección del otro módulo
ni credenciales: esa configuración SHALL llegar por variables de entorno o por la
configuración del despliegue.

#### Scenario: La imagen del backend no lleva herramientas de compilación

- **WHEN** se construye la imagen del backend
- **THEN** la imagen final contiene el artefacto ejecutable y un entorno de ejecución
  de Java 21
- **AND** no contiene la herramienta de construcción ni el código fuente

#### Scenario: Una ruta del cliente en el frontend servido

- **WHEN** se solicita al frontend servido una ruta que solo existe del lado del
  cliente
- **THEN** el servidor devuelve el documento principal de la aplicación
- **AND** no devuelve `404`

#### Scenario: La configuración no está incrustada

- **WHEN** se inspecciona cualquiera de las dos imágenes
- **THEN** no contiene credenciales de base de datos ni la dirección del otro módulo
  fijadas de forma inmutable
