package edu.anadolu.qpp;

import org.clueweb09.InfoNeed;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static edu.anadolu.analysis.Analyzers.getAnalyzedTokens;

/**
 * Collection Query Similarity
 */
public class SCCQ extends Base {

    public SCCQ(Path indexPath) throws IOException {
        super(indexPath, "contents");
    }

    /**
     * Collection Query Similarity (SCQ) of a term
     */
    @Override
    public double value(String word) throws IOException {

        List<String> confs = getAnalyzedTokens(word, analyzer);

        long sumConfs = 0;
        for(String c:confs)
            sumConfs+=ctf(field,c);

        Double itf = Math.log((double) sumTotalTermFreq / sumConfs);

        return (1 + Math.log((ctf(field, word)))) * itf;
    }

    public double value(InfoNeed need) throws IOException {
        double sccq=0.0;

        for(String term: need.getPartialQuery()) {
            List<String> confs = getAnalyzedTokens(term, analyzer);

            long sumConfs = 0;
            for(String c:confs)
                sumConfs+=ctf(field,c);

            Double itf = Math.log((double) sumTotalTermFreq / sumConfs);

            sccq += (1 + Math.log((ctf(field, term)))) * itf;

        }
        return sccq;
    }

    public String toString() {
        return "SCQ";
    }
}
