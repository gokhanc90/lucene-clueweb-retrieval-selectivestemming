package org.apache.lucene.search;

import edu.anadolu.analysis.Tag;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.qpp.IDF;
import edu.anadolu.qpp.PMI;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;

public class SynonymWeightFunctions {
    public  DataSet dataset;
    public  Tag tag;

    public PMI pmi;
    public IDF idf;
    public HashMap<Pair<String,String>,Double> bertSim = new HashMap<>();

    public SynonymWeightFunctions(DataSet dataset, Tag indexTag, Tag runningTag) {
        this.dataset = dataset;
        this.tag = indexTag;
        try {
            this.pmi= new PMI(dataset.indexesPath().resolve(tag.toString()), "contents");
            this.idf = new IDF(dataset.indexesPath().resolve(tag.toString()));
            if(runningTag.toString().contains("BERT")) {
                List<String> lines = Files.readAllLines(Paths.get(dataset.collectionPath().toString(), runningTag.toString() + ".txt"));
                lines.stream().filter(t -> !t.startsWith("#")).
                        forEach(l -> bertSim.put(new ImmutablePair<>(
                                        l.split(",")[0],
                                        l.split(",")[1]),
                                Double.valueOf(l.split(",")[2]))
                        );
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public double BERTSim(Term orj, Term v){
        try {
            if (orj.equals(v)) return 2;
            if (bertSim.containsKey(Pair.of(orj.text(), v.text()))) {

                double bertSimVal = bertSim.get(Pair.of(orj.text(), v.text()));
                double npmi = calculateNPMI(orj, v);
                System.out.println(bertSimVal + " " + npmi);
                return bertSimVal + npmi;
            }
            return calculateNPMI(orj, v)+0.8;
        } catch (IOException | ParseException e) {
            throw new RuntimeException();
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
