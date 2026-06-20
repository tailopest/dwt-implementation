import java.util.ArrayList;

/**
 * Classe responsável por extrair características de uma imagem
 * usando a Transformada Discreta de Wavelets com a wavelet Haar.
 */

public class TransformadaWavelets {
    /**
     * Constante usada na normalização da transformada de Haar.
     */
    private static final double RAIZ_INVERSA = 1.0 / Math.sqrt(2.0);

    /**
     * Aplica a Transformada Discreta de Wavelets Haar em uma imagem.
     *
     * @param input imagem de entrada em níveis de cinza
     * @param niveis quantidade de níveis de decomposição
     * @return vetor de características com energia e entropia das sub-bandas
     */

    public static double[] extrairCaracteristicas(ImageAccess input, int niveis) {
        ImageAccess coeffs = copiarImagem(input);

        ArrayList<Double> caracteristicas = new ArrayList<Double>(); //Transfere para um vetor de numeros reais

        int largura = coeffs.getWidth();
        int altura = coeffs.getHeight();

        for (int i = 1; i <= niveis; i++) {
            if (largura < 2 || altura < 2) {
                break;
            }

            if (largura % 2 != 0) {
                largura--;
            }

            if (altura % 2 != 0) {
                altura--;
            }

            aplicarHaarLinhas(coeffs, largura, altura);
            aplicarColunasHaar(coeffs, largura, altura);

            int meioL = largura / 2;
            int meioA = altura / 2;

            /*
             * Depois da DWT 2D, a região fica assim:
             *
             * LL | HL
             * ---+---
             * LH | HH
             */

            // LL
            adicionarEnergiaEntropia(caracteristicas, coeffs, 0, 0, meioL, meioA);

            // HL
            adicionarEnergiaEntropia(caracteristicas, coeffs, meioL, 0, meioL, meioA);

            // LH
            adicionarEnergiaEntropia(caracteristicas, coeffs, 0, meioA, meioL, meioA);

            // HH
            adicionarEnergiaEntropia(caracteristicas, coeffs, meioL, meioA, meioL, meioA);

            // No próximo nível, decompõe apenas a região LL
            largura = meioL;
            altura = meioA;
        }

        double[] result = new double[caracteristicas.size()];

        for (int i = 0; i < caracteristicas.size(); i++) {
            result[i] = caracteristicas.get(i);
        }

        return result;
    }

    /**
     * Cria uma cópia da imagem recebida.
     *
     * @param entrada imagem original que será copiada
     * @return uma nova imagem com os mesmos pixels da imagem original
     */
    private static ImageAccess copiarImagem(ImageAccess entrada) {
        int largura = entrada.getWidth();
        int altura = entrada.getHeight();

        // Cria uma nova imagem vazia com o mesmo tamanho da imagem original
        ImageAccess copia = new ImageAccess(largura, altura);

        for (int y = 0; y < altura; y++) {
            for (int x = 0; x < largura; x++) {
                copia.putPixel(x, y, entrada.getPixel(x, y));
            }
        }

        return copia;
    }

    /**
     * Aplica a transformada de Haar em todas as linhas da imagem.
     *
     * A metade esquerda de cada linha recebe os coeficientes de baixa frequência,
     * ou seja, as médias dos pares de pixels.
     *
     * A metade direita de cada linha recebe os coeficientes de alta frequência,
     * ou seja, as diferenças entre os pares de pixels.
     *
     * @param imagem imagem que será modificada
     * @param largura largura da região que será processada
     * @param altura altura da região que será processada
     */
    private static void aplicarHaarLinhas(ImageAccess imagem, int largura, int altura) {
        // Vetor temporário usado para guardar o resultado de uma linha transformada
        double[] temporario = new double[largura];

        for (int j = 0; j < altura; j++) {
            int metade = largura / 2;

            // Percorre os pixels da linha em pares
            for (int i = 0; i < metade; i++) {

                // Pega dois pixels consecutivos da linha
                double pixelA = imagem.getPixel(2 * i, j);
                double pixelB = imagem.getPixel(2 * i + 1, j);

                // Coeficiente de baixa frequência: representa a média do par
                double baixaFrequencia = (pixelA + pixelB) * RAIZ_INVERSA;

                // Coeficiente de alta frequência: representa a diferença do par
                double altaFrequencia = (pixelA - pixelB) * RAIZ_INVERSA;

                // Guarda os coeficientes de baixa frequência na primeira metade da linha
                temporario[i] = baixaFrequencia;

                // Guarda os coeficientes de alta frequência na segunda metade da linha
                temporario[i + metade] = altaFrequencia;
            }

            // Copia a linha transformada de volta para a imagem
            for (int i = 0; i < largura; i++) {
                imagem.putPixel(i, j, temporario[i]);
            }
        }
    }

    /**
     * Aplica a transformada de Haar em todas as colunas da imagem.
     *
     * A metade superior de cada coluna recebe os coeficientes de baixa frequência,
     * ou seja, as médias dos pares de pixels.
     *
     * A metade inferior de cada coluna recebe os coeficientes de alta frequência,
     * ou seja, as diferenças entre os pares de pixels.
     *
     * @param imagem imagem que será modificada
     * @param largura largura da região que será processada
     * @param altura altura da região que será processada
     */
    private static void aplicarColunasHaar(ImageAccess imagem, int largura, int altura) {
        // Vetor temporário usado para guardar o resultado de uma coluna transformada
        double[] temporario = new double[altura];

        // Percorre cada coluna da imagem
        for (int i = 0; i < largura; i++) {

            // Metade da altura da região processada
            int metade = altura / 2;

            // Percorre os pixels da coluna em pares
            for (int j = 0; j < metade; j++) {

                // Pega dois pixels consecutivos da coluna
                double pixelA = imagem.getPixel(i, 2 * j);
                double pixelB = imagem.getPixel(i, 2 * j + 1);

                // Coeficiente de baixa frequência: representa a média do par
                double baixaFrequencia = (pixelA + pixelB) * RAIZ_INVERSA;

                // Coeficiente de alta frequência: representa a diferença do par
                double altaFrequencia = (pixelA - pixelB) * RAIZ_INVERSA;

                // Guarda os coeficientes de baixa frequência na primeira metade da coluna
                temporario[j] = baixaFrequencia;

                // Guarda os coeficientes de alta frequência na segunda metade da coluna
                temporario[j + metade] = altaFrequencia;
            }

            // Copia a coluna transformada de volta para a imagem
            for (int j = 0; j < altura; j++) {
                imagem.putPixel(i, j, temporario[j]);
            }
        }
    }

    /**
     * Calcula a energia e a entropia de uma sub-banda da imagem
     * e adiciona esses valores ao vetor de características.
     *
     * @param caracteristicas lista onde serão armazenadas as características extraídas
     * @param imagem imagem contendo os coeficientes da DWT
     * @param inicioX posição inicial da sub-banda no eixo horizontal
     * @param inicioY posição inicial da sub-banda no eixo vertical
     * @param largura largura da sub-banda
     * @param altura altura da sub-banda
     */
    private static void adicionarEnergiaEntropia(
        ArrayList<Double> caracteristicas,
        ImageAccess imagem,
        int inicioX,
        int inicioY,
        int largura,
        int altura
    ) {
        // Calcula a energia da sub-banda selecionada
        double energia = calcularEnergia(imagem, inicioX, inicioY, largura, altura);

        double energiaMedia = energia / (largura * altura);

        // Calcula a entropia da sub-banda selecionada
        double entropia = calcularEntropia(imagem, inicioX, inicioY, largura, altura, energia);

        // Adiciona a energia ao vetor de características
        caracteristicas.add(energiaMedia);

        // Adiciona a entropia ao vetor de características
        caracteristicas.add(entropia);
    }

    /**
     * Calcula a energia de uma sub-banda da imagem.
     *
     * A energia é calculada pela soma dos quadrados dos coeficientes
     * presentes na região selecionada.
     *
     * @param imagem imagem contendo os coeficientes da DWT
     * @param inicioX posição inicial da sub-banda no eixo horizontal
     * @param inicioY posição inicial da sub-banda no eixo vertical
     * @param largura largura da sub-banda
     * @param altura altura da sub-banda
     * @return valor da energia da sub-banda
     */
    private static double calcularEnergia(
        ImageAccess imagem,
        int inicioX,
        int inicioY,
        int largura,
        int altura
    ) {
        // Acumulador da soma dos quadrados dos coeficientes
        double soma = 0.0;

        // Percorre as linhas da sub-banda
        for (int j = inicioY; j < inicioY + altura; j++) {

            // Percorre as colunas da sub-banda
            for (int i = inicioX; i < inicioX + largura; i++) {

                // Obtém o coeficiente da posição atual
                double valor = imagem.getPixel(i, j);

                // Soma o quadrado do coeficiente
                soma += valor * valor;
            }
        }

        // Retorna a energia calculada
        return soma;
    }

    /**
     * Calcula a entropia de uma sub-banda da imagem.
     *
     * A entropia mede a distribuição da energia dos coeficientes
     * dentro da sub-banda.
     *
     * @param imagem imagem contendo os coeficientes da DWT
     * @param inicioX posição inicial da sub-banda no eixo horizontal
     * @param inicioY posição inicial da sub-banda no eixo vertical
     * @param largura largura da sub-banda
     * @param altura altura da sub-banda
     * @param energia energia previamente calculada da sub-banda
     * @return valor da entropia da sub-banda
     */
    private static double calcularEntropia(
        ImageAccess imagem,
        int inicioX,
        int inicioY,
        int largura,
        int altura,
        double energia
    ) {
        // Se a energia for zero, não há informação útil para calcular a entropia
        if (energia == 0.0) {
            return 0.0;
        }

        // Acumulador da entropia
        double entropia = 0.0;

        // Percorre as linhas da sub-banda
        for (int j = inicioY; j < inicioY + altura; j++) {

            // Percorre as colunas da sub-banda
            for (int i = inicioX; i < inicioX + largura; i++) {

                // Obtém o coeficiente da posição atual
                double valor = imagem.getPixel(i, j);

                // Calcula a proporção de energia desse coeficiente
                double p = (valor * valor) / energia;              

                // Evita calcular logaritmo de zero
                if (p > 0.0) {
                    entropia -= p * (Math.log(p) / Math.log(2.0));
                }
            }
        }

        // Retorna a entropia calculada
        return entropia;
    }

    /**
     * Calcula a distância euclidiana entre dois vetores de características.
     *
     * Essa distância pode ser usada no Wavelets_Haar para medir o quão parecidas
     * duas imagens são a partir de seus vetores de energia e entropia.
     *
     * @param a primeiro vetor de características
     * @param b segundo vetor de características
     * @return distância euclidiana entre os dois vetores
     */
    public static double distanciaEuclidiana(double[] a, double[] b) {
        // Usa o menor tamanho entre os dois vetores para evitar erro de índice
        int tamanho = Math.min(a.length, b.length);

        // Acumulador da soma dos quadrados das diferenças
        double soma = 0.0;

        // Percorre os elementos dos vetores
        for (int i = 0; i < tamanho; i++) {

            // Calcula a diferença entre as características na mesma posição
            double diferenca = a[i] - b[i];

            // Soma o quadrado da diferença
            soma += diferenca * diferenca;
        }

        // Retorna a raiz quadrada da soma, que é a distância euclidiana
        return Math.sqrt(soma);
    }

    public static double distanciaManhattan(double[] a, double[] b) {
        int tamanho = Math.min(a.length, b.length);
        double soma = 0.0;

        for (int i = 0; i < tamanho; i++) {
            soma += Math.abs(a[i] - b[i]);
        }

        return soma;
    }

    public static double distanciaInfinity(double[] a, double[] b) {
        int tamanho = Math.min(a.length, b.length);
        double maior = 0.0;

        for (int i = 0; i < tamanho; i++) {
            double diferenca = Math.abs(a[i] - b[i]);

            if (diferenca > maior) {
                maior = diferenca;
            }
        }

        return maior;
    }
}