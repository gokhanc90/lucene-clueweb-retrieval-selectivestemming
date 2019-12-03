package org.apache.lucene.analysis.hps;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import ws.stemmer.Stemmer;

import java.io.IOException;
import java.util.Map;

public class HPSFilterFactory extends TokenFilterFactory implements ResourceLoaderAware {
    /**
     * Initialize this factory via a set of key-value pairs.
     *
     * @param args
     */
    private final String language;
    private  String model;

    public HPSFilterFactory(Map<String, String> args) {
        super(args);
        language = get(args, "language", "en");
        if (!args.isEmpty()) {
            throw new IllegalArgumentException("Unknown parameters: " + args);
        }

    }

    @Override
    public HPSFilter create(TokenStream input) {
        return new HPSFilter(input,model);
    }

    @Override
    public void inform(ResourceLoader loader) throws IOException {
        model=loader.getClass().getClassLoader().getResource("HPS_"+language+".bin").getPath();
    }
}
