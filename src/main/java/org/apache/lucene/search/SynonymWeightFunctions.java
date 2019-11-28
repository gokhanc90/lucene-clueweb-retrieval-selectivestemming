package org.apache.lucene.search;

import edu.anadolu.analysis.Tag;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.qpp.PMI;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;

import java.io.IOException;

public class SynonymWeightFunctions {
    public static DataSet dataset;
    public static Tag tag;

    public double[] QBSART(Term orj, Term[] terms,Term[] otherOrj){
        try {
            PMI pmi = new PMI(dataset.indexesPath().resolve(tag.toString()), "contents");
            double[] f = new double[terms.length];
            for (int i = 0; i < f.length; i++)
                f[i] = 0.7*associationStrength(calculateNPMI(orj, terms[i],pmi))  + 0.3* ;
            return f;
        }catch (IOException  | ParseException e) {
            throw new RuntimeException();
        }
    }
    private double calculateNPMI(Term t1, Term t2,PMI pmi) throws IOException, ParseException {
        if(t1.text().equals(t2.text())) return 1.0;
        return pmi.npmi(t1.text(),t2.text());
    }
    private double associationStrength(double npmi)  {
        if (npmi >= 0.25) return npmi;
        else return 0;
    }

    private double replacementStrength(Term t1, Term t2,Term[] otherOrj)  {
        return Math.min(1.0,RA(t1,t2,otherOrj));
    }

    private double RA(Term t1, Term t2,Term[] otherOrj)  {

    }

}
