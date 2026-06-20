// Plugin para Wavelets Haar adaptado do exemplo para k-nearest 

import java.io.*;
import java.util.*;
import ij.*;
import ij.io.*;
import ij.ImagePlus;
import ij.process.*;
import ij.gui.*;
import ij.plugin.filter.*;
import ij.io.DirectoryChooser;

public class Wavelets_Haar implements PlugInFilter {
    ImagePlus referencia; // Imagem de referência
    int k; // Quantidade de vizinhos próximos
    int nivel; // Nível de decomposição Wavelet
    String funcaoDistancia; 

    public int setup(String arg, ImagePlus imp) {
        referencia = imp;
        ImageConverter ic = new ImageConverter(imp);
        ic.convertToGray8();
        return DOES_8G;
    }

    public void run(ImageProcessor img) {

        GenericDialog gd = new GenericDialog("Wavelets Haar - KNN", IJ.getInstance());
        gd.addNumericField("Quantidade de vizinhos mais proximos (K): ", 1, 0);
        gd.addNumericField("Nivel de decomposicao Wavelet: ", 1, 0);
        gd.addChoice(
            "Funcao de distancia:",
            new String[]{"Euclidiana", "Manhattan", "Infinity"},
            "Euclidiana"
        );

        gd.showDialog();

        if (gd.wasCanceled()) 
            return;

        k = (int) gd.getNextNumber();

        if(k < 0){
            IJ.error("K deve ser maior ou igual a 1.");
        }

        nivel = (int) gd.getNextNumber();

        if(nivel < 1){
            IJ.error("Nivel deve ser maior ou igual a 1.");
        }

        funcaoDistancia = gd.getNextChoice();
        // Seleciona o diretório que contém as imagens da base de dados que serão comparadas à imagem de referência.
        DirectoryChooser dc = new DirectoryChooser("Selecionar pasta da base de imagens");
        String dir = dc.getDirectory();

        if (dir == null) return;

        buscar(dir);
    }

    public void buscar(String dir) {
        IJ.log("");
        IJ.log("Procurando imagens...");
        IJ.log("Funcao de distancia: " + funcaoDistancia);

        if (!dir.endsWith(File.separator))
            dir += File.separator;
        String[] lista = new File(dir).list(); // lista de arquivos
        if (lista == null) return;

        ArrayList<Resultado> resultados = new ArrayList<Resultado>();
        
        ImageAccess refAccess = new ImageAccess(referencia.getProcessor());
        double[] vetorReferencia = TransformadaWavelets.extrairCaracteristicas(refAccess, nivel);

        for (int i = 0; i < lista.length; i++) {
            IJ.showStatus(i + "/" + lista.length + ": " + lista[i]); // mostra na interface
            IJ.showProgress((double) i / lista.length); // 

            File f = new File(dir + lista[i]);

            if (!f.isDirectory()) {
                if (lista[i].equals(referencia.getTitle())) {
                    continue;
                }

                ImagePlus image = new Opener().openImage(dir, lista[i]); // abre imagem image 

                if (image != null) {
                    ImageConverter ic = new ImageConverter(image);
                    ic.convertToGray8();

                    ImageAccess input = new ImageAccess(image.getProcessor());

                    double[] vetorBusca = TransformadaWavelets.extrairCaracteristicas(input, nivel);

                    double distancia = calcularDistancia(
                        vetorReferencia,
                        vetorBusca
                    );

                    resultados.add(new Resultado(lista[i], distancia, vetorBusca));
                }
            }
        }

        salvarVetoresCaracteristicas(dir, vetorReferencia, resultados);

        Collections.sort(resultados);

        imprimirResultado(vetorReferencia, resultados);

        salvarResultado(dir, vetorReferencia, resultados);

        IJ.showProgress(1.0);
        IJ.showStatus("");
    }

    // Método auxiliar de buscar()
    public double calcularDistancia(double[] a, double[] b) {
        if (funcaoDistancia.equals("Manhattan")) {
            return TransformadaWavelets.distanciaManhattan(a, b);
        }

        if (funcaoDistancia.equals("Infinity")) {
            return TransformadaWavelets.distanciaInfinity(a, b);
        }

        return TransformadaWavelets.distanciaEuclidiana(a, b);
    }

    // Saída
    public void imprimirResultado(
        double[] vetorReferencia,
        ArrayList<Resultado> resultados
    ) {
        IJ.log("");
        IJ.log("Vetor para a imagem de referencia:");
        imprimirVetor(vetorReferencia);

        IJ.log("");
        IJ.log("K imagens vizinhas mais proximas:");

        int limite = Math.min(k, resultados.size());

        for (int i = 0; i < limite; i++) {
            Resultado r = resultados.get(i);

            IJ.log("");
            IJ.log((i + 1) + " - " + r.nomeArquivo);
            IJ.log("Distancia: " + IJ.d2s(r.distancia, 3));
            IJ.log("Vetor:");
            imprimirVetor(r.vetor);
        }
    }

    public void salvarResultado(
        String dir,
        double[] vetorReferencia,
        ArrayList<Resultado> resultados
    ) {
        try {
            PrintWriter arquivo =
                new PrintWriter(new FileWriter(dir + "resultado_wavelets.txt"));

            arquivo.println("PARAMETROS");
            arquivo.println("Funcao de distancia: " + funcaoDistancia);
            arquivo.println("K: " + k);
            arquivo.println("Nivel: " + nivel);
            arquivo.println("");

            arquivo.println("IMAGEM DE REFERENCIA");
            arquivo.println(vetorParaString(vetorReferencia));
            arquivo.println("");

            arquivo.println("RESULTADO K-NN");

            int limite = Math.min(k, resultados.size());

            for (int i = 0; i < limite; i++) {
                Resultado r = resultados.get(i);

                arquivo.println("");
                arquivo.println((i + 1) + " - " + r.nomeArquivo);
                arquivo.println("Distancia: " + IJ.d2s(r.distancia, 3));
                arquivo.println("Vetor:");
                arquivo.println(vetorParaString(r.vetor));
            }

            arquivo.close();

        } catch (IOException e) {
            IJ.error("Erro ao salvar resultado");
        }
    }

    public void salvarVetoresCaracteristicas(String dir, double[] vetorReferencia, ArrayList<Resultado> resultados) {
        try {
            PrintWriter arquivo = new PrintWriter(
                new FileWriter(dir + "vetores_caracteristicas.txt")
            );

            arquivo.println("PARAMETROS");
            arquivo.println("Nivel Wavelet: " + nivel);
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

            arquivo.close();

            IJ.log("Vetores salvos em: vetores_caracteristicas.txt");

        } catch (IOException e) {
            IJ.error("Erro ao salvar os vetores de características.");
        }
    }

    // Utilitários
    public String vetorParaString(double[] vetor) {
        StringBuffer linha = new StringBuffer();

        for (int i = 0; i < vetor.length; i++) {
            linha.append(IJ.d2s(vetor[i], 3));

            if (i < vetor.length - 1) {
                linha.append(", ");
            }
        }

        return linha.toString();
    }

    public void imprimirVetor(double[] vetor) {
        IJ.log(vetorParaString(vetor));
    }
}

class Resultado implements Comparable<Resultado> {
    String nomeArquivo;
    double distancia;
    double[] vetor;

    Resultado(String nomeArquivo, double distancia, double[] vetor) {
        this.nomeArquivo = nomeArquivo;
        this.distancia = distancia;
        this.vetor = vetor;
    }

    public int compareTo(Resultado outro) {
        if (this.distancia < outro.distancia) return -1;
        if (this.distancia > outro.distancia) return 1;
        return 0;
    }
}