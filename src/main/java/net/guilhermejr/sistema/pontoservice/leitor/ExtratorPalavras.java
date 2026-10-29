package net.guilhermejr.sistema.pontoservice.leitor;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Tira do PDF o texto corrido (para o cabeçalho) e cada palavra com a sua posição
 * (para a tabela). Quase todos os espelhos vêm criptografados com senha só de
 * proprietário; o PDFBox os abre sem senha.
 */
public final class ExtratorPalavras {

    private ExtratorPalavras() {
    }

    public record Conteudo(String texto, List<Palavra> palavras) {
    }

    public static Conteudo extrair(byte[] pdf) {
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            Coletor coletor = new Coletor();
            coletor.setSortByPosition(true);
            String texto = coletor.getText(documento);
            return new Conteudo(texto, coletor.palavras);
        } catch (IOException e) {
            throw new LeituraEspelhoException("O arquivo não é um PDF válido.", e);
        }
    }

    /** Distância entre dois glifos, em pontos, a partir da qual começa outra palavra. */
    private static final float SALTO = 2.5f;

    /**
     * O PDFTextStripper entrega trechos que atravessam colunas ("05/09/2026 (sáb) 05:17",
     * com a data na primeira coluna e o acúmulo na sétima). As palavras são remontadas
     * glifo a glifo, quebrando nos espaços e nos saltos de posição.
     */
    private static class Coletor extends PDFTextStripper {

        private final List<Palavra> palavras = new ArrayList<>();

        Coletor() throws IOException {
            super();
        }

        @Override
        protected void writeString(String texto, List<TextPosition> posicoes) throws IOException {
            super.writeString(texto, posicoes);
            List<TextPosition> palavra = new ArrayList<>();
            for (TextPosition glifo : posicoes) {
                boolean espaco = glifo.getUnicode().isBlank();
                if (!palavra.isEmpty() && (espaco || glifo.getXDirAdj() - fim(palavra.getLast()) > SALTO)) {
                    guardar(palavra);
                    palavra = new ArrayList<>();
                }
                if (!espaco) {
                    palavra.add(glifo);
                }
            }
            if (!palavra.isEmpty()) {
                guardar(palavra);
            }
        }

        private void guardar(List<TextPosition> glifos) {
            StringBuilder texto = new StringBuilder();
            glifos.forEach(g -> texto.append(g.getUnicode()));
            palavras.add(new Palavra(getCurrentPageNo(), glifos.getFirst().getXDirAdj(), fim(glifos.getLast()),
                    glifos.getFirst().getYDirAdj(), texto.toString()));
        }

        private static float fim(TextPosition glifo) {
            return glifo.getXDirAdj() + glifo.getWidthDirAdj();
        }

    }

}
