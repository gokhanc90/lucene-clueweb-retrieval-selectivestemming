package org.apache.lucene.analysis.hps;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.KeywordAttribute;
import ws.StemmerBuilder;
import ws.stemmer.Stemmer;

import java.io.IOException;

public final class HPSFilter extends TokenFilter {
    private final Stemmer stemmer;

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final KeywordAttribute keywordAttr = addAttribute(KeywordAttribute.class);
    /**
     * Construct a token stream filtering the given input.
     *
     * @param input
     * @param model
     */

    public HPSFilter(TokenStream input, String model) {
        super(input);
        try {
            stemmer=StemmerBuilder.loadStemmer(model,3);
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Error instantiating stemmer", e);
        }
    }

    @Override
    public boolean incrementToken() throws IOException {
        if (input.incrementToken()) {
            if (!keywordAttr.isKeyword()) {
                char termBuffer[] = termAtt.buffer();
                final int length = termAtt.length();
                String stem = stemmer.getClass(String.valueOf(termBuffer,0,length));
                final char finalTerm[] = stem.toCharArray();
                final int newLength = finalTerm.length;
                if (finalTerm != termBuffer)
                    termAtt.copyBuffer(finalTerm, 0, newLength);
                else
                    termAtt.setLength(newLength);
            }
            return true;
        } else {
            return false;
        }
    }
}
