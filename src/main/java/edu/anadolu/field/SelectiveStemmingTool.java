package edu.anadolu.field;

import edu.anadolu.QuerySelector;
import edu.anadolu.SelectionMethods;
import edu.anadolu.analysis.Analyzers;
import edu.anadolu.analysis.Tag;
import edu.anadolu.cmdline.CLI;
import edu.anadolu.cmdline.CmdLineTool;
import edu.anadolu.datasets.Collection;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.eval.Evaluator;
import edu.anadolu.eval.ModelScore;
import edu.anadolu.eval.SystemEvaluator;
import edu.anadolu.eval.SystemScore;
import edu.anadolu.knn.Measure;
import edu.anadolu.knn.Prediction;
import edu.anadolu.knn.Solution;
import edu.anadolu.stats.TermStats;
import org.apache.commons.math3.stat.inference.TTest;
import org.apache.lucene.analysis.Analyzer;
import org.clueweb09.InfoNeed;
import org.kohsuke.args4j.Option;

import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static edu.anadolu.field.FieldTool.sortByValue;


public class SelectiveStemmingTool extends CmdLineTool {
    @Option(name = "-collection", required = true, usage = "underscore separated collection values", metaVar = "MQ08")
    protected Collection collection;

    @Override
    public String getShortDescription() {
        return "Selective Stemming utility";
    }

    @Override
    public String getHelp() {
        return "Following properties must be defined in config.properties for " + CLI.CMD + " " + getName() + " tfd.home";
    }

    @Option(name = "-metric", required = false, usage = "Effectiveness measure")
    protected Measure measure = Measure.MAP;

    @Option(name = "-tags", metaVar = "[NoStemTurkish_Zemberek|NoStem_KStem]", required = false, usage = "First:NoStem Second:Stem")
    protected String tags = "NoStemTurkish_Zemberek";

    @Option(name = "-selection", metaVar = "[MSTTF|MSTDF|TFOrder|DFOrder|KendallTauTFOrder|KendallTauDFOrder|MSTTFBinning|MSTDFBinning" +
            "TFOrderBinning|DFOrderBinning|KendallTauTFOrderBinning|KendallTauDFOrderBinning]", required = true, usage = "Selection Tag")
    protected String selection;

    @Option(name = "-KTT", required = false, usage = "Kendall Tau correlation threshold [-1...1]")
    protected double ktt = 0.99;

    @Option(name = "-binDF", required = false, usage = "Number of bins for DF. Default: 1000 ")
    protected int binDF = 1000;

    @Option(name = "-residualNeeds", required = false, usage = "Removes ALL_SAME and ALL_ZERO")
    protected boolean residualNeeds = false;


    @Option(name = "-spam", metaVar = "[10|15|...|85|90]", required = false, usage = "Non-negative integer spam threshold")
    protected int spam = 0;

    @Option(name = "-op", metaVar = "[AND|OR]", required = false, usage = "query operator (q.op)")
    protected String op = "OR";

    @Option(name = "-fields", metaVar = "[title|body|description|keywords|url|contents]", required = false, usage = "field that you want to search on")
    protected String fields = "contents";

    @Option(name = "-catB", required = false, usage = "use catB qrels for CW12B and CW09B")
    private boolean catB = false;

    private final TTest tTest = new TTest();

    @Override
    public void run(Properties props) throws Exception {

        if (parseArguments(props) == -1) return;

        final String tfd_home = props.getProperty("tfd.home");

        if (tfd_home == null) {
            System.out.println(getHelp());
            return;
        }

        SelectionMethods.KendallTauThreshold=ktt;
        SelectionMethods.TermTFDF.NumberOfBIN=binDF;

        DataSet dataSet = CollectionFactory.dataset(collection, tfd_home);

        String evalDirectory = spam == 0 ? "evals" : "spam_" + spam + "_evals";

        if (catB && (Collection.CW09B.equals(collection) || Collection.CW12B.equals(collection)))
            evalDirectory = "catb_evals";

        //Map<String, Evaluator> evaluatorMap = new HashMap<>();
        Map<Tag, QuerySelector> querySelectorMap = new HashMap<>();

        final String[] tagsArr = tags.split("_");
        if(tagsArr.length!=2) return;

        Set<String> modelIntersection = new HashSet<>();

        Map<Tag, Evaluator> evaluatorMap = new HashMap<>();

        for (int i = 0; i < tagsArr.length; i++) {
            String tag = tagsArr[i];
            final Evaluator evaluator = new Evaluator(dataSet, tag, measure, "all", evalDirectory, op);
            evaluatorMap.put(Tag.tag(tag), evaluator);

            if (i == 0)
                modelIntersection.addAll(evaluator.getModelSet());
            else
                modelIntersection.retainAll(evaluator.getModelSet());
            querySelectorMap.put(Tag.tag(tag), new QuerySelector(dataSet, tag));
        }

       /*
        for (int i = 0; i < tagsArr.length; i++) {
            String tag = tagsArr[i];
            final Evaluator evaluator = new Evaluator(dataSet, tag, measure, "all", evalDirectory, op);
            evaluatorMap.put(tag, evaluator);
            needs = evaluator.getNeeds();

            if (i == 0)
                modelIntersection.addAll(evaluator.getModelSet());
            else
                modelIntersection.retainAll(evaluator.getModelSet());
            querySelectorMap.put(tag, new QuerySelector(dataSet, tag));
        }

        */

        SystemEvaluator systemEvaluator = new SystemEvaluator(evaluatorMap);


        Map<String, double[]> baselines = new HashMap<>();

        for (String model : modelIntersection) {
            List<InfoNeed> needs;
            if (residualNeeds) needs=systemEvaluator.residualNeeds(model);
            else needs=systemEvaluator.getNeeds();

            Solution s = systemEvaluator.oracleMaxAsSolution(needs,model);
            baselines.put(model, s.scores());
        }


        for (String model : modelIntersection) {
            List<InfoNeed> needs;
            if (residualNeeds) needs=systemEvaluator.residualNeeds(model);
            else needs=systemEvaluator.getNeeds();

            List<SystemScore> list = new ArrayList<>();

            for (String tag : tagsArr) {
                final Evaluator evaluator = evaluatorMap.get(Tag.tag(tag));
                ModelScore modelScore = evaluator.averagePerModel(model,needs);
                list.add(new SystemScore(tag, modelScore.score));
            }

            System.err.println(String.format("%s\t",model)); //print part1
            Solution solution = selectiveStemmingSolution(model,evaluatorMap,querySelectorMap,needs,tagsArr);
            if (tTest.pairedTTest(baselines.get(model), solution.scores(), 0.05))
                list.add(new SystemScore("SelectiveStemming" + "*", solution.getMean()));
            else
                list.add(new SystemScore("SelectiveStemming", solution.getMean()));

            Collections.sort(list);

            System.out.print(model + "(" + needs.size() + ")\t");

            for (SystemScore systemScore : list)
                System.out.print(systemScore.system + "(" + String.format("%.5f", systemScore.score) + ")\t");

            System.out.println();

        }

        System.out.println("========= oracles ==============");
        // if (!collection.equals(GOV2)) fields += ",anchor";

        for (String model : modelIntersection) {
            List<InfoNeed> needs;
            if (residualNeeds) needs=systemEvaluator.residualNeeds(model);
            else needs=systemEvaluator.getNeeds();

            Solution solution = systemEvaluator.oracleMaxAsSolution(needs,model);

            Map<String,List<Prediction>> tagPredictions = solution.list.stream().collect(Collectors.groupingBy(s -> s.predictedModel));
            Map<String,Integer> countMap= tagPredictions.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,e->e.getValue().size()));


            System.out.print(String.format("%s(%.5f) \t", model, solution.getMean()));

            countMap = sortByValue(countMap);
            for (Map.Entry<String, Integer> entry : countMap.entrySet())
                System.out.print(entry.getKey() + "(" + entry.getValue() + ")\t");

            System.out.println();
        }
    }

    private Solution selectiveStemmingSolution(String model,Map<Tag, Evaluator> evaluatorMap,Map<Tag,
            QuerySelector> querySelectorMap,List<InfoNeed> needs, String[] tagsArr){

        List<Prediction> list = new ArrayList<>(needs.size());

        for (InfoNeed need : needs) {
            String predictedTag;
            Map<String,ArrayList<TermStats>> tagTermStatsMap = new LinkedHashMap<>();
            for (String tag : tagsArr) {
                QuerySelector selector = querySelectorMap.get(Tag.tag(tag));

                SelectionMethods.TermTFDF.maxDF=selector.numberOfDocuments;
                SelectionMethods.TermTFDF.maxTF=selector.numberOfTokens;

                Map<String, TermStats> statsMap = selector.termStatisticsMap;
               // Evaluator evaluator = evaluatorMap.get(tag);
                String query = need.query();
                Analyzer analyzer = Analyzers.analyzer(Tag.tag(tag));
                for (String term : Analyzers.getAnalyzedTokens(query, analyzer)) {
                    TermStats termStats = statsMap.get(term);
                    if (termStats == null) {
                        //System.out.println(tag+" index does not contain the term: "+ term);
                        termStats = new TermStats(term,0,0,0);//indexes do not contain query term
                        //throw new RuntimeException("Term stats cannot be null: "+ term );
                    }
                    ArrayList<TermStats> termStatsList=tagTermStatsMap.get(tag);
                    if(termStatsList==null){
                        ArrayList<TermStats> l = new ArrayList<>();
                        l.add(termStats);
                        tagTermStatsMap.put(tag,l);
                    }else termStatsList.add(termStats);

                    //System.out.println(tag + " " + term + " " + termStats.totalTermFreq() + " " + termStats.docFreq());
                   // double score = evaluator.score(need, model);
                   // System.out.println(tag + " " + need.id() + " " + score);
                }
            }
            System.err.print(String.format("%s\t%s\t",need.id(),need.query())); //print part2
            predictedTag = SelectionMethods.getPredictedTag(selection,tagTermStatsMap,tagsArr); ///print part3 will done inside
            double predictedScore = evaluatorMap.get(Tag.tag(predictedTag)).score(need, model);
            Prediction prediction = new Prediction(need, predictedTag, predictedScore);
            list.add(prediction);
            System.err.println(String.format("%s\t%f\t%s\t", predictedTag, predictedScore, selection)); //print part4

        }
        Solution solution = new Solution(list, -1);
        System.out.print(String.format("%s(%.5f) \t", model, solution.getMean()));
        return solution;
    }
}
