package pe.dbperu.gre.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import pe.dbperu.gre.domain.ResultadoInsercion;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Acceso a los stored procedures del DataMart.
 *
 * Los SPs del ERP NO se reescriben: funcionan y son logica de negocio de anios.
 * Esta clase solo los invoca y traduce su respuesta.
 */
@Repository
public class GuiaRepository {

    private static final Logger log = LoggerFactory.getLogger(GuiaRepository.class);

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

    /**
     * ¿La guia aterrizo de verdad en el DataMart?
     *
     * prc_InsertGuiaDMKWeb devuelve "Registro Exitoso en DMK" apenas mete la
     * guia en la COLA (GuiaRemision_Odoo), sin importar si InsertarGuiasOdooDmk
     * llego a moverla a las tablas reales. Ese procedimiento puede rechazarla
     * mas adelante -por documento incompleto, por articulo sin replicar, por
     * una tabla temporal pisada- y aun asi el codigo de retorno dice 1.
     *
     * Medido sobre datos reales: 45 guias marcadas estadoproceso=1 y solo 38
     * en GuiaRemision. Ocho se dieron por registradas sin existir.
     *
     * Por eso no se le cree al codigo de retorno: se comprueba.
     */
    public boolean existeEnDataMart(int anio, String tipoGuia, int numSerie, long numeroGuia) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM GuiaRemision "
              + "WHERE AnioGuiaRemision = ? AND TipoGuia = ? AND NumSerie = ? AND NumeroGuia = ?",
                Integer.class, anio, tipoGuia, numSerie, numeroGuia);
        return n != null && n > 0;
    }

    /**
     * El motivo del rechazo, tal como lo dejo el procedimiento del ERP.
     * Sin esto el usuario solo ve "no se registro", sin saber por que.
     */
    public String motivoRechazo(int anio, int numSerie, long numeroGuia) {
        try {
            List<Map<String, Object>> filas = jdbc.queryForList(
                    "SELECT TOP 1 msg_error FROM audit_InsertarGuiasOdooDmk "
                  + "WHERE anioguia = ? AND numserie = ? AND numeroguia = ? AND msg_error IS NOT NULL "
                  + "ORDER BY fecharegistro DESC",
                    anio, numSerie, numeroGuia);
            if (! filas.isEmpty()) {
                Object v = filas.get(0).get("msg_error");
                if (v != null) { return String.valueOf(v).trim(); }
            }
        } catch (Exception e) {
            log.warn("No se pudo leer audit_InsertarGuiasOdooDmk: {}", e.getMessage());
        }
        return null;
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
