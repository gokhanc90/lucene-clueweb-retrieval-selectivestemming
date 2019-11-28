package edu.anadolu.qpp;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.*;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static edu.anadolu.analysis.Analyzers.getAnalyzedTokens;

public class Commonality extends Base {
    private long docLenAcc;
    public Commonality(Path indexPath) throws IOException {
        super(indexPath, "contents");
    }

    public long getdocLenAcc() {
        return docLenAcc;
    }


    @Override
    public double value(String word) throws IOException {
        int counter=0;
        try (TokenStream ts = analyzer.tokenStream("contents", new StringReader(word))) {

            final CharTermAttribute termAtt = ts.addAttribute(CharTermAttribute.class);
            ts.reset(); // Resets this stream to the beginning. (Required)
            while (ts.incrementToken()) {
                //System.out.println(termAtt.toString());
                counter++;
            }

            ts.end();   // Perform end-of-stream operations, e.g. set the final offset.
        } catch (IOException ioe) {
            throw new RuntimeException("happened during string analysis", ioe);
        }
        return counter;
    }

    public String toString() {
        return "Commonality";
    }

    public long df(String word) throws IOException {
        docLenAcc=0;
        List<String> confs = getAnalyzedTokens(word, analyzer);

        Set<Integer> df = new HashSet<>();
        for(String t:confs) {

            Term term = new Term(field, t);
            PostingsEnum postingsEnum = MultiFields.getTermDocsEnum(reader, field, term.bytes());

            if (postingsEnum == null) continue;

            while (postingsEnum.nextDoc() != PostingsEnum.NO_MORE_DOCS) {
                df.add(postingsEnum.docID());
            }
        }

        //doclen is set
        for (Integer docID : df) {
            long doclen;
            NumericDocValues norms = MultiDocValues.getNormValues(reader, field);

            if (norms.advanceExact(docID)) {
                doclen = norms.longValue();
            } else {
                doclen = 0;
            }

            docLenAcc += doclen;
        }
        return df.size();
    }

    public long TF(String word) throws IOException {
        List<String> confs = getAnalyzedTokens(word, analyzer);

        long tf = 0;
        for(String c:confs)
            tf +=ctf(field,c);
        return tf;
    }
}
