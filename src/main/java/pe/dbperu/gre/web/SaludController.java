package pe.dbperu.gre.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.dbperu.gre.repository.GuiaRepository;

import java.util.LinkedHashMap;
import java.util.Map;

/** Para saber, desde fuera, en que estado esta la instalacion de cada cliente. */
@RestController
@RequestMapping("/api/v1")
public class SaludController {

    private final GuiaRepository repo;
    private final String version;
    private final String tasaIgv;

    public SaludController(GuiaRepository repo,
                           @Value("${gre.version:1.0.0}") String version,
                           @Value("${gre.igv.tasa:0.18}") String tasaIgv) {
        this.repo = repo;
        this.version = version;
        this.tasaIgv = tasaIgv;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        boolean db = repo.disponible();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", db ? "UP" : "DOWN");
        m.put("version", version);
        m.put("sqlServer", db ? "UP" : "DOWN");
        m.put("igvTasa", tasaIgv);
        return m;
    }
}
