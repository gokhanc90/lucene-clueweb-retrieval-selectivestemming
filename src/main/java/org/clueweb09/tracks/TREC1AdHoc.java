package org.clueweb09.tracks;

import java.io.IOException;
import java.nio.file.Paths;

public class TREC1AdHoc extends Track {

    public TREC1AdHoc(String home) {
        super(home);
    }

    @Override
    protected void populateInfoNeeds() throws IOException {
        populateInfoNeedsTREC(Paths.get(home, "topics-and-qrels", "topics.51-100"));
    }

    @Override
    protected void populateQRelsMap() throws Exception {
        populateQRelsMap(Paths.get(home, "topics-and-qrels", "qrels.51-100-removed.txt"));
    }
}
