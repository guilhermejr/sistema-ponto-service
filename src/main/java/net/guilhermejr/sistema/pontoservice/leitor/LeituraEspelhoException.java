package net.guilhermejr.sistema.pontoservice.leitor;

/** O arquivo não tem o formato do espelho de ponto. A mensagem vai para a tela. */
public class LeituraEspelhoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public LeituraEspelhoException(String mensagem) {
        super(mensagem);
    }

    public LeituraEspelhoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }

}
