package edu.anadolu;

import edu.anadolu.analysis.Tag;
import edu.anadolu.cmdline.CLI;
import edu.anadolu.datasets.Collection;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.eval.Evaluator;
import edu.anadolu.eval.SystemEvaluator;
import edu.anadolu.knn.Measure;
import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.util.Pair;
import org.clueweb09.InfoNeed;

import java.util.*;


public class Test {

    @org.junit.Test
    public void test() throws Exception {
        Integer[] spam = new Integer[] { 1, 2, 3,8,10,45,7,4,9,12 };
        List<Integer> t= Arrays.asList(spam);
        t.sort((t1, t2) -> Integer.compare(t1, t2));
        System.out.println(t.toString());
        //String[] args ={"SelectiveStemming","-collection","MC"};
        //CLI.main(args);
    }

    @org.junit.Test
    public void CLITest() throws Exception {

//        String[] args ={"HighestScores","-collection","MC","-metric","NDCG20","-tags","NoStemTurkish_Zemberek"};
//        String[] args ={"Indexer","-collection","MC","-tag","Zemberek"};
        String[] args ={"Searcher","-collection","MC","-task","param"};

        CLI.main(args);
    }

    @org.junit.Test
    public void testRandom() throws Exception {
        String[] tags = {"NoStemTurkish","F5Stem","Snowball","Zemberek"};
        List<Pair<String,Double>> weights = new ArrayList();

            weights.add(new Pair(tags[0], 0.25));
        weights.add(new Pair(tags[1], 0.25));
        weights.add(new Pair(tags[2], 0.25));
        weights.add(new Pair(tags[3], 0.25));

        for(int i=0;i<20;i++) {
            String selectedItem = new EnumeratedDistribution(weights).sample().toString();
            System.out.println(selectedItem);
        }
    }
    @org.junit.Test
    public void testEvaluator(){
        final String tfd_home = "C:\\Data\\TFD_HOME";
        Collection collection=Collection.MC;
        boolean catB = false;
        String tags = "NoStemTurkish_Zemberek_SnowballTr_F5Stem";
        Measure measure = Measure.NDCG20;
        String op = "OR";

        DataSet dataSet = CollectionFactory.dataset(collection, tfd_home);
        int spam =0;
        String evalDirectory = spam == 0 ? "evals" : "spam_" + spam + "_evals";

        if (catB && (Collection.CW09B.equals(collection) || Collection.CW12B.equals(collection)))
            evalDirectory = "catb_evals";

        final String[] tagsArr = tags.split("_");
        //if(tagsArr.length!=2) return;

        Set<String> modelIntersection = new HashSet<>();

        Map<Tag, Evaluator> evaluatorMap = new HashMap<>();

        for (int i = 0; i < tagsArr.length; i++) {
            String tag = tagsArr[i];
            final Evaluator evaluator = new Evaluator(dataSet, tag, measure, "all", evalDirectory, op);
            evaluator.oracleMax();
            evaluatorMap.put(Tag.tag(tag), evaluator);
            //needs = evaluator.getNeeds();

            if (i == 0)
                modelIntersection.addAll(evaluator.getModelSet());
            else
                modelIntersection.retainAll(evaluator.getModelSet());
        }



        SystemEvaluator systemEvaluator = new SystemEvaluator(evaluatorMap);

        systemEvaluator.printTopicModelSortedByVariance();

        //Map<String, List<InfoNeed>> bestModelMap = systemEvaluator.absoluteBestModelMap();
        //Map<String, Double> riskMap = systemEvaluator.riskSOTA();
        //Map<String, Double> ctiMap = systemEvaluator.cti();
        //Map<String, Double> zRiskMap = systemEvaluator.zRisk();
        //Map<String, Double> geoRiskMap = systemEvaluator.geoRisk();

        systemEvaluator.printCountMap();



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
        //systemEvaluator.printFacets();

        //needs = evaluatorMap.get(tagsArr[0]).residualNeeds()
        /*
        needs = evaluatorMap.get(tagsArr[0]).getNeeds();
        Integer needSize = needs.size();
        for (String model : modelIntersection) {
            double avgBestScores=0.0;
            System.out.println(model);
            System.out.println("Query\tTag\t"+measure);
            for(int i=0; i<needSize;i++){
                String bestTag="";
                double bestScore = Double.NEGATIVE_INFINITY;
                for(String tag:tagsArr) {
                    double score = evaluatorMap.get(tag).score(needs.get(i), model);
                    if(score>bestScore){
                        bestScore=score;
                        bestTag=tag;
                    }
                }
                System.out.println(needs.get(i).id()+"\t"+bestTag+"\t"+bestScore);
                avgBestScores+=bestScore;
            }
            System.out.println("Average\t"+avgBestScores/needSize);
            System.out.println("========\t========\t=======");

        }
        */
    }

}
