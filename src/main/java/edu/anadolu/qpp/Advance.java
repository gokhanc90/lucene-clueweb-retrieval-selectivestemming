package edu.anadolu.qpp;

import org.clueweb09.InfoNeed;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static edu.anadolu.analysis.Analyzers.getAnalyzedTokens;

/**
 * Collection Query Similarity
 */
public class Advance extends Base {

    public Advance(Path indexPath) throws IOException {
        super(indexPath, "contents");
    }

    @Override
    public double value(String word) throws IOException {
        throw new RuntimeException("This method does not supported for this class please use valueCom");
    }

    public double valueCom(String word,long DFStem,long TFStem) throws IOException {

        long TFNoStem=0, DFNoStem=0;
        TFNoStem=ctf(field,word);
        DFNoStem=df(field,word);
        double tfAdv = (TFStem-TFNoStem)/(double)TFNoStem;
        double dfAdv = (DFStem-DFNoStem)/(double)DFNoStem;

        if(dfAdv==0.0) return  1.0;
        return tfAdv/dfAdv;
    }


    public String toString() {
        return "Advance";
    }

    public double valueComTF(String word, long TFStem)  throws IOException  {
        long TFNoStem=0;
        TFNoStem=ctf(field,word);
        return  (TFStem-TFNoStem)/(double)TFNoStem;
    }

    public double valueComDF(String word, long DFStem) throws IOException {
        long DFNoStem=0;
        DFNoStem=df(field,word);
        return (DFStem-DFNoStem)/(double)DFNoStem;

    }
}
