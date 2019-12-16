package edu.anadolu.cmdline;

import com.google.common.collect.Sets;
import com.google.common.graph.*;
import edu.anadolu.QuerySelector;
import edu.anadolu.analysis.Tag;
import edu.anadolu.datasets.Collection;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.qpp.PMI;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.util.Pair;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.spell.LuceneDictionary;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefIterator;
import org.clueweb09.InfoNeed;
import org.kohsuke.args4j.Option;
import org.paukov.combinatorics3.Generator;
import org.paukov.combinatorics3.IGenerator;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CorpusBasedStemmingTool extends CmdLineTool{

    @Option(name = "-collection", required = true, usage = "Collection")
    private Collection collection;

    @Option(name = "-task",  metaVar = "[CBSGupta19|]", required = false)
    private String task="CBSGupta19|";

    @Option(name = "-avgTL",  metaVar = "[CBSGupta19|]", required = false, usage = "Average Term Length")
    private Double avgTL;

    @Option(name = "-maxPair",  metaVar = "[CBSGupta19|]", required = false, usage = "Maximum number of the pair in Collection.")
    private Long maxPairCount;


    @Override
    public String getShortDescription() {
        return "Generates equivalence class of query terms for stemming";
    }

    @Override
    public String getHelp() {
        return "Following properties must be defined in config.properties for " + CLI.CMD + " " + getName() + " paths.indexes tfd.home";
    }

    private PMI pmi;
    private DataSet dataSet;

    private Map<String, Set<String>> suffixMap;
    @Override
    public void run(Properties props) throws Exception {

        if (parseArguments(props) == -1) return;

        final String tfd_home = props.getProperty("tfd.home");

        if (tfd_home == null) {
            System.out.println(getHelp());
            return;
        }

        dataSet = CollectionFactory.dataset(collection, tfd_home);
        this.pmi= new PMI(dataSet.indexesPath().resolve(Tag.NoStem.toString()), "contents");

        if ("CBSGupta19".equals(task)) {
            this.suffixMap=getMap(12);
            Map<String, Set<String>> prefixVariants = prefixMap();
            morphologicalClassFormulation(prefixVariants);

        }

    }

    private void morphologicalClassFormulation(Map<String, Set<String>> prefixVariants) throws IOException, ParseException {
        for( Map.Entry<String,Set<String>> e : prefixVariants.entrySet()){
            Set<String> nodes = e.getValue();
            IGenerator<List<String>> pairs = Generator.combination(nodes).simple(2);

            Iterator<List<String>> it = pairs.iterator();
            MutableValueGraph<String,Double> graph = ValueGraphBuilder.undirected().build();


            while(it.hasNext()){
                List<String> pair = it.next();
                String t1 = pair.get(0);
                String t2 = pair.get(1);
                double coOccurrence = coOccurrenceSim(t1,t2);
                double suffixSim = potentialSuffixPairSim(t1,t2);
                if (coOccurrence == 0 || suffixSim == 0) continue;
                double weight = coOccurrence + suffixSim + LexicalSim(t1,t2);
                graph.putEdgeValue(pair.get(0),pair.get(1),weight);
            }
            if(graph.nodes().size()==0) continue;

            for(String n: graph.nodes()) System.out.println(graph.adjacentNodes(n));

            Set<String> sortedNodes = graph.nodes().stream().sorted((o1, o2) -> {
                if (graph.degree(o1) == graph.degree(o2) ) return 0;
                else if(graph.degree(o1) < graph.degree(o2)) return -1;
                return 1;
            }).collect(Collectors.toCollection(LinkedHashSet::new));

            for(String node : sortedNodes){
                Set<String> adjacentNodes = graph.adjacentNodes(node);

                LinkedHashSet<String> sortedAd = adjacentNodes.stream().sorted((o1, o2) -> {
                    if (graph.edgeValue(node, o1).get().equals(graph.edgeValue(node, o2).get())) return 0;
                    else if (graph.edgeValue(node, o1).get() < graph.edgeValue(node, o2).get()) return -1;
                    return 1;
                }).collect(Collectors.toCollection(LinkedHashSet::new));

                for(String adNode : sortedAd){
                    Set<String> u = new LinkedHashSet<>(graph.adjacentNodes(node));
                    Set<String> v = new LinkedHashSet<>(graph.adjacentNodes(adNode));
                    double association = Sets.intersection(u,v).size()/(double)(Sets.union(u,v).size());
                    if(association<0.2) graph.removeEdge(node,adNode);
                }
            }

            for(String n: graph.nodes()) System.out.println(graph.adjacentNodes(n));

        }
    }

    private Map<String, Set<String>> prefixMap() throws IOException {
        QuerySelector selector = new QuerySelector(dataSet, Tag.NoStem.toString());

        int prefixLength = ((avgTL == null) ? (int) Math.round(getAvgTermLength(dataSet,selector)) : (int) Math.round(avgTL));
        return getPrefixTermMap(dataSet, selector, prefixLength);
    }

    private Map<String, Set<String>> getPrefixTermMap(DataSet dataSet, QuerySelector selector, int prefixLength) throws IOException {
        Map<String, Set<String>> map = new LinkedHashMap<>();


        for (InfoNeed need : selector.allQueries) {

            for (String t : need.getPartialQuery()) {
                if (t.length() < prefixLength) continue;

                String prefix = StringUtils.left(t, prefixLength);
                if (map.containsKey(prefix)) {
                    Set<String> variants = map.get(prefix);
                    variants.add(t);
                    map.put(prefix, variants);
                } else {
                    Set<String> variants = new TreeSet<>();
                    variants.add(t);
                    map.put(prefix, variants);
                }
            }
        }
        //All queries are added, now iterate collection
        for (final Path indexPath : discoverIndexes(dataSet)) {
            String tagC = indexPath.getFileName().toString();
            if (tagC.equals(Tag.NoStem.toString())) {
                IndexReader reader = DirectoryReader.open(FSDirectory.open(indexPath));
                LuceneDictionary ld = new LuceneDictionary(reader, "contents");
                BytesRefIterator it = ld.getEntryIterator();
                BytesRef spare;

                while ((spare = it.next()) != null) {
                    String t = spare.utf8ToString();
                    if (t.length() < prefixLength) continue;

                    String prefix = StringUtils.left(t, prefixLength);

                    if (map.containsKey(prefix)) {
                        Set<String> variants = map.get(prefix);
                        variants.add(t);
                        map.put(prefix, variants);
                    }
                }
            }


        }
        return map;
    }

    private double LexicalSim(String t1, String t2){
        int minL = (t1.length() <= t2.length() ? t1 : t2).length();
        int maxL = (t1.length() > t2.length() ? t1 : t2).length();

        int commonPrefixL = StringUtils.getCommonPrefix(t1,t2).length();
        double sum=0.0;
        for(int i=1; i<=minL;i++) sum+=Math.pow(0.5,i)*(t1.charAt(i)==t2.charAt(i)?1:0);
        return ((double)commonPrefixL/(double)maxL)*sum;
    }

    private double coOccurrenceSim(String t1, String t2) throws IOException, ParseException {
        return pmi.coOccurrenceGupta19(t1,t2);
    }

    private double potentialSuffixPairSim(String t1, String t2) throws IOException {
        int prefixLength = 4;
        String s1=t1.substring(prefixLength);
        String s2=t2.substring(prefixLength);

        if (maxPairCount == null) maxPairCount = getMaxOccurrenceOfPair(prefixLength) ;
        int count = getOccurrenceOfPair( Pair.create(s1,s2),prefixLength);
        return (double) count/(double)maxPairCount;
    }

    private Long getMaxOccurrenceOfPair(int prefixLength) throws IOException {

        HashMap<Pair<String, String>, Long> maxes = new HashMap<>();
        for(Map.Entry<String, Set<String>> e: suffixMap.entrySet()){
            Set<String> termsInClass = e.getValue();
            List<Pair<String,String>> pairs = Generator.combination(termsInClass).simple(2).stream()
                    .map(c->new Pair<>(c.get(0).substring(prefixLength),c.get(1).substring(prefixLength)))
                    .collect(Collectors.toList());
            Optional<Map.Entry<Pair<String, String>, Long>> max = pairs.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                    .entrySet()
                    .stream()
                    .max(Comparator.comparing(Map.Entry::getValue));

            if(maxes.containsKey(max.get())){
                Long sum=maxes.get(max.get())+max.get().getValue();
                maxes.put(max.get().getKey(),sum);
            }else maxes.put(max.get().getKey(),max.get().getValue());
        }
        Long count=maxes.entrySet().stream().max(Map.Entry.comparingByValue()).get().getValue();
        return count;
    }

    private Map<String, Set<String>> getMap(int prefixLength) throws IOException {
        Map<String, Set<String>> map = new LinkedHashMap<>();
        for (final Path indexPath : discoverIndexes(dataSet)) {
            String tagC = indexPath.getFileName().toString();
            if (tagC.equals(Tag.NoStem.toString())) {
                IndexReader reader = DirectoryReader.open(FSDirectory.open(indexPath));
                LuceneDictionary ld = new LuceneDictionary(reader, "contents");
                BytesRefIterator it = ld.getEntryIterator();
                BytesRef spare;

                while ((spare = it.next()) != null) {
                    String t = spare.utf8ToString();
                    if (t.length() < prefixLength) continue;

                    String prefix = StringUtils.left(t, prefixLength);

                    if (map.containsKey(prefix)) {
                        Set<String> variants = map.get(prefix);
                        variants.add(t);
                        map.put(prefix, variants);
                    }else {
                        Set<String> variants = new LinkedHashSet<>();
                        variants.add(t);
                        map.put(prefix,variants);
                    }
                }
                reader.close();
            }
        }
        return map;
    }

    private int getOccurrenceOfPair(Pair<String, String> pair,int prefixLength) throws IOException {
        int count = 0;
        for(Map.Entry<String, Set<String>> e: suffixMap.entrySet()){
            Set<String> filteredSet = e.getValue().stream().filter(s -> s.endsWith(pair.getFirst()) || s.endsWith(pair.getSecond())).collect(Collectors.toSet());

            List<Pair<String,String>> pairs = Generator.combination(filteredSet).simple(2).stream().map(c->new Pair<>(c.get(0),c.get(1))).collect(Collectors.toList());
            filteredSet=null;

            for(Pair<String,String> p: pairs){
                if(p.getFirst().length()<prefixLength+1 || p.getSecond().length()<prefixLength+1) continue;
                count++;

                }
            }

        return count;

    }

    private double getAvgTermLength(DataSet dataSet,QuerySelector selector) throws IOException {
        long tokens=0;
        long sum=0;
        for (final Path indexPath : discoverIndexes(dataSet)) {
            String tagC = indexPath.getFileName().toString();
            if (tagC.equals(Tag.NoStem.toString())) {
                IndexReader reader = DirectoryReader.open(FSDirectory.open(indexPath));
                LuceneDictionary ld = new LuceneDictionary(reader, "contents");
                BytesRefIterator it = ld.getEntryIterator();
                BytesRef spare;

                while ((spare = it.next()) != null) {
                    if(spare.utf8ToString().length()>23) continue;
                    tokens++;
                    sum+= spare.utf8ToString().length();
                }
                reader.close();
            }
        }
        System.out.println("sum: "+ sum +"\ttokens: " +tokens+"\tavg: "+((double)sum)/tokens);
        return ((double)sum)/tokens;
    }

}
