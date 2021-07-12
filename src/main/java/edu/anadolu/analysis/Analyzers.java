package edu.anadolu.analysis;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.en.EnglishMinimalStemFilterFactory;
import org.apache.lucene.analysis.hps.HPSFilterFactory;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.miscellaneous.TruncateTokenFilterFactory;
import org.apache.lucene.analysis.snowball.SnowballPorterFilterFactory;
import org.apache.lucene.analysis.standard.UAX29URLEmailTokenizer;
import org.apache.lucene.analysis.synonym.SynonymGraphFilter;
import org.apache.lucene.analysis.synonym.SynonymGraphFilterFactory;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionLengthAttribute;
import org.apache.lucene.analysis.tokenattributes.TermToBytesRefAttribute;
import org.apache.lucene.analysis.tr.Zemberek3StemFilterFactory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Array;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static edu.anadolu.analysis.Tag.KStem;

/**
 * Utility to hold {@link Analyzer} implementation used in this work.
 */
public class Analyzers {

    public static final String FIELD = "field";

    public static final String[] scripts = new String[]{
            "Jpan",
            "Cyrillic",
            "Greek",
            "Arabic",
            "Hangul",
            "Thai",
            "Armenian",
            "Devanagari",
            "Hebrew",
            "Georgian"
    };

    /**
     * Intended to use with one term queries (otq) only
     *
     * @param text input string to analyze
     * @return analyzed input
     */
    public static String getAnalyzedToken(String text, Analyzer analyzer) {
        final List<String> list = getAnalyzedTokens(text, analyzer);
        if (list.size() != 1)
            System.err.println("Text : " + text + " contains more than one tokens : " + list.toString());
        return list.get(0);
    }

    /**
     * Modified from : http://lucene.apache.org/core/4_10_2/core/org/apache/lucene/analysis/package-summary.html
     */
    public static List<String> getAnalyzedTokens(String text, Analyzer analyzer) {

        final List<String> list = new ArrayList<>();
        try (TokenStream ts = analyzer.tokenStream(FIELD, new StringReader(text))) {

            final CharTermAttribute termAtt = ts.addAttribute(CharTermAttribute.class);
            ts.reset(); // Resets this stream to the beginning. (Required)
            while (ts.incrementToken())
                list.add(termAtt.toString());

            ts.end();   // Perform end-of-stream operations, e.g. set the final offset.
        } catch (IOException ioe) {
            throw new RuntimeException("happened during string analysis", ioe);
        }
        return list;
    }

    public static Map<Integer,List<String>> getAnalyzedTokensWithSynonym(String text, Analyzer analyzer) {

        final Map<Integer,List<String>> list = new LinkedHashMap<>();
        try (TokenStream ts = analyzer.tokenStream(FIELD, new StringReader(text))) {

            final CharTermAttribute termAtt = ts.addAttribute(CharTermAttribute.class);
            final OffsetAttribute offsetAttribute = ts.addAttribute(OffsetAttribute.class);

            ts.reset(); // Resets this stream to the beginning. (Required)
            while (ts.incrementToken()) {
                int startOffset = offsetAttribute.startOffset();
                if(list.containsKey(startOffset)){
                    List<String> l = list.get(startOffset);
                    l.add(termAtt.toString());
                    list.put(startOffset,l);
                }
                else{
                    ArrayList<String> l = new ArrayList<>();
                    l.add(termAtt.toString());
                    list.put(startOffset, l);
                }
            }
            ts.end();   // Perform end-of-stream operations, e.g. set the final offset.
        } catch (IOException ioe) {
            throw new RuntimeException("happened during string analysis", ioe);
        }
        return list;
    }


    public static Analyzer analyzer(Tag tag) {
        try {
            return anlyzr(tag);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public static Analyzer analyzer(Tag tag, Path synonymPath) {
        try {
            return anlyzr(tag,synonymPath);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public static Analyzer analyzer(Path synonymPath) {
        try {
            return anlyzr(synonymPath);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    private static Analyzer anlyzr(Tag tag,Path synonymPath) throws IOException {

        return CustomAnalyzer.builder(synonymPath)
                .withTokenizer("standard")
                .addTokenFilter("lowercase")
                .addTokenFilter(SynonymGraphFilterFactory.class, "format", "solr", "expand", "true", "synonyms", tag.toString() + ".txt")
                .build();
    }

    private static Analyzer anlyzr(Path synonymPath) throws IOException {

        return CustomAnalyzer.builder(synonymPath.getParent())
                .withTokenizer("standard")
                .addTokenFilter("lowercase")
                .addTokenFilter(SynonymGraphFilterFactory.class, "format", "solr", "expand", "true", "synonyms", synonymPath.getFileName().toString())
                .build();
    }

    private static Analyzer anlyzr(Tag tag) throws IOException {

        switch (tag) {

            case NoStem:
                return CustomAnalyzer.builder()
                        .withTokenizer("standard")
                        .addTokenFilter("lowercase")
                        .build();

            case KStem:
                return CustomAnalyzer.builder()
                        .withTokenizer("standard")
                        .addTokenFilter("lowercase")
                        .addTokenFilter("kstem")
                        .build();

            case ICU:
                return CustomAnalyzer.builder()
                        .withTokenizer("icu")
                        .addTokenFilter("lowercase")
                        .addTokenFilter("kstem")
                        .build();

            case Latin:
                return CustomAnalyzer.builder()
                        .withTokenizer("icu")
                        .addTokenFilter(ScriptAsTypeTokenFilterFactory.class)
                        .addTokenFilter(FilterTypeTokenFilterFactory.class, "useWhitelist", "true", "types", "Latin")
                        .addTokenFilter("lowercase")
                        .addTokenFilter("kstem")
                        .build();

            case ASCII:
                return CustomAnalyzer.builder()
                        .withTokenizer("icu")
                        .addTokenFilter(BasicLatinTokenFilterFactory.class)
                        .addTokenFilter(FilterTypeTokenFilterFactory.class, "useWhitelist", "true", "types", "ASCII")
                        .addTokenFilter("lowercase")
                        .addTokenFilter("kstem")
                        .build();

            case Zemberek:
                return CustomAnalyzer.builder()
                        .withTokenizer("standard")
                        .addTokenFilter("apostrophe")
                        .addTokenFilter("turkishlowercase")
                        .addTokenFilter(Zemberek3StemFilterFactory.class, "strategy", "maxLength")
                        .build();

            case NoStemTurkish:
                return CustomAnalyzer.builder()
                        .withTokenizer("standard")
                        .addTokenFilter("apostrophe")
                        .addTokenFilter("turkishlowercase")
                        .build();

            case KStemField: {

                Map<String, Analyzer> analyzerPerField = new HashMap<>();
                analyzerPerField.put("url", new SimpleAnalyzer());

                return new PerFieldAnalyzerWrapper(
                        Analyzers.analyzer(KStem), analyzerPerField);
            }

            case UAX: {

                Map<String, Analyzer> analyzerPerField = new HashMap<>();

                analyzerPerField.put("url", CustomAnalyzer.builder()
                        .withTokenizer("uax29urlemail")
                        .addTokenFilter("lowercase")
                        .addTokenFilter(FilterTypeTokenFilterFactory.class, "useWhitelist", "true", "types", UAX29URLEmailTokenizer.TOKEN_TYPES[UAX29URLEmailTokenizer.URL])
                        .build());

                analyzerPerField.put("email", CustomAnalyzer.builder()
                        .withTokenizer("uax29urlemail")
                        .addTokenFilter("lowercase")
                        .addTokenFilter(FilterTypeTokenFilterFactory.class, "useWhitelist", "true", "types", UAX29URLEmailTokenizer.TOKEN_TYPES[UAX29URLEmailTokenizer.EMAIL])
                        .build());

                return new PerFieldAnalyzerWrapper(CustomAnalyzer.builder()
                        .withTokenizer("uax29urlemail")
                        .addTokenFilter("lowercase")
                        .build(), analyzerPerField);
            }


            case Script: {

                Map<String, Analyzer> analyzerPerField = new HashMap<>();

                for (String script : scripts)
                    analyzerPerField.put(script, script(script));

                analyzerPerField.put("ascii", CustomAnalyzer.builder()
                        .withTokenizer("icu")
                        .addTokenFilter(NonASCIITokenFilterFactory.class)
                        .build());

                analyzerPerField.put("latin", CustomAnalyzer.builder()
                        .withTokenizer("icu")
                        .addTokenFilter(NonLatinTokenFilterFactory.class)
                        .build());

                return new PerFieldAnalyzerWrapper(
                        CustomAnalyzer.builder()
                                .withTokenizer("icu")
                                .addTokenFilter(ScriptAsTypeTokenFilterFactory.class)
                                .addTokenFilter(BasicLatinTokenFilterFactory.class)
                                .addTokenFilter(TypeAsTermTokenFilterFactory.class)
                                .build(),
                        analyzerPerField);
            }

            case SnowballTr:
                return CustomAnalyzer.builder()
                        .withTokenizer("standard")
                        .addTokenFilter("apostrophe")
                        .addTokenFilter("turkishlowercase")
                        .addTokenFilter(SnowballPorterFilterFactory.class, "language", "Turkish")
                        .build();
            /**
             * Porter2
             */
            case SnowballEng:
                return CustomAnalyzer.builder()
                        .withTokenizer("standard")
                        .addTokenFilter("lowercase")
                        .addTokenFilter(SnowballPorterFilterFactory.class, "language", "English")
                        .build();
            /**
            * Brychcín, T., & Konopík, M. (2015). HPS: High precision stemmer. Information Processing & Management, 51(1), 68-91.
            * */
            case HPS:
                return CustomAnalyzer.builder()
                        .withTokenizer("standard")
                        .addTokenFilter("lowercase")
                        .addTokenFilter(HPSFilterFactory.class, "language", "en")
                        .build();
            /**
             * "S-Stemmer" from How Effective Is Suffixing? Donna Harman.
             */
            case Sstem:
                return CustomAnalyzer.builder()
                        .withTokenizer("standard")
                        .addTokenFilter("lowercase")
                        .addTokenFilter(EnglishMinimalStemFilterFactory.class)
                        .build();

            case F5Stem:
                return CustomAnalyzer.builder()
                        .withTokenizer("standard")
                        .addTokenFilter("apostrophe")
                        .addTokenFilter("turkishlowercase")
                        .addTokenFilter(TruncateTokenFilterFactory.class,"prefixLength","5")
                        .build();

            case BoilerpipeArt:
                return Analyzers.anlyzr(Tag.NoStem);

            case BoilerpipeLC:
                return Analyzers.anlyzr(Tag.NoStem);

            case BoilerpipeDefault:
                return Analyzers.anlyzr(Tag.NoStem);

            case CustomBoilerPipe:
                return Analyzers.anlyzr(Tag.NoStem);

            case CustomBoilerPipeAndJsoup:
                return Analyzers.anlyzr(Tag.NoStem);

            case CustomRemovalBoilerPipeAndJsoup:
                return Analyzers.anlyzr(Tag.NoStem);
            default:
                throw new AssertionError(Analyzers.class);

        }
    }


    private static Analyzer script(String script) throws IOException {
        return CustomAnalyzer.builder()
                .withTokenizer("icu")
                .addTokenFilter(ScriptAsTypeTokenFilterFactory.class)
                .addTokenFilter(FilterTypeTokenFilterFactory.class, "useWhitelist", "true", "types", script)
                .addTokenFilter("lowercase")
                .build();
    }
}
