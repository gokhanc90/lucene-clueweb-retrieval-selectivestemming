package edu.anadolu.datasets;

import org.clueweb09.tracks.*;

public class WSJ extends DataSet {

    WSJ(String tfd_home) {
        super(Collection.WSJ, new Track[]{
                new TREC1AdHoc(tfd_home),
                new TREC2AdHoc(tfd_home),
                new TREC3AdHoc(tfd_home)
        }, tfd_home);
    }

    @Override
    public String getNoDocumentsID() {
        return "WSJ000000-0000";
    }

    @Override
    public boolean spamAvailable() {
        return false;
    }
}
