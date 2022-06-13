package org.apache.lucene.anaysis.lancaster;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.KeywordAttribute;
import smile.nlp.stemmer.LancasterStemmer;


import java.io.IOException;

/**
 * The Paice/Husk Lancaster stemming algorithm. The stemmer is a conflation
 * based iterative stemmer. The stemmer, although remaining efficient and
 * easily implemented, is known to be very strong and aggressive. The stemmer
 * utilizes a single table of rules, each of which may specify
 * the removal or replacement of an ending. For details, see
 *
 * <h2>References</h2>
 * <ol>
 * <li> Paice, Another stemmer, SIGIR Forum, 24(3), 56-61, 1990. </li>
 * <li> http://www.comp.lancs.ac.uk/computing/research/stemming/Links/paice.htm </li>
 * </ol>
 **/

public final class LancasterFilter extends TokenFilter {
    private final LancasterStemmer stemmer;

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final KeywordAttribute keywordAttr = addAttribute(KeywordAttribute.class);
    /**
     * Construct a token stream filtering the given input.
     *
     * @param input
     */

    public LancasterFilter(TokenStream input) {
        super(input);
        stemmer=new LancasterStemmer();
    }

    @Override
    public boolean incrementToken() throws IOException {
        if (input.incrementToken()) {
            if (!keywordAttr.isKeyword()) {
                char termBuffer[] = termAtt.buffer();
                final int length = termAtt.length();
                String stem = stemmer.stem(String.valueOf(termBuffer,0,length));
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
