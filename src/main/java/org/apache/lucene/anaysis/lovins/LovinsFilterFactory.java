package org.apache.lucene.anaysis.lovins;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.tartarus.snowball.ext.LovinsStemmer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class LovinsFilterFactory extends TokenFilterFactory implements ResourceLoaderAware {
    /**
     * Initialize this factory via a set of key-value pairs.
     *
     * @param args
     */

    public LovinsFilterFactory(Map<String, String> args) {
        super(args);

    }

    @Override
    public SnowballFilter create(TokenStream input) {
        return new SnowballFilter(input,"Lovins");
    }

    @Override
    public void inform(ResourceLoader loader) throws IOException {

    }
}
