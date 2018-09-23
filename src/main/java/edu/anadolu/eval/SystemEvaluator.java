package edu.anadolu.eval;


import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import edu.anadolu.analysis.Tag;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.knn.Measure;
import edu.anadolu.knn.Prediction;
import edu.anadolu.knn.Solution;
import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.stat.StatUtils;
import org.clueweb09.InfoNeed;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class SystemEvaluator{

    private Metric metric;
    private int k;
    private final Map<Tag,Evaluator> tagEvaluatorMap;
    private Set<String> modelIntersection;
    private List<String> tags;
    private List<InfoNeed> needs;
    private final Map<String,Set<InfoNeed>> modelAllZeroMap = new HashMap<>();
    private final Map<String,Set<InfoNeed>> modelAllSameMap = new HashMap<>();

    private Map<InfoNeed, Map<String,List<SystemScore>>> performanceMap = null; //InfoNeed, DPH {NoStem0.924,Stem0.854}
    private Map<InfoNeed, Map<String,Double>> varianceMap = null;  ////InfoNeed, DPH variance
    public Map<String,Map<String, List<InfoNeed>>> bestModelSystemMap;

    private final NormalDistribution normalDistribution = new NormalDistribution();

    public SystemEvaluator(List<String> tags,DataSet dataSet,Measure measure, String evalDirectory,String op) {
        this.tags=tags;
        this.metric=measure.metric();
        this.k=measure.k();
        tagEvaluatorMap = new HashMap<>();
        for (String tag: tags) {
            final Evaluator evaluator = new Evaluator(dataSet, tag, measure, "all", evalDirectory, op);
            tagEvaluatorMap.put(Tag.tag(tag), evaluator);

        }
        initialize();
    }
    public SystemEvaluator(Map<Tag, Evaluator> tagEvaluatorMap) {
        this.tagEvaluatorMap = tagEvaluatorMap;
        tags = new ArrayList<>();
        tagEvaluatorMap.keySet().stream().forEach(tag -> tags.add(tag.name()));
        this.metric=tagEvaluatorMap.entrySet().stream().findAny().get().getValue().metric;
        this.k=tagEvaluatorMap.entrySet().stream().findAny().get().getValue().k;
        initialize();
    }

    private void initialize() {
        setNeeds();
        setModelIntersection();
        populatePerformanceMap();
        fillBestSystemMap();
    }
    private void populatePerformanceMap() {

        Map<InfoNeed, Map<String,List<SystemScore>>> performanceMap = new HashedMap<>();
        Map<InfoNeed, Map<String,Double>> varianceMap =  new HashedMap<>();

        for (InfoNeed need : needs) {
            Map<String,List<SystemScore>> modelSytemScoreMap = new HashMap<>();
            Map<String,Double> modelVarianceMap = new HashMap<>();
            for(String model: modelIntersection) {
                List<SystemScore> list = systemScoreList(need,model);
                modelSytemScoreMap.put(model,list);
                modelVarianceMap.put(model,variance(list));
            }
            performanceMap.put(need, modelSytemScoreMap);
            varianceMap.put(need, modelVarianceMap);
        }

        this.performanceMap = Collections.unmodifiableMap(performanceMap);
        this.varianceMap = Collections.unmodifiableMap(varianceMap);
    }

    private Double variance(List<SystemScore> list) {
        return Evaluator.variance(list.stream().map(s->new ModelScore(s.system,s.score)).collect(Collectors.toList()));
    }

    private List<SystemScore> systemScoreList(InfoNeed need, String model) {
        List<SystemScore> list = new ArrayList<>();
        for(String tag:tags){
            Evaluator evaluator = tagEvaluatorMap.get(Tag.tag(tag));
            Double score = evaluator.score(need,model);
            list.add(new SystemScore(tag, score));
        }
        return list;
    }

    private void fillBestSystemMap() {
        Map<String,Map<String, List<InfoNeed>>> mapBestModelSystem = new HashMap<>();
        for(String model:modelIntersection) {
            Map<String, List<InfoNeed>> map = new HashMap<>();

            HashSet<InfoNeed> needsZero = new HashSet<>();
            HashSet<InfoNeed> needsSame = new HashSet<>();
            for (InfoNeed need : needs) {

                List<SystemScore> list = performanceMap.get(need).get(model);
                Collections.sort(list);

                SystemScore best = list.get(0);
                String bestSystem = best.system;

                if (best.score == 0.0)
                    bestSystem = "ALL_ZERO";
                else {
                    boolean allSameFlag = true;
                    for (SystemScore modelScore : list) {
                        if (best.score != modelScore.score) {
                            allSameFlag = false;
                            break;
                        }
                    }
                    if (allSameFlag) bestSystem = "ALL_SAME";
                }

                if ("ALL_ZERO".equals(bestSystem))
                    needsZero.add(need);
                else if ("ALL_SAME".equals(bestSystem))
                    needsSame.add(need);
                else if (best.score == list.get(1).score) {

                    // System.out.println("Found tie for " + best);

                    for (int i = 1; i < list.size(); i++) {
                        if (list.get(i).score == best.score) {
                            //  System.out.println(list.get(i));
                            Evaluator.addSingleItem2Map(map, list.get(i).system, need);
                        }
                    }

                }

                Evaluator.addSingleItem2Map(map, bestSystem, need);

            }

            mapBestModelSystem.put(model, map);
            modelAllSameMap.put(model,needsSame);
            modelAllZeroMap.put(model, needsZero);
        }
        this.bestModelSystemMap = Collections.unmodifiableMap(mapBestModelSystem);
    }

    private void setNeeds() {
        Evaluator evaluator = tagEvaluatorMap.entrySet().stream().findAny().get().getValue();
        needs=evaluator.getNeeds();
    }

    private void setModelIntersection() {
        modelIntersection = new HashSet<>();
        boolean first = true;
        for (Tag tag: tagEvaluatorMap.keySet()) {
            Evaluator evaluator = tagEvaluatorMap.get(tag);
            if (first) {
                modelIntersection.addAll(evaluator.getModelSet());
                first=false;
            }
            else
                modelIntersection.retainAll(evaluator.getModelSet());
        }
    }
    public void printMeanWT(){
        for (Tag tag: tagEvaluatorMap.keySet()) {
            Evaluator evaluator = tagEvaluatorMap.get(tag);
            System.out.print(tag + "\t");
            evaluator.printMeanWT(modelIntersection);
        }

    }
    public void printMean(){
        for (Tag tag: tagEvaluatorMap.keySet()) {
            Evaluator evaluator = tagEvaluatorMap.get(tag);
            System.out.print(tag + "\t");
            evaluator.printMean(modelIntersection);
        }
    }
    public void printOracleMax(){
        for(String model:modelIntersection) {
            Solution s = oracleMaxAsSolution(needs, model);
            System.out.println(s.key + " max:\t" + s.getMean()+"\t"+s.hits0+"\t"+s.hits1+"\t"+s.hits2);
        }
    }
    public void printRandom(){
        for(String model:modelIntersection) {
            Solution s = randomAsSolution(needs, model);
            System.out.println(s.key + ":\t" + s.getMean()+"\t"+s.hits0+"\t"+s.hits1+"\t"+s.hits2);
        }
    }

    public void printRandomMLE(){
        for(String model:modelIntersection) {
            Solution s = randomMLE(needs, model);
            System.out.println(s.key + " :\t " + s.getMean()+"\t"+s.hits0+"\t"+s.hits1+"\t"+s.hits2);
        }
    }

    public void printRandomX(){
        for(String model:modelIntersection) {
            SystemScore s = randomX(model);
            System.out.println(s.system + " : " + s.score);
        }
    }

    public Solution oracleMaxAsSolution(List<InfoNeed> needs, String model) {

        List<Prediction> list = new ArrayList<>(needs.size());

        for (InfoNeed testQuery : needs) {
            double bestScore = Double.NEGATIVE_INFINITY;
            String bestTag = null;

            for (String tag : tags) {
                double score = tagEvaluatorMap.get(Tag.tag(tag)).score(testQuery, model);
                if (score > bestScore) {
                    bestScore = score;
                    bestTag = tag;
                }
            }

            if (null == bestTag) throw new RuntimeException("predictedSystem is null!");

            Prediction prediction = new Prediction(testQuery, bestTag, bestScore);
            list.add(prediction);
        }
        Solution solution = new Solution(list, -1);
        calculateAccuracy(solution, model);

        solution.setKey("Oracle\t" + model);
        solution.model = model;
        return solution;

    }

    public void printFacets() {
        for(String model:modelIntersection) {
            System.out.println("-------" + model + "-------");
            Multiset<String> multiset = facet(model);
            for (String key : Multisets.copyHighestCountFirst(multiset).elementSet()) {
                System.out.println(key + "\t(" + multiset.count(key) + ")");
            }
        }
    }

    public void printHighestScoresWithCoV(boolean residualNeeds){
        for(String model:modelIntersection) {
            Solution s;
            if(residualNeeds) s = oracleMaxAsSolution(residualNeeds(model),model);
            else s = oracleMaxAsSolution(needs,model);
            System.out.println("-----" + model + "-----");
            System.out.printf("Query\tSystem\t%s_%d\tCOV\n", metric, k);
            for(Prediction p: s.list){
                double variance = varianceMap.get(p.testQuery).get(s.model);
                System.out.printf("%d\t%s\t%.4f\t%.4f\n", p.testQuery.id(), p.predictedModel, p.predictedScore, variance);
            }
            System.out.printf("Average:\t%.4f\n", s.getMean());
        }
    }

    public List<InfoNeed> residualNeeds(String model) {

        List<InfoNeed> residualNeeds = new ArrayList<>(needs);
        residualNeeds.removeAll(modelAllSameMap.get(model));
        residualNeeds.removeAll(modelAllZeroMap.get(model));
        return residualNeeds;
    }
    private Multiset<String> facet(String model) {

        Multiset<String> multiset = HashMultiset.create();

        for (InfoNeed need : needs) {

            if (modelAllZeroMap.get(model).contains(need) || modelAllSameMap.get(model).contains(need))
                continue;

            List<SystemScore> list = performanceMap.get(need).get(model);
            Collections.sort(list);
            multiset.add(list.get(0).system + "_" + list.get(list.size() - 1).system);

        }

        return multiset;
    }

    public void calculateAccuracy(Solution solution,String model) {
        solution.hits2 = hits(solution, multiLabelMap(2.0,model),model);
        solution.hits1 = hits(solution, multiLabelMap(1.0,model),model);
        solution.hits0 = hits(solution, singleLabelMap(model),model);

        solution.sigma1 = (double) solution.hits1 / solution.list.size() * 100.0;
        solution.sigma0 = (double) solution.hits0 / solution.list.size() * 100.0;
    }
    public Map<InfoNeed, Set<String>> multiLabelMap(double se,String model) {
        // SE x 2
        LinkedHashMap<InfoNeed, Set<String>> multiLabelMap = new LinkedHashMap<>();
        if (se == 2.0) {
            for (InfoNeed need : needs) {
                if (modelAllZeroMap.get(model).contains(need) || modelAllSameMap.get(model).contains(need))
                    continue;
                LinkedHashSet<String> winners = multiLabelWinners(need, se,model);
                multiLabelMap.put(need, winners);
            }
            return multiLabelMap;
        }

        for (InfoNeed need : needs) {
            if (modelAllZeroMap.get(model).contains(need) || modelAllSameMap.get(model).contains(need))
                continue;
            LinkedHashSet<String> winners = multiLabelWinners(need, se, model);
            multiLabelMap.put(need, winners);
        }
        return multiLabelMap;
    }
    private LinkedHashSet<String> multiLabelWinners(InfoNeed need, double se,String model) {
        List<SystemScore> list = performanceMap.get(need).get(model);
        Collections.sort(list);
        double standardError = Math.sqrt(varianceMap.get(need).get(model) / list.size());
        return sigmaLabels(list, standardError * se);
    }
    private LinkedHashSet<String> sigmaLabels(List<SystemScore> list, double sigma) {

        SystemScore bestSingleSystem = list.get(0);

        double best = bestSingleSystem.score;

        LinkedHashSet<String> set = new LinkedHashSet<>();

        set.add(bestSingleSystem.system);

        for (SystemScore systemScore : list) {
            if (systemScore.score > (best - sigma))
                set.add(systemScore.system);
            else
                break;
        }

        return set;
    }
    public Map<InfoNeed, Set<String>> singleLabelMap(String model) {
        Map<InfoNeed, Set<String>> singleLabelMap;

        singleLabelMap = Evaluator.revert(bestModelSystemMap.get(model));

        return singleLabelMap;
    }

    private int hits(Solution solution, final Map<InfoNeed, Set<String>> labelMap,String model) {

        int correct = 0;
        for (Prediction prediction : solution.list) {

            if (modelAllZeroMap.get(model).contains(prediction.testQuery) || modelAllSameMap.get(model).contains(prediction.testQuery)) {
                correct++;
                continue;
            }

            if (prediction.predictedModel == null) {

                System.out.println("---------------" + prediction.predictedScore);

                for (String winnerTag : labelMap.get(prediction.testQuery)) {
                    double score = tagEvaluatorMap.get(Tag.tag(winnerTag)).score(prediction.testQuery, model);
                    System.out.println(score);
                    if (prediction.predictedScore >= score) {
                        correct++;
                        break;
                    }
                }

            } else {
                if (labelMap.get(prediction.testQuery).contains(prediction.predictedModel))
                    correct++;
            }
        }

        return correct;

    }
    public Map<String, Double> geoRisk(String model) {

        final Map<String, Double> geoRiskMap = new HashMap<>();
        final double c = needs.size();
        final Map<String, Double> zRiskMap = zRisk(model);

        for (String systems : tags) {
            double si = 0.0;

            for (InfoNeed need : needs) {
                double score = tagEvaluatorMap.get(Tag.tag(systems)).score(need, model);
                si += score;
            }

            final double zRisk = zRiskMap.get(systems);
            final double geoRisk = Math.sqrt(si / c * normalDistribution.cumulativeProbability(zRisk / c));
            geoRiskMap.put(systems, geoRisk);
        }
        return geoRiskMap;

    }
    public Map<String, Double> zRisk(String model) {

        final Map<String, Double> rowSum = rowSum(model);
        final Map<InfoNeed, Double> columnSum = columnSum(model);

        final double N = N(model);

        Map<String, Double> zRiskMap = new HashMap<>();

        for (String system : tags) {
            double zRisk = 0.0;
            for (InfoNeed need : needs) {
                final double e = rowSum.get(system) * columnSum.get(need) / N;
                if (e == 0.0) continue;
                double score = tagEvaluatorMap.get(Tag.tag(system)).score(need, model);
                zRisk += (score - e) / Math.sqrt(e);
            }
            zRiskMap.put(system, zRisk);
        }
        return zRiskMap;
    }
    public Map<String, Double> rowSum(String model) {
        Map<String, Double> TF = new HashMap<>();
        for (String system : tags) {
            double sum = 0.0;
            for (InfoNeed need : needs) {
                double score = tagEvaluatorMap.get(Tag.tag(system)).score(need, model);
                sum += score;
            }
            TF.put(model, sum);
        }

        return TF;
    }
    public Map<InfoNeed, Double> columnSum(String model) {
        Map<InfoNeed, Double> D = new HashMap<>();
        for (InfoNeed need : needs) {
            double sum = 0.0;
            for (String system : tags) {
                double score = tagEvaluatorMap.get(Tag.tag(system)).score(need, model);
                sum += score;
            }
            D.put(need, sum);
        }

        return D;
    }
    public double N(String model) {
        double sum = 0.0;
        for (InfoNeed need : needs) {
            for (String system : tags) {
                double score = tagEvaluatorMap.get(Tag.tag(system)).score(need, model);
                sum += score;
            }
        }

        return sum;
    }

    public Solution randomAsSolution(String model) {
        return randomAsSolution(this.needs, model);
    }
    public Solution randomAsSolution(List<InfoNeed> needs,String model) {

        List<Prediction> list = new ArrayList<>(needs.size());

        List<String> systemSet = new ArrayList<>(tags);


        boolean isUnix;
        Random r=null;
        DataInputStream is=null;
        try {
            is = new DataInputStream(Files.newInputStream(Paths.get("/dev/urandom")));
            isUnix=true;
        } catch (IOException ioe) {
            //System.err.println("Error while getting random integers from /dev/urandom. Random number is being generated vie Random class");
            isUnix=false;
            r = new Random();
        }

        for (InfoNeed testQuery : needs) {
            int randomIndex;
            if(isUnix) {
                try{ randomIndex = Math.abs(is.readInt()) % systemSet.size();
                }catch (IOException io){throw new RuntimeException("is.readInt() random integers from /dev/urandom", io);}
            }
            else randomIndex = Math.abs(r.nextInt()) % systemSet.size();
            String predictedSystem = systemSet.get(randomIndex);
            double predictedScore = tagEvaluatorMap.get(Tag.tag(predictedSystem)).score(testQuery, model);

            Prediction prediction = new Prediction(testQuery, predictedSystem, predictedScore);
            list.add(prediction);
        }

        Solution solution = new Solution(list, -1);
        calculateAccuracy(solution,model);

        solution.setKey("RND\t" + model);
        solution.model = model;

        return solution;
    }

    public Solution randomMLE(List<InfoNeed> needs,String model) {
        return randomMLE(needs, randomMLEMap(model), model);
    }
    public Map<String, Double> randomMLEMap(String model) {

        Map<String, Double> map = new HashMap<>();

        int residualNeedSize = needs.size() - modelAllSameMap.get(model).size() - modelAllZeroMap.get(model).size();

        for (String system : tags) {

            // a model may not exist in best model map, e.g. winner for zero queries
            if (!bestModelSystemMap.get(model).containsKey(system)) {
                map.put(system, 0.0);
                continue;
            }

            int count = bestModelSystemMap.get(model).get(system).size();

            map.put(system, (double) count / residualNeedSize);

        }

        return map;

    }

    public Solution randomMLE(List<InfoNeed> needs, Map<String, Double> map,String model) {

        List<String> systemSet = new ArrayList<>(tags);

        /**
         * How many times a system winner?
         * That much addition is done for the system
         */
        List<String> localSystems = new ArrayList<>(needs.size());

        for (String system : map.keySet()) {

            int count = Math.round(map.get(system).floatValue() * (float) needs.size());
            for (int i = 0; i < count; i++)
                localSystems.add(system);
        }

        List<Prediction> list = new ArrayList<>(needs.size());


        boolean isUnix;
        Random r=null;
        DataInputStream is=null;
        try {
            is = new DataInputStream(Files.newInputStream(Paths.get("/dev/urandom")));
            isUnix=true;
        } catch (IOException ioe) {
            //System.err.println("Error while getting random integers from /dev/urandom. Random number is being generated vie Random class");
            isUnix=false;
            r = new Random();
        }

        for (int i = localSystems.size(); i < needs.size(); i++) {
            int randomIndex;
            if (isUnix) {
                try {
                    randomIndex = Math.abs(is.readInt()) % systemSet.size();
                } catch (IOException io) {
                    throw new RuntimeException("is.readInt() random integers from /dev/urandom", io);
                }
            } else randomIndex = Math.abs(r.nextInt()) % systemSet.size();
            localSystems.add(systemSet.get(randomIndex));
        }


        Collections.shuffle(localSystems);
        for (InfoNeed testQuery : needs) {

            String predictedSystem = localSystems.remove(0);
            double predictedScore = tagEvaluatorMap.get(Tag.tag(predictedSystem)).score(testQuery, model);


            Prediction prediction = new Prediction(testQuery, predictedSystem, predictedScore);
            list.add(prediction);
        }


        Solution solution = new Solution(list, -1);
        calculateAccuracy(solution,model);

        solution.setKey("MLE\t" + model);
        solution.model = model;

        return solution;

    }

    public double randomXScore(String model) {
        return randomMLE(this.needs,model).getMean();
    }

    public SystemScore randomX(String model) {

        double[] means = new double[500];

        for (int i = 0; i < 500; i++) {
            means[i] = randomXScore(model);
        }

        return new SystemScore(String.format("RandomMLE (\u00B1%.2f)", Math.sqrt(StatUtils.variance(means))), StatUtils.mean(means));
    }
}
