package edu.anadolu;

import edu.anadolu.analysis.Analyzers;
import edu.anadolu.analysis.Tag;
import edu.anadolu.cmdline.CLI;
import edu.anadolu.cmdline.SpamTool;
import edu.anadolu.datasets.Collection;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.eval.Evaluator;
import edu.anadolu.eval.SystemEvaluator;
import edu.anadolu.knn.Measure;
import edu.anadolu.qpp.Aggregate;
import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.stat.inference.ChiSquareTest;
import org.apache.commons.math3.util.Pair;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;

import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.concurrent.TimeUnit;


public class Test {

    @org.junit.Test
    public void testIgnite() {
        //Ignite ignite = Ignition.ignite();
        IgniteConfiguration cfg = new IgniteConfiguration();

        DataStorageConfiguration storageCfg = new DataStorageConfiguration();
        storageCfg.getDefaultDataRegionConfiguration().setMaxSize(2L * 1024 * 1024 * 1024);//20GB
       // storageCfg.getDefaultDataRegionConfiguration().setPersistenceEnabled(true); //use disk
        cfg.setDataStorageConfiguration(storageCfg);

        Ignite ignite=null;
        IgniteCache<String, LinkedList<String>> cache=null;
        try {
            ignite = Ignition.start(cfg);
            //ignite.cluster().active(true);


            ignite.destroyCache("stemVariants");
            cache = ignite.createCache("stemVariants");
            cache = cache.withExpiryPolicy(new CreatedExpiryPolicy(new Duration(TimeUnit.MINUTES,1)));

            System.out.println("Start");
            LinkedList<String> ls = new LinkedList<String>();
            for (int i = 0; i < 10; i++)
                ls.add(Integer.toString(i));
            cache.put("a", ls);

            LinkedList<String> l = cache.get("a");
            l.add("sad");
            cache.put("a", l);
            System.out.println(cache.get("a"));
            ignite.destroyCache("stemVariants");
            System.out.println("Finish");
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if(cache!=null)cache.close();
            if(ignite!=null)ignite.close();
        }
    }

    @org.junit.Test
    public void testChiSquare() {
        long[] obs1 = {100,120,130};
        long[] obs2 = {100,120,130};

        ChiSquareTest chi = new ChiSquareTest();
        //boolean isSig=chi.chiSquareTest(exp,obs,0.05);
        double pval=chi.chiSquareTestDataSetsComparison(obs1,obs2);
        boolean isSig=chi.chiSquareTestDataSetsComparison(obs1,obs2,0.05);
        System.out.println(isSig+" "+pval);

    }


    @org.junit.Test
    public void testAnalyzer() {
        Analyzer analyzer = Analyzers.analyzer(Tag.SnowballEng);
        try (TokenStream ts = analyzer.tokenStream("contents", new StringReader("1960's"))) {

            final CharTermAttribute termAtt = ts.addAttribute(CharTermAttribute.class);
            ts.reset(); // Resets this stream to the beginning. (Required)
            while (ts.incrementToken())
                System.out.println(termAtt.toString());

            ts.end();   // Perform end-of-stream operations, e.g. set the final offset.
        } catch (IOException ioe) {
            throw new RuntimeException("happened during string analysis", ioe);
        }

    }

    @org.junit.Test
    public void testTirt() {
        Aggregate.Variance v = new Aggregate.Variance();
        double[] a = {2,3,4,4,6};
        double[] b = {2,3,4,4,6};
        System.out.println(Math.round(4.764654234));
        //double v1IDF[] = Arrays.stream(a).map(d->new Aggregate.Average().aggregate(d)).toArray();
        //Arrays.stream(v1IDF).forEach(System.out::println);

       // System.out.println(new Aggregate.GeometricMean().aggregate(a));

        //System.out.println(new Aggregate.HarmonicMean().aggregate(a));
    }

    @org.junit.Test
    public void testSolr() throws IOException, SolrServerException {

        Properties props=CLI.readProperties();
        final String tfd_home = props.getProperty("tfd.home");
        SpamTool.CW09_spam_link = props.getProperty("link.spam.CW09");
        SpamTool.CW12_spam_link = props.getProperty("link.spam.CW12");


        final HttpSolrClient solr = SpamTool.getSpamSolr(Collection.MQ09);

        int p = SpamTool.percentile(solr, "clueweb09-en0010-79-02218");
        System.out.println(p);
    }

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
     //   String[] args ={"TFDistribution","-collection","MC","-task","term"};
      //  String[] args = {"SelectiveStemming", "-collection", "MC", "-tags", "NoStemTurkish_Zemberek", "-metric","NDCG20", "-spam", "0", "-selection", "MSTDF", "-binDF","10"};
        String[] args ={"Stats","-collection","MC"};
     //  String[] args ={"SystemEvaluator","-collection","MC","-metric","NDCG20","-tags","NoStemTurkish_Zemberek_SnowballTr_F5Stem"};
//        String[] args ={"Indexer","-collection","MC","-tag","Zemberek"};
     //   String[] args ={"Searcher","-collection","MC","-task","param"};

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
