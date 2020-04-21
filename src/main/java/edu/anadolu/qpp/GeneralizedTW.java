package edu.anadolu.qpp;

import edu.anadolu.similarities.*;
import org.apache.lucene.search.similarities.ModelBase;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static edu.anadolu.analysis.Analyzers.getAnalyzedTokens;

/**
 * Collection Query Similarity
 */
public class GeneralizedTW extends Base {

    public GeneralizedTW(Path indexPath) throws IOException {
        super(indexPath, "contents");
    }

    @Override
    public double value(String word) throws IOException {
        throw new RuntimeException("This method does not supported for this class please use valueCom");
    }

    public double valueCom(ModelBase tw, long DFStem, long TFStem,long AccDoclenStem) throws IOException {
        long tf = Math.round(TFStem/(double)DFStem);
        long doclen = Math.round(AccDoclenStem/(double)DFStem);
        double avdl = (double) sumTotalTermFreq / docCount;
        if(tw instanceof BM25c)
            return ((BM25c)tw).score(tf,doclen,avdl,1,DFStem,TFStem,docCount,sumTotalTermFreq);
        else if (tw instanceof LGDc)
            return ((LGDc)tw).score(tf,doclen,avdl,1,DFStem,TFStem,docCount,sumTotalTermFreq);
        else if (tw instanceof DirichletLM)
            return ((DirichletLM)tw).score(tf,doclen,avdl,1,DFStem,TFStem,docCount,sumTotalTermFreq);
        else if (tw instanceof PL2c)
            return ((PL2c)tw).score(tf,doclen,avdl,1,DFStem,TFStem,docCount,sumTotalTermFreq);
        else if (tw instanceof DFIC)
            return ((DFIC)tw).score(tf,doclen,avdl,1,DFStem,TFStem,docCount,sumTotalTermFreq);
        else if (tw instanceof DPH)
            return ((DPH)tw).score(tf,doclen,avdl,1,DFStem,TFStem,docCount,sumTotalTermFreq);
        else if (tw instanceof DLH13)
            return ((DLH13)tw).score(tf,doclen,avdl,1,DFStem,TFStem,docCount,sumTotalTermFreq);
        else if (tw instanceof DFRee)
            return ((DFRee)tw).score(tf,doclen,avdl,1,DFStem,TFStem,docCount,sumTotalTermFreq);

        throw new RuntimeException("Undefined ModelBase in Generalized class");
    }


    public String toString() {
        return "GeneralizedTW";
    }
}
