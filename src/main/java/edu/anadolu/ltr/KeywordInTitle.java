package edu.anadolu.ltr;

import edu.anadolu.analysis.Analyzers;
import edu.anadolu.analysis.Tag;
import edu.anadolu.field.MetaTag;


public class KeywordInTitle implements IDocFeature {

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    @Override
    public double calculate(DocFeatureBase base) {
        try {
            String title = base.jDoc.title();
            String keyword = MetaTag.enrich3(base.jDoc, "keywords");
            for (String token : Analyzers.getAnalyzedTokens(keyword, Analyzers.analyzer(Tag.NoStem))) {
                if (title.contains(token))
                    return 1;
            }
            return 0;
        } catch (NullPointerException ex) {
            return 0;
        }
    }
}
