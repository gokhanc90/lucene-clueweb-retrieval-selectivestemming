package edu.anadolu.qpp;

import edu.anadolu.datasets.DataSet;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;
import org.clueweb09.InfoNeed;

import java.io.IOException;
import java.nio.file.Path;

import static edu.anadolu.analysis.Analyzers.getAnalyzedToken;
import static org.apache.lucene.search.similarities.ModelBase.log2;

/**
 * The PMI of a pair of outcomes x and y belonging to discrete random variables X and Y quantifies the discrepancy between
 * the probability of their coincidence given their joint distribution and their individual distributions, assuming independence.
 */
public class PMI extends Base {

    private final QueryParser queryParser;

    public PMI(Path indexPath, String field) throws IOException {
        super(indexPath, field);
        queryParser = new QueryParser(field, analyzer);
        queryParser.setDefaultOperator(QueryParser.Operator.AND);
    }

    public PMI(DataSet dataset, String tag, String field) throws IOException {
        this(dataset.indexesPath().resolve(tag), field);
    }


    /**
     * the number of documents containing at both of the terms
     */
    private int term1ANDterm2(String term1, String term2) throws IOException, ParseException {
        int temp = t1ANDt2(term1, term2);
        int actual = searcher.count(queryParser.parse(term1 + " " + term2)) + 1;

        if (temp != actual) System.err.println("previous implementation returns different result from new one "+term1+" "+term2+" "+temp+" "+actual);

        return actual;
    }

    /**
     * the number of documents containing at both of the terms
     */
    private int t1ANDt2(String term1, String term2) throws IOException {

        TermQuery t1 = new TermQuery(new Term(field, getAnalyzedToken(term1, analyzer)));
        TermQuery t2 = new TermQuery(new Term(field, getAnalyzedToken(term2, analyzer)));

        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(t1, BooleanClause.Occur.MUST).add(t2, BooleanClause.Occur.MUST);

        return searcher.count(builder.build()) + 1;
    }

    @Override
    public double value(String word) {
        throw new UnsupportedOperationException();
    }


    public long analyzedDF(String field, String word) throws IOException {
        return df(field, getAnalyzedToken(word, analyzer)) + 1;
    }

    public double pmi(String m1, String m2) throws IOException, ParseException {
        return log2((docCount + 1) * (double) term1ANDterm2(m1, m2) / (double) (analyzedDF(field, m1) * analyzedDF(field, m2)));
    }


    public double value(InfoNeed need) throws IOException, ParseException {

        double pmi = 0.0;
        int counter = 0;

        String[] distinctTerms = need.distinctSet.toArray(new String[0]);

        if (distinctTerms.length == 1) {
            // TODO what is the value of average PMI for one term query?
            return 0.0;
        }

        for (int i = 0; i < distinctTerms.length; i++) {
            final String m1 = distinctTerms[i];
            for (int j = i + 1; j < distinctTerms.length; j++) {
                final String m2 = distinctTerms[j];

                int intersect = term1ANDterm2(m1, m2);

                if (intersect == 0) {
                    //TODO do something when there is no intersection since logarithm of zero is not defined.
                    // at the time of being use +1 trick
                }

                pmi += pmi(m1, m2);
                counter++;

            }
        }
        return pmi / counter;
    }
}

