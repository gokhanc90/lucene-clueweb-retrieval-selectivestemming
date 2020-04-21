package edu.anadolu.cmdline;

import edu.anadolu.QuerySelector;
import edu.anadolu.analysis.Analyzers;
import edu.anadolu.analysis.Tag;
import edu.anadolu.datasets.Collection;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.eval.Evaluator;
import edu.anadolu.knn.Measure;
import edu.anadolu.qpp.*;
import edu.anadolu.similarities.*;
import edu.anadolu.stats.DocLengthStats;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.similarities.ModelBase;
import org.clueweb09.InfoNeed;
import org.kohsuke.args4j.Option;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Feature Extraction Tool
 */
public final class FeatureTool extends CmdLineTool {

    @Option(name = "-collection", required = true, usage = "Collection")
    protected edu.anadolu.datasets.Collection collection;
    @Option(name = "-tag", metaVar = "[KStem|KStemAnchor]", required = false, usage = "Index Tag")
    protected String tag = Tag.KStem.toString();
    @Option(name = "-measure", required = false, usage = "Effectiveness measure")
    protected Measure measure = Measure.NDCG100;
    @Option(name = "-task", required = false, usage = "task to be executed")
    private String task;

    @Override
    public String getShortDescription() {
        return "Feature Extraction Tool";
    }

    @Override
    public String getHelp() {
        return "Following properties must be defined in config.properties for " + CLI.CMD + " " + getName() + " paths.indexes tfd.home";
    }

    protected String evalDirectory(DataSet dataset) {
        if (Collection.GOV2.equals(dataset.collection()) || Collection.MC.equals(dataset.collection()) || Collection.ROB04.equals(dataset.collection())) {
            return "evals";
        } else {
            final int bestSpamThreshold = SpamEvalTool.bestSpamThreshold(dataset, tag, measure, "OR");
            return bestSpamThreshold == 0 ? "evals" : "spam_" + bestSpamThreshold + "_evals";
        }
    }

    @Override
    public void run(Properties props) throws Exception {

        if (parseArguments(props) == -1) return;

        final String tfd_home = props.getProperty("tfd.home");

        if (tfd_home == null) {
            System.out.println(getHelp());
            return;
        }

        DataSet dataset = CollectionFactory.dataset(collection, tfd_home);

        if ("labels".equals(task)) {
            Evaluator evaluator = new Evaluator(dataset, tag, measure, "all", evalDirectory(dataset), "OR");
            List<InfoNeed> needs = evaluator.getNeeds();

            // Print header
            System.out.println("QueryID\tWinner\t" + measure + "\tLoser\t" + measure);
            for (InfoNeed need : needs) {
                System.out.println("qid:" + need.id() + "\t" + evaluator.bestModel(need, false) + "\t" + evaluator.bestModelScore(need, false) + "\t" + evaluator.bestModel(need, true) + "\t" + evaluator.bestModelScore(need, true));
            }
            return;
        }

        Path synonymPath = dataset.collectionPath();
        Analyzer analyzer, comAnalyzer ;


        if (tag.contains("Synonym")){
            comAnalyzer = Analyzers.analyzer(Tag.tag(tag), synonymPath);
            tag = "NoStem";
        }else comAnalyzer=Analyzers.analyzer(Tag.tag(tag));

        analyzer = Analyzers.analyzer(Tag.tag(tag));

        PMI pmi = new PMI(dataset.indexesPath().resolve(tag), "contents");
        SCS scs = new SCS(dataset.indexesPath().resolve(tag), "contents");
        SCQ scq = new SCQ(dataset.indexesPath().resolve(tag));
        IDF idf = new IDF(dataset.indexesPath().resolve(tag));
        ICTF ictf = new ICTF(dataset.indexesPath().resolve(tag));
        CTI cti = new CTI(dataset.indexesPath().resolve(tag));
        Scope scope = new Scope(dataset.indexesPath().resolve(tag));
        Commonality com = new Commonality(dataset.indexesPath().resolve(tag));
        com.setAnalyzer(comAnalyzer);
        SCCS sccs = new SCCS(dataset.indexesPath().resolve(tag), "contents");
        sccs.setAnalyzer(comAnalyzer);
        SCCQ sccq = new SCCQ(dataset.indexesPath().resolve(tag));
        sccq.setAnalyzer(comAnalyzer);
        Advance adv = new Advance(dataset.indexesPath().resolve(tag));
        GeneralizedTW generalizedTW = new GeneralizedTW(dataset.indexesPath().resolve(tag));

        QuerySelector querySelector = new QuerySelector(dataset, tag);

        // Print header

        System.out.println("QueryID\tWordCount\tGamma\tOmega\tAvgPMI\tSCS\tMeanICTF\tVarICTF\tMeanIDF\tVarIDF\tMeanCTI\tVarCTI" +
                "\tMeanSkew\tVarSkew\tMeanKurt\tVarKurt\tMeanSCQ\tVarSCQ\tMeanCommonality\tVarCommonality\tSCCS\tMeanSCCQ\tVarSCCQ\tAdvance" +
                "\tBM25"+"\tDLM"+"\tLGD"+"\tPL2"+"\tDFIC"+"\tDPH"+"\tDLH13"+"\tDFRee");
        if (task.equals("term")) {
            System.err.println("QueryID\t" + "word\t" + "ictfs" + "\t" + "idfs" + "\t" + "ctis" + "\t" + "skew" + "\t" + "kurt" + "\t" + "scqs" + "\t" + "commonalities" + "\t" + "DF\t" + "TF\t" + "sccq\t" + "DocLenAcc\t" + "advance"
                   + "\tBM25"+"\tDLM"+"\tLGD"+"\tPL2"+"\tDFIC"+"\tDPH"+"\tDLH13"+"\tDFRee");
        }
        for (InfoNeed need : querySelector.allQueries) {

            Map<String, String> map = querySelector.getFrequencyDistributionList(need, "contents_all_freq_1000.csv");

            if(map.values().stream().anyMatch(c->c.contains("(stopword)"))){
                //System.err.println(need.query()+" is skipping");
                continue;
            }

            List<String> analyzedTokens = Analyzers.getAnalyzedTokens(need.query(), analyzer);

            double[] idfs = new double[analyzedTokens.size()];
            double[] ictfs = new double[analyzedTokens.size()];
            double[] ctis = new double[analyzedTokens.size()];
            double[] skew = new double[analyzedTokens.size()];
            double[] kurt = new double[analyzedTokens.size()];
            double[] scqs = new double[analyzedTokens.size()];
            double[] commonalities = new double[analyzedTokens.size()];
            double[] sccqs = new double[analyzedTokens.size()];
            long[] TFs = new long[analyzedTokens.size()];
            long[] DFs = new long[analyzedTokens.size()];
            long[] DocLenAccs = new long[analyzedTokens.size()];
            double[] advs = new double[analyzedTokens.size()];

            double[] BM25s = new double[analyzedTokens.size()];
            double[] DLMs = new double[analyzedTokens.size()];
            double[] LGDs = new double[analyzedTokens.size()];
            double[] PL2s = new double[analyzedTokens.size()];
            double[] DPHs = new double[analyzedTokens.size()];
            double[] DFRees = new double[analyzedTokens.size()];
            double[] DLH13s = new double[analyzedTokens.size()];
            double[] DFICs = new double[analyzedTokens.size()];

            for (int c = 0; c < analyzedTokens.size(); c++) {
                String word = analyzedTokens.get(c);
                String freqLine = map.get(word);
                DescriptiveStatistics descriptiveStatistics = querySelector.toDescriptiveStatistics(freqLine);

                idfs[c] = idf.value(word);
                DFs[c] = com.df(word);
                DocLenAccs[c] = com.getdocLenAcc(); //It should be called after com.df()
                ictfs[c] = ictf.value(word);
                TFs[c] = com.TF(word);
                ctis[c] = cti.value(word);
                skew[c] = descriptiveStatistics.getSkewness();
                kurt[c] = descriptiveStatistics.getKurtosis();
                scqs[c] = scq.value(word);
                commonalities[c] = com.value(word);
                sccqs[c] = sccq.value(word);
                advs[c] = adv.valueCom(word,DFs[c],TFs[c]);

                BM25s[c]  = generalizedTW.valueCom(ParamTool.string2model("BM25k1.2b0.75"),DFs[c],TFs[c],DocLenAccs[c]);
                DLMs[c]  = generalizedTW.valueCom(ParamTool.string2model("DirichletLMc2500.0"),DFs[c],TFs[c],DocLenAccs[c]);
                LGDs[c]  = generalizedTW.valueCom(ParamTool.string2model("LGDc1.0"),DFs[c],TFs[c],DocLenAccs[c]);
                PL2s[c]  = generalizedTW.valueCom(ParamTool.string2model("PL2c1.0"),DFs[c],TFs[c],DocLenAccs[c]);
                DFICs[c]  = generalizedTW.valueCom(new DFIC(),DFs[c],TFs[c],DocLenAccs[c]);
                DPHs[c]  = generalizedTW.valueCom(new DPH(),DFs[c],TFs[c],DocLenAccs[c]);
                DLH13s[c]  = generalizedTW.valueCom(new DLH13(),DFs[c],TFs[c],DocLenAccs[c]);
                DFRees[c]  = generalizedTW.valueCom(new DFRee(),DFs[c],TFs[c],DocLenAccs[c]);

                if (task.equals("term")){
                    System.err.println(need.id() + "\t"+ word +"\t"+ ictfs[c] + "\t" + idfs[c] + "\t" + ctis[c] + "\t" + skew[c] + "\t" + kurt[c] + "\t" + scqs[c] + "\t" + commonalities[c] + "\t"
                            + DFs[c] + "\t"+ TFs[c] + "\t"+ sccqs[c]+"\t"+DocLenAccs[c]+"\t"+advs[c]+"\t"+
                            BM25s[c] + "\t"+ DLMs[c]  + "\t"+ LGDs[c]  + "\t"+ PL2s[c] + "\t"+  DFICs[c]  + "\t"+ DPHs[c]  + "\t"+ DLH13s[c]  + "\t"+ DFRees[c]);
                    }
            }
            System.out.print("qid:" + need.id() + "\t" + need.wordCount() + "\t" + idf.aggregated(need, new Aggregate.Gamma1()) + "\t" + scope.value(need) + "\t");
            System.out.print(pmi.value(need) + "\t" + scs.value(need) + "\t");
            System.out.print(StatUtils.mean(ictfs) + "\t" + StatUtils.variance(ictfs) + "\t");
            System.out.print(StatUtils.mean(idfs) + "\t" + StatUtils.variance(idfs) + "\t");
            System.out.print(StatUtils.mean(ctis) + "\t" + StatUtils.variance(ctis) + "\t");
            System.out.print(StatUtils.mean(skew) + "\t" + StatUtils.variance(skew) + "\t" + StatUtils.mean(kurt) + "\t" + StatUtils.variance(kurt) + "\t");
            System.out.print(StatUtils.mean(scqs) + "\t" + StatUtils.variance(scqs) + "\t");
            System.out.print(StatUtils.mean(commonalities) + "\t" + StatUtils.variance(commonalities) + "\t");
            System.out.print(sccs.value(need) + "\t");
            System.out.print(StatUtils.mean(sccqs) + "\t" + StatUtils.variance(sccqs) + "\t");
            System.out.print(StatUtils.mean(advs) + "\t");
            System.out.print(StatUtils.sum(BM25s) + "\t");
            System.out.print(StatUtils.sum(DLMs) + "\t");
            System.out.print(StatUtils.sum(LGDs) + "\t");
            System.out.print(StatUtils.sum(PL2s) + "\t");
            System.out.print(StatUtils.sum(DFICs) + "\t");
            System.out.print(StatUtils.sum(DPHs) + "\t");
            System.out.print(StatUtils.sum(DLH13s) + "\t");
            System.out.println(StatUtils.sum(DFRees) + "\t");

            System.gc();

        }

        pmi.close();
        scs.close();
        scq.close();
        idf.close();
        ictf.close();
        cti.close();
        scope.close();
        com.close();
        sccq.close();
        sccs.close();
    }
}
