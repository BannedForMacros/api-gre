# ApiGRE

API de Guías de Remisión Electrónicas. **Único componente autorizado a abrir
conexiones contra el SQL Server del cliente** — la aplicación Laravel no tiene
conexión `sqlsrv`.

## Por qué existe

Reemplaza al `ApiDMK`, que mezclaba guías con POSM, ventas, preventas y comandas
(11 494 líneas). De todo eso, solo el módulo GRE tenía que ver con guías.
ApiGRE se queda únicamente con eso.

## Stack

| | Versión | Por qué |
|---|---|---|
| Java | 8 | Mínimo común denominador de Windows 7 / Server 2008 R2 |
| Spring Boot | 2.7.18 | Última rama que corre en Java 8 |
| Driver | mssql-jdbc 7.4.1.jre8 | Compatible con SQL Server 2008 |
| Empaquetado | fat JAR + Tomcat embebido | Instalar es copiar un archivo |

## Construir

    mvn clean package
    java -jar target/api-gre.jar

## Configurar

`src/main/resources/application.properties`, o variables de entorno:

    SPRING_DATASOURCE_URL=jdbc:sqlserver://localhost:1433;DatabaseName=db_cliente;encrypt=false;trustServerCertificate=true
    SPRING_DATASOURCE_USERNAME=sa
    SPRING_DATASOURCE_PASSWORD=...
    GRE_IGV_TASA=0.18

## Endpoints

    GET  /api/v1/health     estado del servicio y del SQL Server
    POST /api/v1/guias      registra una guía en el DataMart

## Notas

- Los importes se calculan **en el servidor**. El cliente manda precios sin IGV.
- `esConsignado` viaja por línea del detalle, no como UPDATE aparte.
- Las líneas cuyo artículo no existe en `MaestroArticulo` se reportan en
  `lineasRechazadas`. El stored procedure del ERP las descartaba en silencio.
- Los stored procedures del DataMart **no se reescriben**: se invocan.
