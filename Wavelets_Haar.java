import java.io.*;
import java.util.*;
import java.awt.Desktop;
import ij.*;
import ij.io.*;
import ij.ImagePlus;
import ij.process.*;
import ij.gui.*;
import ij.plugin.filter.*;
import ij.io.DirectoryChooser;

/**
 * Plugin ImageJ para busca de imagens por similaridade usando a
 * Transformada Discreta de Wavelets Haar e o algoritmo K-nearest.
 */

public class Wavelets_Haar implements PlugInFilter {
    /** Imagem de referência */
    ImagePlus referencia;

    /** Quantidade de vizinhos mais próximos */
    int k;

    /** Número de níveis de decomposição da DWT Haar. */
    int nivel;

    /** Função de distância escolhida: "Euclidiana", "Manhattan" ou "Infinity". */
    String funcaoDistancia;

    /**
     * Inicializa o plugin com a imagem aberta no ImageJ.
     *
     * Armazena a imagem de referência e a converte para escala de
     * cinza de 8 bits, formato utilizado na extração das características
     * pela Transformada Wavelet Haar.
     *
     * @param arg argumento passado pelo ImageJ ao plugin
     * @param imp imagem ativa no momento da execução do plugin
     * @return indica que o plugin aceita apenas imagens 8 bits
     */

    public int setup(String arg, ImagePlus imp) {
        referencia = imp;
        ImageConverter ic = new ImageConverter(imp);
        ic.convertToGray8();
        return DOES_8G;
    }
 
    /**
     * Exibe o diálogo de configuração e inicia a busca.
     *
     * Coleta do usuário: número de vizinhos K, nível de decomposição
     * e função de distância. Em seguida, solicita a pasta da base de imagens
     * e realiza a busca
     *
     * @param img processador da imagem ativa
     */

    public void run(ImageProcessor img) {

        // Cria a janela de configuração do plugin
        GenericDialog gd = new GenericDialog("Wavelets Haar - KNN", IJ.getInstance());

        // Campo para definir a quantidade de vizinhos mais próximos
        gd.addNumericField("Quantidade de vizinhos mais proximos (K): ", 1, 0);

        // Campo para definir o nível de decomposição da Wavelet Haar
        gd.addNumericField("Nivel de decomposicao Wavelet: ", 1, 0);

        // Lista de opções para escolha da função de distância
        gd.addChoice("Funcao de distancia:",
            new String[]{"Euclidiana", "Manhattan", "Infinity"},
            "Euclidiana"
        );

        gd.showDialog();

        // Encerra a execução caso o usuário cancele a operação
        if (gd.wasCanceled()) 
            return;

        // Obtém o valor de K informado pelo usuário
        k = (int) gd.getNextNumber();

        if (k < 1){
            IJ.error("K deve ser maior ou igual a 1.");
            return;
        }
        

        // Obtém o nível de decomposição informado pelo usuário
        nivel = (int) gd.getNextNumber();

        if (nivel < 1){
            IJ.error("Nivel deve ser maior ou igual a 1.");
            return;
        }

        // Obtém a função de distância selecionada
        funcaoDistancia = gd.getNextChoice();

        DirectoryChooser dc = new DirectoryChooser("Selecionar pasta da base de imagens");
        String dir = dc.getDirectory();

        // Encerra a execução se nenhuma pasta for selecionada
        if (dir == null) return;

        // Inicia o processo de busca
        buscar(dir);
    }

    /**
     * Executa a busca K-NN na pasta selecionada.
     *
     * Para cada imagem válida encontrada na pasta (excluindo a própria
     * imagem de referência), extrai o vetor de características e calcula 
     * a distância em relação à referência. Ao final, salva os
     * vetores e o resultado da busca em arquivos de texto.
     *
     * @param dir caminho da pasta contendo a base de imagens
     */

    public void buscar(String dir) {

        IJ.log("Procurando imagens...");
        IJ.log("");

        IJ.log("PARAMETROS");
        IJ.log("- Funcao de distancia: " + funcaoDistancia);
        IJ.log("- K: " + k);
        IJ.log("- Nivel Wavelet: " + nivel);

        // Garante que o caminho da pasta termine com o separador correto
        if (!dir.endsWith(File.separator))
            dir += File.separator;

        // Obtém a lista de arquivos da pasta
        String[] lista = new File(dir).list(); 

        // Encerra o método caso a pasta não possa ser lida
        if (lista == null) return;

        // Lista que armazenará os resultados da busca
        ArrayList<Resultado> resultados = new ArrayList<Resultado>();

        // Cria um objeto ImageAccess para acessar os pixels da imagem de referência
        ImageAccess refAccess = new ImageAccess(referencia.getProcessor());

        // Extrai o vetor de características
        double[] vetorReferencia = TransformadaWavelets.extrairCaracteristicas(refAccess, nivel);

        // Percorre os arquivos encontrados na pasta
        for (int i = 0; i < lista.length; i++) {
            // Mostra o arquivo atual sendo processado
            IJ.showStatus(i + "/" + lista.length + ": " + lista[i]); 

            // Atualiza a barra de progresso
            IJ.showProgress((double) i / lista.length);

            // Cria um objeto File para representar o item atual da pasta
            File f = new File(dir + lista[i]);

            // Processa apenas arquivos, ignorando subpastas
            if (!f.isDirectory()) {
                // Ignora a imagem de referência, caso ela esteja dentro da pasta da base
                if (lista[i].equals(referencia.getTitle())) {
                    continue;
                }

                // Abre imagem atual da base
                ImagePlus image = new Opener().openImage(dir, lista[i]); 

                // Verifica se a imagem foi aberta corretamente
                if (image != null) {

                    // Converte a imagem para tons de cinza de 8 bits
                    ImageConverter ic = new ImageConverter(image);
                    ic.convertToGray8();

                    // Cria um objeto ImageAccess para acessar os pixels da imagem atual
                    ImageAccess input = new ImageAccess(image.getProcessor());

                    // Extrai o vetor de características da imagem atual
                    double[] vetorBusca = TransformadaWavelets.extrairCaracteristicas(input, nivel);

                    // Calcula a distância entre a imagem de referência e a imagem atual
                    double distancia = calcularDistancia(
                        vetorReferencia,
                        vetorBusca
                    );

                    // Adiciona o nome da imagem, a distância e o vetor na lista de resultados
                    resultados.add(new Resultado(lista[i], distancia, vetorBusca));
                }
            }
        }

        // Salva os vetores de características calculados em arquivo
        salvarVetoresCaracteristicas(dir, vetorReferencia, resultados);

        // Ordena os resultados pela menor distância
        Collections.sort(resultados);

        // Exibe o resultado da busca
        imprimirResultado(vetorReferencia, resultados);

        // Salva o resultado final da busca em arquivo
        salvarResultado(dir, vetorReferencia, resultados);

        IJ.log("");
        IJ.log("ARQUIVOS GERADOS");
        IJ.log("- vetores_caracteristicas.txt");
        IJ.log("- resultado_wavelets.txt");

        // Abre os arquivos de resultado automaticamente
        try {
            Desktop.getDesktop().open(new File(dir + "vetores_caracteristicas.txt"));
            Desktop.getDesktop().open(new File(dir + "resultado_wavelets.txt"));
        } catch (IOException e) {
            IJ.error("Erro ao abrir os arquivos de resultado.");
        }

        // Finaliza a barra de progresso
        IJ.showProgress(1.0);

        // Limpa a mensagem da barra de status
        IJ.showStatus("");
    }

    /**
     * Calcula a distância entre dois vetores de características usando
     * a função de distância selecionada pelo usuário.
     *
     * @param a primeiro vetor de características
     * @param b segundo vetor de características
     * @return distância calculada entre os dois vetores
     */

    public double calcularDistancia(double[] a, double[] b) {
        if (funcaoDistancia.equals("Manhattan")) {
            return TransformadaWavelets.distanciaManhattan(a, b);
        }

        if (funcaoDistancia.equals("Infinity")) {
            return TransformadaWavelets.distanciaInfinity(a, b);
        }

        return TransformadaWavelets.distanciaEuclidiana(a, b);
    }

    /**
     * Exibe no log do ImageJ o vetor da imagem de referência e os
     * K resultados mais próximos com seus respectivos vetores e distâncias.
     *
     * @param vetorReferencia vetor de características da imagem de referência
     * @param resultados lista de resultados já ordenada por distância crescente
     */

    public void imprimirResultado(
        double[] vetorReferencia,
        ArrayList<Resultado> resultados
    ) {
        IJ.log("");
        IJ.log("VETOR PARA IMAGEM DE REFERENCIA");
        imprimirVetor(vetorReferencia);

        IJ.log("");
        IJ.log("K IMAGENS VIZINHAS MAIS PROXIMAS");

        // Define quantos resultados serão exibidos
        int limite = Math.min(k, resultados.size());

        // Percorre os K vizinhos mais próximos
        for (int i = 0; i < limite; i++) {
            // Obtém o resultado atual
            Resultado r = resultados.get(i);

            IJ.log("");
            IJ.log((i + 1) + " - " + r.nomeArquivo);

            IJ.log("Distancia: " + IJ.d2s(r.distancia, 3));

            IJ.log("Vetor:");
            imprimirVetor(r.vetor);
        }
    }

    /**
     * Salva o resultado da busca K-NN em "resultado_wavelets.txt"
     * na mesma pasta da base de imagens.
     *
     * O arquivo contém os parâmetros usados, o vetor da imagem de
     * referência e os K resultados mais próximos com distância e vetor.
     *
     * @param dir caminho da pasta onde o arquivo será salvo
     * @param vetorReferencia vetor de características da imagem de referência
     * @param resultados lista de resultados ordenada por distância crescente
     */

    public void salvarResultado(
        String dir,
        double[] vetorReferencia,
        ArrayList<Resultado> resultados
    ) {
        try {
            // Cria o arquivo de saída na pasta da base de imagens
            PrintWriter arquivo =
                new PrintWriter(new FileWriter(dir + "resultado_wavelets.txt"));

            arquivo.println("PARAMETROS");
            arquivo.println("- Funcao de distancia: " + funcaoDistancia);
            arquivo.println("- K: " + k);
            arquivo.println("- Nivel: " + nivel);
            arquivo.println("");

            arquivo.println("IMAGEM DE REFERENCIA");
            arquivo.println("");
            arquivo.println(vetorParaString(vetorReferencia));
            arquivo.println("");

            arquivo.println("RESULTADO K-NN");
            arquivo.println("");

            // Define quantos resultados serão gravados
            int limite = Math.min(k, resultados.size());

            for (int i = 0; i < limite; i++) {
                Resultado r = resultados.get(i);

                arquivo.println((i + 1) + " - " + r.nomeArquivo);
                arquivo.println("Distancia: " + IJ.d2s(r.distancia, 3));
                arquivo.println("Vetor:");
                arquivo.println(vetorParaString(r.vetor));
            }

            // Fecha o arquivo para garantir a gravação dos dados
            arquivo.close();

        } catch (IOException e) {
            IJ.error("Erro ao salvar resultado");
        }
    }

    /**
     * Salva todos os vetores de características extraídos em
     * "vetores_caracteristicas.txt" na pasta da base de imagens.
     *
     * O arquivo contém o vetor da imagem de referência e os vetores
     * de todas as imagens da base, independentemente do K escolhido.
     *
     * @param dir caminho da pasta onde o arquivo será salvo
     * @param vetorReferencia vetor de características da imagem de referência
     * @param resultados lista com todas as imagens da base e seus vetores
     */

    public void salvarVetoresCaracteristicas(String dir, double[] vetorReferencia, ArrayList<Resultado> resultados) {
        try {
            // Cria o arquivo que armazenará os vetores de características
            PrintWriter arquivo = new PrintWriter(
                new FileWriter(dir + "vetores_caracteristicas.txt")
            );

            arquivo.println("PARAMETROS");
            arquivo.println("- Nivel: " + nivel);
            arquivo.println("");

            arquivo.println("IMAGEM DE REFERENCIA");
            arquivo.println("referencia; " + vetorParaString(vetorReferencia));
            arquivo.println("");

            arquivo.println("IMAGENS DA BASE");

            for (Resultado r : resultados) {
                arquivo.println(
                    r.nomeArquivo + "; " + vetorParaString(r.vetor) + "\n"
                );
            }

            // Fecha o arquivo para concluir a gravação
            arquivo.close();

        } catch (IOException e) {
            IJ.error("Erro ao salvar os vetores de características.");
        }
    }

    /**
     * Converte um vetor de doubles em uma String formatada com 3 casas decimais,
     * separando os valores por vírgula e espaço.
     *
     * @param vetor vetor de valores a ser convertido
     * @return representação textual do vetor
     */

    public String vetorParaString(double[] vetor) {
        // Armazena a representação textual do vetor
        StringBuffer linha = new StringBuffer();

        for (int i = 0; i < vetor.length; i++) {
            // Adiciona o valor formatado com 3 casas decimais
            linha.append(IJ.d2s(vetor[i], 3));

            // Adiciona separador entre os elementos
            if (i < vetor.length - 1) {
                linha.append(", ");
            }
        }

        return linha.toString();
    }

    /**
     * Exibe um vetor de características no log do ImageJ.
     *
     * @param vetor vetor de valores a ser exibido
     */

    public void imprimirVetor(double[] vetor) {
        IJ.log(vetorParaString(vetor));
    }
}

/**
 * Representa o resultado da comparação entre a imagem de referência
 * e uma imagem da base de dados.
 */

class Resultado implements Comparable<Resultado> {
    String nomeArquivo;
    double distancia;
    double[] vetor;

    /**
     * Cria um novo resultado.
     *
     * @param nomeArquivo nome do arquivo da imagem
     * @param distancia distância em relação à imagem de referência
     * @param vetor vetor de características da imagem
     */

    Resultado(String nomeArquivo, double distancia, double[] vetor) {
        this.nomeArquivo = nomeArquivo;
        this.distancia = distancia;
        this.vetor = vetor;
    }

    /**
     * Compara este resultado com outro pela distância, em ordem crescente.
     *
     * @param outro outro resultado a ser comparado
     * @return -1 se este for mais próximo, 1 se for mais distante, 0 se igual
     */

    public int compareTo(Resultado outro) {
        if (this.distancia < outro.distancia) return -1;
        if (this.distancia > outro.distancia) return 1;
        return 0;
    }
}