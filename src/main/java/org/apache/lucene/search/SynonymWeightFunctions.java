package org.apache.lucene.search;

import edu.anadolu.analysis.Tag;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.qpp.IDF;
import edu.anadolu.qpp.PMI;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;

import java.io.IOException;

public class SynonymWeightFunctions {
    public  DataSet dataset;
    public  Tag tag;

    private PMI pmi;
    private IDF idf;

    public SynonymWeightFunctions(DataSet dataset, Tag tag) {
        this.dataset = dataset;
        this.tag = tag;
        try {
            this.pmi= new PMI(dataset.indexesPath().resolve(tag.toString()), "contents");
            this.idf = new IDF(dataset.indexesPath().resolve(tag.toString()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public double QBSART(Term orj, Term v, Term[] otherOrj){
        try {
            if (orj.equals(v)) return 1;
            return  0.7*associationStrength(calculateNPMI(orj, v))  + 0.3* replacementStrength(orj,v,otherOrj);
        }catch (IOException  | ParseException e) {
            throw new RuntimeException();
        }
    }
    private double calculateNPMI(Term t1, Term t2) throws IOException, ParseException {
        if(t1.text().equals(t2.text())) return 1.0;
        return pmi.npmi(t1.text(),t2.text());
    }
    private double associationStrength(double npmi)  {
        if (npmi >= 0.25) return npmi;
        else return 0;
    }

    private double replacementStrength(Term t1, Term t2,Term[] otherOrj) throws IOException, ParseException {
        return Math.min(1.0,RA(t1,t2,otherOrj));
    }

    private double RA(Term t1, Term t2,Term[] otherOrj) throws IOException, ParseException {
        double sumOver = 0.0, sumBelow=0.0;
        for(int i=0;i<otherOrj.length;i++){
            sumOver+=idf.value(otherOrj[i].text()) * associationStrength(calculateNPMI(t2, otherOrj[i]));
        }
        for(int i=0;i<otherOrj.length;i++){
            sumBelow+=idf.value(otherOrj[i].text()) * associationStrength(calculateNPMI(t1, otherOrj[i]));
        }
        if(sumOver==0.0) return 0;
        return sumOver/sumBelow;
    }

}
