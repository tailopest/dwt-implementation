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
    ImagePlus reference;
    int k;
    int level;
    String distanceFunction;

    public int setup(String arg, ImagePlus imp) {
        reference = imp;
        ImageConverter ic = new ImageConverter(imp);
        ic.convertToGray8();
        return DOES_8G;
    }

    public void run(ImageProcessor img) {

        GenericDialog gd = new GenericDialog("Wavelets Haar - KNN", IJ.getInstance());
        gd.addNumericField("Number of nearest neighbors (K):", 1, 0);
        gd.addNumericField("Wavelet decomposition level:", 1, 0);
        gd.addChoice(
            "Distance function:",
            new String[]{"Euclidean", "Manhattan", "Infinity"},
            "Euclidean"
        );

        gd.showDialog();

        if (gd.wasCanceled()) 
            return;

        k = (int) gd.getNextNumber();
        level = (int) gd.getNextNumber();
        distanceFunction = gd.getNextChoice();

        DirectoryChooser dc = new DirectoryChooser("Select image database folder");
        String dir = dc.getDirectory();

        if (dir == null) return;

        search(dir);
    }

    public void search(String dir) {
        IJ.log("");
        IJ.log("Searching images...");
        IJ.log("Distance function: " + distanceFunction);

        if (!dir.endsWith(File.separator))
            dir += File.separator;

        String[] list = new File(dir).list();
        if (list == null) return;

        ArrayList<Resultado> resultados = new ArrayList<Resultado>();

        ImageAccess refAccess = new ImageAccess(reference.getProcessor());
        double[] vetorReferencia =
            TransformadaWavelets.extrairCaracteristicas(refAccess, level);

        IJ.log("");
        IJ.log("Reference image vector:");
        imprimirVetor(vetorReferencia);

        for (int i = 0; i < list.length; i++) {
            IJ.showStatus(i + "/" + list.length + ": " + list[i]);
            IJ.showProgress((double) i / list.length);

            File f = new File(dir + list[i]);

            if (!f.isDirectory()) {
                ImagePlus image = new Opener().openImage(dir, list[i]);

                if (image != null) {
                    ImageConverter ic = new ImageConverter(image);
                    ic.convertToGray8();

                    ImageAccess input = new ImageAccess(image.getProcessor());

                    double[] vetorBusca =
                        TransformadaWavelets.extrairCaracteristicas(input, level);

                    double distancia = calcularDistancia(
                        vetorReferencia,
                        vetorBusca
                    );

                    resultados.add(new Resultado(list[i], distancia, vetorBusca));
                }
            }
        }

        Collections.sort(resultados);

        IJ.log("");
        IJ.log("K nearest images:");

        int limite = Math.min(k, resultados.size());

        for (int i = 0; i < limite; i++) {
            Resultado r = resultados.get(i);

            IJ.log("");
            IJ.log((i + 1) + " - " + r.nomeArquivo);
            IJ.log("Distance: " + r.distancia);
            IJ.log("Vector:");
            imprimirVetor(r.vetor);
        }

        IJ.showProgress(1.0);
        IJ.showStatus("");
    }

    public double calcularDistancia(double[] a, double[] b) {
        if (distanceFunction.equals("Manhattan")) {
            return TransformadaWavelets.distanciaManhattan(a, b);
        }

        if (distanceFunction.equals("Infinity")) {
            return TransformadaWavelets.distanciaInfinity(a, b);
        }

        return TransformadaWavelets.distanciaEuclidiana(a, b);
    }

    public void imprimirVetor(double[] vetor) {
        String linha = "";

        for (int i = 0; i < vetor.length; i++) {
            linha += IJ.d2s(vetor[i], 4);

            if (i < vetor.length - 1) {
                linha += ", ";
            }
        }

        IJ.log(linha);
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