package edu.anadolu.cmdline;


import edu.anadolu.analysis.Tag;
import edu.anadolu.datasets.Collection;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.eval.Evaluator;
import edu.anadolu.eval.SystemEvaluator;
import edu.anadolu.knn.Measure;
import org.clueweb09.InfoNeed;
import org.kohsuke.args4j.Option;

import java.util.*;

/**
 * This class produces the best scores for each information need and IR model among the multisystems (Tags)
 */
public class SystemEvaluatorTool extends CmdLineTool {
    @Option(name = "-collection", required = true, usage = "underscore separated collection values", metaVar = "CW09A_CW12B")
    protected Collection collection;

    @Option(name = "-metric", required = false, usage = "Effectiveness measure")
    protected Measure measure = Measure.NDCG20;

    @Option(name = "-tags", metaVar = "[NoStemTurkish_Zemberek|NoStem_KStem|NoStemTurkish_Zemberek_SnowballTr_F5Stem]", required = true, usage = "Index Tag")
    protected String tags = "NoStemTurkish_Zemberek_SnowballTr_F5Stem";

    @Option(name = "-models", metaVar = "all|custom as in usage", required = false, usage = "ex:BM25k1.5b0.5_PL2c3.5_LGDc1.5_DirichletLMc500")
    protected String models = "all";

    @Option(name = "-op", metaVar = "[AND|OR]", required = false, usage = "query operator (q.op)")
    protected String op = "OR";

    @Option(name = "-fields", metaVar = "[title|body|description|keywords|url|contents]", required = false, usage = "field that you want to search on")
    protected String fields = "contents";

    @Option(name = "-catB", required = false, usage = "use catB qrels for CW12B and CW09B")
    private boolean catB = false;

    @Option(name = "-spam", metaVar = "[10|15|...|85|90]", required = false, usage = "Non-negative integer spam threshold")
    protected int spam = 0;


    @Override
    public void run(Properties props) throws Exception {
        if (parseArguments(props) == -1) return;

        final String tfd_home = props.getProperty("tfd.home");

        if (tfd_home == null) {
            System.out.println(getHelp());
            return;
        }

        DataSet dataSet = CollectionFactory.dataset(collection, tfd_home);

        String evalDirectory = spam == 0 ? "evals" : "spam_" + spam + "_evals";

        if (catB && (Collection.CW09B.equals(collection) || Collection.CW12B.equals(collection)))
            evalDirectory = "catb_evals";

        final String[] tagsArr = tags.split("_");
        //if(tagsArr.length!=2) return;

        Set<String> modelIntersection = new HashSet<>();

        Map<Tag, Evaluator> evaluatorMap = new HashMap<>();

        for (int i = 0; i < tagsArr.length; i++) {
            String tag = tagsArr[i];
            final Evaluator evaluator = new Evaluator(dataSet, tag, measure, models, evalDirectory, op);
            evaluator.oracleMax();
            evaluatorMap.put(Tag.tag(tag), evaluator);
            //needs = evaluator.getNeeds();

            if (i == 0)
                modelIntersection.addAll(evaluator.getModelSet());
            else
                modelIntersection.retainAll(evaluator.getModelSet());
        }



        SystemEvaluator systemEvaluator = new SystemEvaluator(evaluatorMap);
        systemEvaluator.printTopicSystemMatrix();
        systemEvaluator.printTopicModelSortedByVariance();
        systemEvaluator.printCountMap();


        System.out.println("=========  Mean and MeanWT ===========");
        systemEvaluator.printMean();
        systemEvaluator.printMeanWT();

        System.out.println("=========  Random and Oracle ===========");
        systemEvaluator.printRandom();
        systemEvaluator.printRandomMLE();
        systemEvaluator.printRandomX();
        //System.out.println("OracleMin : " + evaluator.oracleMin());
        systemEvaluator.printOracleMax();
        systemEvaluator.printHighestScoresWithCoV(false);

        System.out.println("========= Facets ===========");
        systemEvaluator.printFacets();

        System.out.println("========= SPSS ANOVA Input ===========");
        systemEvaluator.printAnova();
        System.out.println("========= Scores ===========");
        systemEvaluator.printScoreTable();

    }

    @Override
    public String getHelp() {
        return null;
    }

    @Override
    public String getShortDescription() {
        return null;
    }
}
