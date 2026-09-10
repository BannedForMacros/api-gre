package pe.dbperu.gre.repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Traduce los nombres de columna del DataMart a los del contrato.
 *
 * Los stored procedures devuelven PascalCase (CodFormaPago, Descripcion) porque
 * son los nombres de las columnas del ERP. El contrato usa camelCase. El ApiDMK
 * hacia esta traduccion con un DTO por catalogo; aqui se hace con un mapa, que
 * es explicito y cabe en una pantalla.
 */
final class Mapeo {

    private Mapeo() { }

    /**
     * Renombra las claves indicadas y descarta el resto.
     * @param pares columnaOrigen1, claveDestino1, columnaOrigen2, claveDestino2, ...
     */
    static List<Map<String, Object>> proyectar(List<Map<String, Object>> filas, String... pares) {
        if (pares.length % 2 != 0) {
            throw new IllegalArgumentException("Se esperan pares columna/clave");
        }
        return filas.stream().map(fila -> {
            Map<String, Object> out = new LinkedHashMap<>();
            for (int i = 0; i < pares.length; i += 2) {
                out.put(pares[i + 1], buscar(fila, pares[i]));
            }
            return out;
        }).collect(Collectors.toList());
    }

    /** Busca ignorando mayusculas: los SPs no son consistentes entre si. */
    private static Object buscar(Map<String, Object> fila, String columna) {
        Object v = fila.get(columna);
        if (v != null || fila.containsKey(columna)) {
            return normalizar(v);
        }
        for (Map.Entry<String, Object> e : fila.entrySet()) {
            if (e.getKey().equalsIgnoreCase(columna)) {
                return normalizar(e.getValue());
            }
        }
        return null;
    }

    /** Los char(n) del ERP vienen con espacios de relleno. */
    private static Object normalizar(Object v) {
        return (v instanceof String) ? ((String) v).trim() : v;
    }
}
