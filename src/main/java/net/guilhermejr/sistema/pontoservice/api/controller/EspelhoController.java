package net.guilhermejr.sistema.pontoservice.api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.guilhermejr.sistema.pontoservice.api.response.EspelhoResponse;
import net.guilhermejr.sistema.pontoservice.api.response.EspelhoResumoResponse;
import net.guilhermejr.sistema.pontoservice.api.response.ImportacaoResponse;
import net.guilhermejr.sistema.pontoservice.service.EspelhoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Log4j2
@RequiredArgsConstructor
@RestController
@PreAuthorize("hasAnyRole('PONTO')")
@RequestMapping("/espelhos")
public class EspelhoController {

    private final EspelhoService espelhoService;

    @GetMapping
    public ResponseEntity<List<EspelhoResumoResponse>> listar() {

        log.info("Listando espelhos");
        return ResponseEntity.status(HttpStatus.OK).body(espelhoService.listar());

    }

    @GetMapping("/{ano}/{mes}")
    public ResponseEntity<EspelhoResponse> detalhar(@PathVariable Integer ano, @PathVariable Integer mes) {

        log.info("Detalhando espelho {}/{}", mes, ano);
        return ResponseEntity.status(HttpStatus.OK).body(espelhoService.detalhar(ano, mes));

    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportacaoResponse> importar(@RequestParam("arquivo") MultipartFile arquivo) {

        log.info("Importando espelho: {}", arquivo.getOriginalFilename());
        return ResponseEntity.status(HttpStatus.CREATED).body(espelhoService.importar(arquivo));

    }

    @DeleteMapping("/{ano}/{mes}")
    public ResponseEntity<Void> apagar(@PathVariable Integer ano, @PathVariable Integer mes) {

        log.info("Apagando espelho {}/{}", mes, ano);
        espelhoService.apagar(ano, mes);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();

    }

}
