package pe.dbperu.gre;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ApiGRE — Guias de Remision Electronicas.
 *
 * Unico componente autorizado a abrir conexiones contra el SQL Server del
 * cliente. La aplicacion Laravel NO tiene conexion 'sqlsrv'.
 *
 * Se instala on-premise, junto al DataMart del cliente. Fat JAR con Tomcat
 * embebido: instalar es copiar un archivo, no administrar un Tomcat.
 */
@SpringBootApplication
public class ApiGreApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGreApplication.class, args);
    }
}
