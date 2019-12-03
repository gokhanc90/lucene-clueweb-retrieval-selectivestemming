/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.lucene.search;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import edu.anadolu.analysis.Tag;
import edu.anadolu.datasets.DataSet;
import org.apache.lucene.index.*;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.search.similarities.Similarity.SimScorer;
import org.apache.lucene.util.BytesRef;

/**
 * A query that treats multiple terms as synonyms.
 * <p>
 * For scoring purposes, this query tries to score the terms as if you
 * had indexed them as one term: it will match any of the terms but
 * only invoke the similarity a single time, scoring the sum of all
 * term frequencies for the document.
 */
public final class SynonymWeightedQuery extends Query {
    private final Term[] terms;
    private final Term orginal;
    private final Term[] otherOrj;
    private SynonymWeightFunctions functions;

    /**
     * Creates a new SynonymQuery, matching any of the supplied terms.
     * <p>
     * The terms must all have the same field.
     */
    public SynonymWeightedQuery(SynonymWeightFunctions functions,DataSet dataset, Term[] otherOrj, Tag tag, Term original, Term... terms) {
        this.terms = Objects.requireNonNull(terms).clone();
        this.orginal = original;
        this.otherOrj=otherOrj;
        this.functions=functions;
        // check that all terms are the same field
        String field = null;
        for (Term term : terms) {
            if (field == null) {
                field = term.field();
            } else if (!term.field().equals(field)) {
                throw new IllegalArgumentException("Synonyms must be across the same field");
            }
        }
        if (!original.field().equals(field)) {
            throw new IllegalArgumentException("Original term and Synonyms must be across the same field");
        }

        if (terms.length > BooleanQuery.getMaxClauseCount()) {
            throw new BooleanQuery.TooManyClauses();
        }
        Arrays.sort(this.terms);
    }

    public List<Term> getTerms() {
        return Collections.unmodifiableList(Arrays.asList(terms));
    }

    @Override
    public String toString(String field) {
        StringBuilder builder = new StringBuilder("SynonymWeighted(");
        for (int i = 0; i < terms.length; i++) {
            if (i != 0) {
                builder.append(" ");
            }
            Query termQuery = new TermQuery(terms[i]);
            builder.append(termQuery.toString(field));
        }
        builder.append(")");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        return 31 * classHash() + Arrays.hashCode(terms);
    }

    @Override
    public boolean equals(Object other) {
        return sameClassAs(other) &&
                Arrays.equals(terms, ((SynonymWeightedQuery) other).terms);
    }

    @Override
    public Query rewrite(IndexReader reader) throws IOException {
        // optimize zero and single term cases
        if (terms.length == 0) {
            return new BooleanQuery.Builder().build();
        }
        if (terms.length == 1) {
            return new TermQuery(terms[0]);
        }
        return this;
    }

    @Override
    public Weight createWeight(IndexSearcher searcher, boolean needsScores, float boost) throws IOException {
        if (needsScores) {
            return new SynonymWeight(this, searcher, boost);
        } else {
            // if scores are not needed, let BooleanWeight deal with optimizing that case.
            BooleanQuery.Builder bq = new BooleanQuery.Builder();
            for (Term term : terms) {
                bq.add(new TermQuery(term), BooleanClause.Occur.SHOULD);
            }
            return searcher.rewrite(bq.build()).createWeight(searcher, needsScores, boost);
        }
    }

    class SynonymWeight extends Weight {
        private final TermContext termContexts[];
        private final Similarity similarity;
        private final Similarity.SimWeight simWeight;
        final double factors[];

        SynonymWeight(Query query, IndexSearcher searcher, float boost) throws IOException {
            super(query);
            CollectionStatistics collectionStats = searcher.collectionStatistics(terms[0].field());
            long docFreq = 0;
            long totalTermFreq = 0;
            termContexts = new TermContext[terms.length];
            factors = new double[terms.length];

            for (int i = 0; i < termContexts.length; i++) {
                factors[i]=functions.QBSART(orginal,terms[i],otherOrj);
                termContexts[i] = build(searcher.getTopReaderContext(), terms[i],factors[i]);//PMI
                TermStatistics termStats = searcher.termStatistics(terms[i], termContexts[i]);
                docFreq = Math.max(termStats.docFreq(), docFreq);
                if (termStats.totalTermFreq() == -1) {
                    totalTermFreq = -1;
                } else if (totalTermFreq != -1) {
                    totalTermFreq += termStats.totalTermFreq();
                }
            }
            TermStatistics pseudoStats = new TermStatistics(null, docFreq, totalTermFreq);
            this.similarity = searcher.getSimilarity(true);
            this.simWeight = similarity.computeWeight(boost, collectionStats, pseudoStats);
        }

        @Override
        public void extractTerms(Set<Term> terms) {
            for (Term term : SynonymWeightedQuery.this.terms) {
                terms.add(term);
            }
        }

        @Override
        public Matches matches(LeafReaderContext context, int doc) throws IOException {
            String field = terms[0].field();
            Terms terms = context.reader().terms(field);
            if (terms == null || terms.hasPositions() == false) {
                return super.matches(context, doc);
            }
            return MatchesUtils.forField(field, () -> DisjunctionMatchesIterator.fromTerms(context, doc, getQuery(), field, Arrays.asList(SynonymWeightedQuery.this.terms)));
        }

        @Override
        public Explanation explain(LeafReaderContext context, int doc) throws IOException {
            Scorer scorer = scorer(context);
            if (scorer != null) {
                int newDoc = scorer.iterator().advance(doc);
                if (newDoc == doc) {
                    final float freq;
                    if (scorer instanceof SynonymScorer) {
                        SynonymScorer synScorer = (SynonymScorer) scorer;
                        freq = synScorer.tf(synScorer.getSubMatches());
                    } else {
                        assert scorer instanceof TermScorer;
                        freq = ((TermScorer)scorer).freq();
                    }
                    SimScorer docScorer = similarity.simScorer(simWeight, context);
                    Explanation freqExplanation = Explanation.match(freq, "termFreq=" + freq);
                    Explanation scoreExplanation = docScorer.explain(doc, freqExplanation);
                    return Explanation.match(
                            scoreExplanation.getValue(),
                            "weight(" + getQuery() + " in " + doc + ") ["
                                    + similarity.getClass().getSimpleName() + "], result of:",
                            scoreExplanation);
                }
            }
            return Explanation.noMatch("no matching term");
        }

        @Override
        public Scorer scorer(LeafReaderContext context) throws IOException {
            Similarity.SimScorer simScorer = similarity.simScorer(simWeight, context);
            // we use termscorers + disjunction as an impl detail
            List<Scorer> subScorers = new ArrayList<>();
            for (int i = 0; i < terms.length; i++) {
                TermState state = termContexts[i].get(context.ord);
                if (state != null) {
                    TermsEnum termsEnum = context.reader().terms(terms[i].field()).iterator();
                    termsEnum.seekExact(terms[i].bytes(), state);
                    PostingsEnum postings = termsEnum.postings(null, PostingsEnum.FREQS);
                    subScorers.add(new TermScorerWrapper(this, postings, simScorer,factors[i],terms[i]));
                }
            }
            if (subScorers.isEmpty()) {
                return null;
            } else if (subScorers.size() == 1) {
                // we must optimize this case (term not in segment), disjunctionscorer requires >= 2 subs
                return subScorers.get(0);
            } else {
                return new SynonymScorer(simScorer, this, subScorers);
            }
        }

        @Override
        public boolean isCacheable(LeafReaderContext ctx) {
            return true;
        }

        public TermContext build(IndexReaderContext context, Term term, double TFWeight)
                throws IOException {
            assert context != null && context.isTopLevel;
            final String field = term.field();
            final BytesRef bytes = term.bytes();
            final TermContext perReaderTermState = new TermContext(context);
            //if (DEBUG) System.out.println("prts.build term=" + term);
            for (final LeafReaderContext ctx : context.leaves()) {
                //if (DEBUG) System.out.println("  r=" + leaves[i].reader);
                final Terms terms = ctx.reader().terms(field);
                if (terms != null) {
                    final TermsEnum termsEnum = terms.iterator();
                    if (termsEnum.seekExact(bytes)) {
                        final TermState termState = termsEnum.termState();
                        //if (DEBUG) System.out.println("    found");
                        long TF = termsEnum.totalTermFreq();
                        long newTF = Math.round (TFWeight*TF);
                        if(newTF<=termsEnum.docFreq()) newTF=termsEnum.docFreq()+1;
                        perReaderTermState.register(termState, ctx.ord, termsEnum.docFreq(), newTF);
                    }
                }
            }
            return perReaderTermState;
        }

    }

    static class SynonymScorer extends DisjunctionScorer {
        private final Similarity.SimScorer similarity;

        SynonymScorer(Similarity.SimScorer similarity, Weight weight, List<Scorer> subScorers) {
            super(weight, subScorers, true);
            this.similarity = similarity;
        }

        @Override
        protected float score(DisiWrapper topList) throws IOException {
            float freq =  tf(topList);
            if(freq == 0) return 0;
            return similarity.score(topList.doc, freq);
        }

        /** combines TF of all subs. */
        final int tf(DisiWrapper topList) throws IOException {
            int tf = 0;
            for (DisiWrapper w = topList; w != null; w = w.next) {
                TermScorerWrapper scorerWrapper = (TermScorerWrapper)w.scorer;
                tf += Math.round(scorerWrapper.getScorer().freq()*scorerWrapper.getFactor());
            }
            return tf;
        }
    }

    static class TermScorerWrapper extends Scorer {
        private TermScorer scorer;
        private double factor;
        private Term term;
        public TermScorerWrapper(SynonymWeight synonymWeight, PostingsEnum postings, SimScorer simScorer,double factor,Term term) {
            super(synonymWeight);
            this.scorer = new TermScorer(synonymWeight,postings,simScorer);
            this.factor = factor;
            this.term = term;
        }

        public TermScorer getScorer() {
            return scorer;
        }

        public double getFactor() {
            return factor;
        }

        @Override
        public int docID() {
            return scorer.docID();
        }

        @Override
        public float score() throws IOException {
            return scorer.score();
        }

        @Override
        public DocIdSetIterator iterator() {
            return scorer.iterator();
        }
    }
}
