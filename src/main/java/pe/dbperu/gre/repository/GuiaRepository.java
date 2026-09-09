package pe.dbperu.gre.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import pe.dbperu.gre.domain.ResultadoInsercion;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Acceso a los stored procedures del DataMart.
 *
 * Los SPs del ERP NO se reescriben: funcionan y son logica de negocio de anios.
 * Esta clase solo los invoca y traduce su respuesta.
 */
@Repository
public class GuiaRepository {

    private final JdbcTemplate jdbc;

    public GuiaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * prc_InsertGuiaDMKWeb -> InsertarGuiasOdooDmk.
     *
     * OJO: el SP usa metodos XML (.value(), .nodes()), que exigen
     * QUOTED_IDENTIFIER ON. JDBC lo manda encendido por defecto; sqlcmd no.
     * Si el SP se despliega con la opcion apagada queda grabado roto y falla
     * en runtime aunque el codigo sea correcto.
     */
    public ResultadoInsercion insertarGuia(String xml) {
        return jdbc.execute(
            (org.springframework.jdbc.core.ConnectionCallback<ResultadoInsercion>) con -> {
                try (CallableStatement cs = con.prepareCall("{ call prc_InsertGuiaDMKWeb(?) }")) {
                    cs.setString(1, xml);
                    try (ResultSet rs = cs.executeQuery()) {
                        if (rs.next()) {
                            int codigo = rs.getInt("CODIGO");
                            String mensaje = rs.getString("MENSAJE");
                            return new ResultadoInsercion(codigo == 1, mensaje);
                        }
                    }
                }
                return new ResultadoInsercion(false, "El procedimiento no devolvio respuesta");
            });
    }

    /**
     * Cuantas de las lineas enviadas NO existen en MaestroArticulo.
     *
     * InsertarGuiasOdooDmk hace INNER JOIN MaestroArticulo: si el articulo no
     * esta replicado, la linea se descarta SIN AVISO. Esa es la causa real del
     * "de 20 solo entraron 15". Se consulta ANTES de insertar para poder
     * reportarlo en vez de perderlo en silencio.
     */
    public List<Integer> articulosInexistentes(List<Integer> codigos) {
        if (codigos == null || codigos.isEmpty()) {
            return Collections.emptyList();
        }
        StringBuilder in = new StringBuilder();
        for (int i = 0; i < codigos.size(); i++) {
            in.append(i == 0 ? "?" : ",?");
        }
        List<Integer> existentes = jdbc.queryForList(
            "SELECT CodArticulo FROM MaestroArticulo WHERE CodArticulo IN (" + in + ")",
            Integer.class, codigos.toArray());

        List<Integer> faltantes = new ArrayList<>(codigos);
        faltantes.removeAll(existentes);
        return faltantes;
    }

    /** Ping para GET /health. */
    public boolean disponible() {
        try {
            jdbc.queryForObject("SELECT 1", Integer.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
