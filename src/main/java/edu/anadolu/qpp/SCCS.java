package edu.anadolu.qpp;

import org.clueweb09.InfoNeed;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static edu.anadolu.analysis.Analyzers.getAnalyzedTokens;

/**
 * Simplified Clarity Conflation Score (SCS)
 */
public class SCCS extends Base {

    public SCCS(Path indexPath, String field) throws IOException {
        super(indexPath, field);
    }

    @Override
    public double value(String word) {
        throw new UnsupportedOperationException();
    }

    /**
     * Kullback-Leibler between a (simplified) query language model and a collection model
     * qtf is the number of occurrences of a term in the query
     * ctf is the number of occurrences of a term in the collection
     */
    public double value(InfoNeed need) throws IOException {

        double sccs = 0.0;
        double qtf, ctf;

        for(String term: need.getPartialQuery()) {
            List<String> confs = getAnalyzedTokens(term, analyzer);

            long sumConfs = 0;
            for(String c:confs)
                sumConfs+=ctf(field,c);

            qtf = (double) Collections.frequency(Arrays.asList(need.getPartialQuery()), term) / need.getPartialQuery().length;
            ctf = (double) ctf(field, term) / sumConfs;
            sccs += qtf * Math.log(ctf == 0.0 ? Double.MAX_VALUE : qtf / ctf);

        }
        return sccs;
    }

    public String toString() {
        return "SCCS";
    }
}