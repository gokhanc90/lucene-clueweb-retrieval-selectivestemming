package edu.anadolu.similarities;

import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.ModelBase;

/**
 * This class implements the DFRee weighting model. DFRee stands for DFR free from parameters.
 * In particular, the DFRee model computes an average number of extra bits (as information
 * divergence) that are necessary to code one extra token of the query term with respect to
 * the probability distribution observed in the document. There are two possible populations
 * to sample the probability distribution: considering only the document and no other document
 * in the collection, or the document considered as sample drawn from the entire collection
 * statistics. DFRee takes an average of these two information measures, that is their inner product.
 */
public final class IFB2 extends ModelBase {


    @Override
    public double score(double tf, long docLength, double averageDocumentLength, double keyFrequency, double documentFrequency, double termFrequency, double numberOfDocuments, double numberOfTokens) {
        double ntf = tf * log2(1+averageDocumentLength/docLength);
        return  keyFrequency*ntf*( (termFrequency+1)/(documentFrequency*(ntf+1)) ) * log2((numberOfDocuments+1)/(termFrequency+0.5));
    }

    @Override
    public String toString() {
        return "IFB2";
    }
}
