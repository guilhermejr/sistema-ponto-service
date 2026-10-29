package net.guilhermejr.sistema.pontoservice.api.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Resultado do envio de um espelho. {@code substituido}: o mês já existia e foi trocado. */
@Getter
@AllArgsConstructor
public class ImportacaoResponse {

    private EspelhoResumoResponse espelho;
    private boolean substituido;

}
