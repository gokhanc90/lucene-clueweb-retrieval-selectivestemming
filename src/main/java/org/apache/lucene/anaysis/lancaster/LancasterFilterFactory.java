package org.apache.lucene.anaysis.lancaster;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.analysis.util.TokenFilterFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class LancasterFilterFactory extends TokenFilterFactory implements ResourceLoaderAware {
    /**
     * Initialize this factory via a set of key-value pairs.
     *
     * @param args
     */
    public LancasterFilterFactory(Map<String, String> args) {
        super(args);

    }

    @Override
    public LancasterFilter create(TokenStream input) {
        return new LancasterFilter(input);
    }

    @Override
    public void inform(ResourceLoader loader) throws IOException {

    }
}
