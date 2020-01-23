package org.clueweb09.tracks;

import java.io.IOException;
import java.nio.file.Paths;

public class TREC2AdHoc extends Track {

    public TREC2AdHoc(String home) {
        super(home);
    }

    @Override
    protected void populateInfoNeeds() throws IOException {
        populateInfoNeedsTREC(Paths.get(home, "topics-and-qrels", "topics.101-150"));
    }

    @Override
    protected void populateQRelsMap() throws Exception {
        populateQRelsMap(Paths.get(home, "topics-and-qrels", "qrels.101-150.txt"));
    }
}
