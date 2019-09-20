package edu.anadolu;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static edu.anadolu.analysis.Analyzers.getAnalyzedTokens;

public class Utils {
    private static final double LOG_2 = Math.log(2);

    public static double log2(double x) {
        // Put this to a 'util' class if we need more of these.
        return Math.log(x) / LOG_2;
    }

    // qtf = (double) Collections.frequency(terms, term) / terms.size();
    // ctf = (double) ctf(field, term) / sumTotalTermFreq;
    public static double scs(double[] qtfs, double[] ctfs){
        double scs = 0.0;

        for (int i = 0; i < qtfs.length; i++) {
            // the specificity of a query term is assumed to be large if it does not exist in the collection
            scs += qtfs[0] * Math.log(ctfs[0] == 0.0 ? Double.MAX_VALUE : qtfs[0] / ctfs[0]);
        }

        return scs;
    }

    public static double ictf(long[] TFs,long totalTF){
        double cross = 1.0;
        for(int i=0;i<TFs.length;i++)
            cross*=totalTF/(double)TFs[i];
        return Math.log(cross);
    }

    public static double gamma2(double[] idfs){
        Arrays.sort(idfs);
        return idfs[idfs.length-1] / idfs[0];
    }

    public static double scq(long numberOfDocs, long docFreq, long TF){
        return (1 + Math.log(TF))*idf(numberOfDocs,docFreq);
    }

    public static double idf(long numberOfDocs, long docFreq){
        return Math.log(((double) numberOfDocs / (double) docFreq +1) + 1);
    }

    public static double idf2(long numberOfDocs, long docFreq){
        return log2(((double) numberOfDocs / (double) docFreq +1) + 1);
    }

    public static double angularSim(double[] v1, double[] v2) {
        double cosineSimilarity =cosineSim(v1,v2);
        double angular_distance = 2*Math.acos(cosineSimilarity)/Math.PI;
        double angular_sim=1-angular_distance;
        return angular_sim;
    }


    public static double cosineSim(double[] v1, double[] v2) {
        if(v1.length!=v2.length)throw new AssertionError(SelectionMethods.class);

        long dotProduct = 0;
        for(int i = 0;i<v1.length;i++) dotProduct+= v1[i] * v2[i];

        double d1 = 0.0d;
        for (final double value : v1) {
            d1 += Math.pow(value, 2);
        }

        double d2 = 0.0d;
        for (final double value : v2) {
            d2 += Math.pow(value, 2);
        }

        double cosineSimilarity;
        if (d1 <= 0.0 || d2 <= 0.0) {
            cosineSimilarity = 0.0;
        } else {
            cosineSimilarity = dotProduct / (Math.sqrt(d1) * Math.sqrt(d2));
        }
        return cosineSimilarity;

    }

}
