package net.guilhermejr.sistema.pontoservice.leitor;

/**
 * Uma palavra do PDF com a sua posição. Coordenadas em pontos, com a origem no canto
 * superior esquerdo da página (y cresce para baixo), como o PDFBox entrega.
 */
public record Palavra(int pagina, float x, float xFim, float y, String texto) {

    public float centro() {
        return (x + xFim) / 2;
    }

}
