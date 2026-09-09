package pe.dbperu.gre.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import pe.dbperu.gre.domain.GuiaRequest;
import pe.dbperu.gre.domain.GuiaResponse;
import pe.dbperu.gre.service.GuiaService;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/guias")
@Validated
public class GuiaController {

    private final GuiaService service;

    public GuiaController(GuiaService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<GuiaResponse> registrar(@Valid @RequestBody GuiaRequest req) {
        GuiaResponse resp = service.registrar(req);
        return resp.isExito()
                ? ResponseEntity.status(HttpStatus.CREATED).body(resp)
                : ResponseEntity.unprocessableEntity().body(resp);
    }
}
