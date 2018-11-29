package edu.anadolu.eval;


import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import edu.anadolu.analysis.Tag;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.knn.Measure;
import edu.anadolu.knn.Prediction;
import edu.anadolu.knn.Solution;
import edu.anadolu.qpp.Aggregate;
import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
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
    private Map<InfoNeed, Map<String,Double>> varianceMap = null;  //InfoNeed, DPH variance
    private Map<InfoNeed, Map<String,Double>> avgMap = null;  //InfoNeed, DPH avg
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
            Map<String,Double> modelAvgMap = new HashMap<>();
            for(String model: modelIntersection) {
                List<SystemScore> list = systemScoreList(need,model);
                modelSytemScoreMap.put(model,list);
                modelVarianceMap.put(model,variance(list));
                modelAvgMap.put(model,avg(list));
            }
            performanceMap.put(need, modelSytemScoreMap);
            varianceMap.put(need, modelVarianceMap);
        }

        this.performanceMap = Collections.unmodifiableMap(performanceMap);
        this.varianceMap = Collections.unmodifiableMap(varianceMap);
    }


    public static double avg(List<SystemScore> list) {

        SummaryStatistics summaryStatistics = new SummaryStatistics();

        for (SystemScore systemScore : list) {
            summaryStatistics.addValue(systemScore.score);
        }
        return summaryStatistics.getMean();
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

    public List<InfoNeed> getNeeds() {
        return Collections.unmodifiableList(new ArrayList<>(needs));
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
            System.out.println(tag);
            evaluator.printMeanWT(modelIntersection);
        }

    }
    public void printTopicSystemMatrix(){
        for(String model:modelIntersection) {
            System.out.println(model);

            StringBuilder header=new StringBuilder("\t");
            for(InfoNeed need:needs){
                header.append(need.id()+"\t");
            }
            System.out.println(header.toString());


            for(String tag: tags){
                StringBuilder system=new StringBuilder();
                system.append(tag + "\t");
                for(InfoNeed need:needs){
                    system.append(tagEvaluatorMap.get(Tag.tag(tag)).score(need,model)+"\t");
                }
                System.out.println(system.toString());
            }
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
            System.out.println(s.key + " max:\t" + s.getMean() + "\t" + s.hits0 + "\t" + s.hits1 + "\t" + s.hits2);
        }
    }
    public void printRandom(){
        for(String model:modelIntersection) {
            Solution s = randomAsSolution(needs, model);
            System.out.println(s.key + ":\t" + s.getMean() + "\t" + s.hits0 + "\t" + s.hits1 + "\t" + s.hits2);
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
            System.out.println(s.system + " : \t" + s.score);
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
                double avg = avgMap.get(p.testQuery).get(s.model);
                double cov =  new Aggregate.CoVar().aggregateWtihVariance(variance, avg);
                System.out.printf("%d\t%s\t%.4f\t%.4f\n", p.testQuery.id(), p.predictedModel, p.predictedScore,cov);
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

    public List<InfoNeed> excludeOneTermNeeds(List<InfoNeed> needs) {

        List<InfoNeed> excludedOTN = new ArrayList<>();
        for(InfoNeed n: needs){
            if(n.termCount()==1) continue;
            excludedOTN.add(n);
        }
        return excludedOTN;
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
            TF.put(system, sum);
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

        return new SystemScore(String.format("RandomMLE %s (\u00B1%.2f)",model, Math.sqrt(StatUtils.variance(means))), StatUtils.mean(means));
    }


    private List<InfoNeed> needsSortedByVariance(String model) {

        List<InfoNeed> needs = new ArrayList<>();
        needs.addAll(this.needs);
        needs.sort(Comparator.comparing(varianceMap::get,(o1,o2)->o1.get(model).compareTo(o2.get(model))));
        return needs;
    }

    public void printTopicModelSortedByVariance() {
        for(String model:modelIntersection) {

            List<InfoNeed> needs = needsSortedByVariance(model);
            System.out.println(model);
            for (InfoNeed need : needs) {
                System.out.println(sortedTopicModel(need,model));
            }
        }

    }

    public String sortedTopicModel(InfoNeed need,String model) {


        List<SystemScore> list = performanceMap.get(need).get(model);
        Collections.sort(list);

        double standardError = Math.sqrt(varianceMap.get(need).get(model) / list.size());

        StringBuilder builder = new StringBuilder();
        builder.append(need.id()).append("\t").append(String.format("s=%.4f", standardError)).append("\t");

        double best = list.get(0).score;
        double worst = list.get(list.size() - 1).score;

        for (SystemScore systemScoree : list) {

            if (systemScoree.score > (best - standardError))
                builder.append("+");

            if (systemScoree.score < (worst + standardError))
                builder.append("-");

            builder.append(systemScoree.toString()).append("\t");


            if ((systemScoree.score > (best - standardError * 2.0)) && (systemScoree.score < (worst + standardError * 2.0)))
                System.err.println(systemScoree.toString() + " is both winner and loser for the need :" + need.toString());

        }

        return builder.toString();
    }
    public void printCountMap(){
        for(String model:modelIntersection) {
            System.out.println(model);
            Map<String, List<InfoNeed>> bestSystemMap = absoluteBestSystemMap(model);
            Map<String, Double> riskMap = riskSOTA(model);
            Map<String, Double> ctiMap = cti(model);
            Map<String, Double> zRiskMap = zRisk(model);
            Map<String, Double> geoRiskMap = geoRisk(model);
            System.out.println("System\tbestCount\tsotaRisk\tCTI\tzRisk\tgeoRisk");

            for (Map.Entry<String, List<InfoNeed>> entry : bestSystemMap.entrySet()) {
                System.out.print(entry.getKey() + "\t" + entry.getValue().size());

                if (riskMap.containsKey(entry.getKey())) {
                    System.out.print("\t" + String.format("%.4f", riskMap.get(entry.getKey())));
                } else System.out.print("\t---");

                if (ctiMap.containsKey(entry.getKey())) {
                    System.out.print("\t" + String.format("%.4f", ctiMap.get(entry.getKey())));
                } else System.out.print("\t---");

                if (zRiskMap.containsKey(entry.getKey())) {
                    System.out.print("\t" + String.format("%.4f", zRiskMap.get(entry.getKey())));
                } else System.out.print("\t---");

                if (geoRiskMap.containsKey(entry.getKey())) {
                    System.out.print("\t" + String.format("%.4f", geoRiskMap.get(entry.getKey())));
                } else System.out.print("\t---");

                System.out.println();

            }

            for (Map.Entry<String, List<InfoNeed>> entry : bestSystemMap.entrySet())
                System.out.println(entry.getKey() + "\t" + entry.getValue());
        }
    }
    public Map<String, List<InfoNeed>> absoluteBestSystemMap(String model) {

        Map<String, List<InfoNeed>> map = new HashMap<>();

        for (InfoNeed need : needs) {

            if (modelAllZeroMap.get(model).contains(need) || modelAllSameMap.get(model).contains(need))
                continue;

            Set<String> winners = multiLabelWinners(need, 1.0,model);

            if (winners.size() > 1) continue;

            String bestSim = Evaluator.onlyItem(winners);

            Evaluator.addSingleItem2Map(map, bestSim, need);

        }

        map.put("ALL_SAME", bestModelSystemMap.get(model).get("ALL_SAME") == null ? Collections.emptyList() : bestModelSystemMap.get(model).get("ALL_SAME"));
        map.put("ALL_ZERO", bestModelSystemMap.get(model).get("ALL_ZERO") == null ? Collections.emptyList() : bestModelSystemMap.get(model).get("ALL_ZERO"));

        tags.stream().filter(system -> !map.containsKey(system)).forEach(system -> map.put(system, Collections.emptyList()));
        return map;
    }

    public Map<String, Double> riskSOTA(String model) {

        Map<String, Double> riskMap = new HashMap<>();

        Map<InfoNeed, SystemScore> oracleMap = oracleMap(model);

        for (String system : tags) {
            double avg = 0.0;

            for (InfoNeed need : needs) {
                avg +=tagEvaluatorMap.get(Tag.tag(system)).score(need, model) - oracleMap.get(need).score;
            }

            avg /= needs.size();
            // System.out.println(model + " " + avg);

            riskMap.put(system, avg);
        }

        return riskMap;
    }
    public Map<String, Double> cti(String model) {

        final Map<String, Double> rowSum = rowSum(model);
        final Map<InfoNeed, Double> columnSum = columnSum(model);

        final double N = N(model);

        Map<String, Double> ctiMap = new HashMap<>();

        for (String system : tags) {

            double cti = 0.0;

            for (InfoNeed need : needs) {

                final double e = rowSum.get(system) * columnSum.get(need) / N;

                if (e == 0.0) continue;

                cti += Math.pow((tagEvaluatorMap.get(Tag.tag(system)).score(need, model) - e), 2) / e;
            }

            ctiMap.put(system, cti);
        }


        return ctiMap;

    }
    private Map<InfoNeed, SystemScore> oracleMap(String model) {

        Map<InfoNeed, SystemScore> map = new HashMap<>();

        for (InfoNeed need : needs) {

            double bestScore = Double.NEGATIVE_INFINITY;
            String bestTag = null;

            for (String tag : tags) {
                double score = tagEvaluatorMap.get(Tag.tag(tag)).score(need, model);
                if (score > bestScore) {
                    bestScore = score;
                    bestTag = tag;
                }
            }


            map.put(need, new SystemScore(bestTag, bestScore));
        }


        return map;
    }
}
