package org.clueweb09.tracks;

import java.io.IOException;
import java.nio.file.Paths;

public class TREC3AdHoc extends Track {

    public TREC3AdHoc(String home) {
        super(home);
    }

    @Override
    protected void populateInfoNeeds() throws IOException {
        populateInfoNeedsTREC(Paths.get(home, "topics-and-qrels", "topics.151-200"));
    }

    @Override
    protected void populateQRelsMap() throws Exception {
        populateQRelsMap(Paths.get(home, "topics-and-qrels", "qrels.151-200-removed.txt"));
    }
}
