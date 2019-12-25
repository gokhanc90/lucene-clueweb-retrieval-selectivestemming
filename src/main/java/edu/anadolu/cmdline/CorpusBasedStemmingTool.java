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
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
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

    @Option(name = "-Ignite", required = false, usage = "Max RAM size of ignite in GB when task commonality")
    private long ram=30L;

    @Override
    public String getShortDescription() {
        return "Generates equivalence class of query terms for stemming";
    }

    @Override
    public String getHelp() {
        return "Following properties must be defined in config.properties for " + CLI.CMD + " " + getName() + " paths.indexes tfd.home";
    }

    public static final String[] suffixes = {"ete","ite","ote","ute","ate","acy","t"
    };


    private PMI pmi;
    private DataSet dataSet;
    int prefixL4suffix = 4;
    Map<String, LinkedHashSet<String>> dictionary = new LinkedHashMap<>();

    private Set<String> queryTerms = new TreeSet<>();

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
            if(maxPairCount==null) maxPairCount = getMaxPair() ;
            Map<String, LinkedHashSet<String>> prefixVariants = prefixMap();
            System.out.println("PrefixTermMap is constructed");
            loadVariantsSuffixes(dataSet,prefixVariants);
            System.out.println("Variants' Suffixes are constructed");
            morphologicalClassFormulation(prefixVariants);
        }

    }

    private void morphologicalClassFormulation(Map<String, LinkedHashSet<String>> prefixVariants) throws IOException, ParseException {
        PrintWriter out = new PrintWriter(Files.newBufferedWriter(Paths.get("SynonymGupta19"+collection+".txt"), StandardCharsets.US_ASCII));

        prefixVariants.entrySet().parallelStream().map(e-> {
            try {
                return graphWork(e.getValue());
            } catch (IOException ex) {
                ex.printStackTrace();
            } catch (ParseException ex) {
                ex.printStackTrace();
            }
            return "";
        }).filter(s->s.length()==0).forEach(s->{
            out.print(s);
            out.flush();
        });

        out.flush();
        out.close();
    }

    public String graphWork(LinkedHashSet<String> nodes)throws IOException, ParseException{
        IGenerator<List<String>> pairs = Generator.combination(nodes).simple(2);

        Iterator<List<String>> it = pairs.iterator();
        MutableValueGraph<String,Double> graph = ValueGraphBuilder.undirected().build();
        System.out.println("Initial graph is constructing");
        while(it.hasNext()){
            List<String> pair = it.next();
            String t1 = pair.get(0);
            String t2 = pair.get(1);
            double coOccurrence = coOccurrenceSim(t1,t2);
            double suffixSim = potentialSuffixPairSim(t1,t2);
            if (coOccurrence == 0 && suffixSim == 0) continue;
            double weight = coOccurrence + suffixSim + LexicalSim(t1,t2);
            graph.putEdgeValue(pair.get(0),pair.get(1),weight);
        }
        if(graph.nodes().size()==0) return "";
        System.out.println("Initial graph is constructed");
        //for(String n: graph.nodes()) System.out.println(graph.adjacentNodes(n));

        Set<String> sortedNodes = graph.nodes().stream().sorted((o1, o2) -> {
            if (graph.degree(o1) == graph.degree(o2) ) return 0;
            else if(graph.degree(o1) < graph.degree(o2)) return 1;
            return -1;
        }).collect(Collectors.toCollection(LinkedHashSet::new));

//            for (String q : Sets.intersection(queryTerms, graph.nodes())) {
//                if(graph.adjacentNodes(q).size()==0) continue;
//                List<Double> ed = graph.edges().stream().map(p -> graph.edgeValue(p.nodeU(), p.nodeV()).get()).collect(Collectors.toList());
//                double edges[] = ed.stream().mapToDouble(d -> d).toArray();
//                DescriptiveStatistics statistics = new DescriptiveStatistics(edges);
//                out.println("Max:\t" + statistics.getMax() + "\tMean:\t" + statistics.getMean() + "\tMin:\t" + statistics.getMin() + "\tStd\t" + statistics.getStandardDeviation());
//                out.flush();
//                System.out.println("Max:\t" + statistics.getMax() + "\tMean:\t" + statistics.getMean() + "\tMin:\t" + statistics.getMin() + "\tStd\t" + statistics.getStandardDeviation());
//
//            }

        System.out.println("Last graph is constructing...");
        for (String node : sortedNodes) {
            Set<String> adjacentNodes = graph.adjacentNodes(node);

            LinkedHashSet<String> sortedAd = adjacentNodes.stream().sorted((o1, o2) -> {
                if (graph.edgeValue(node, o1).get().equals(graph.edgeValue(node, o2).get())) return 0;
                else if (graph.edgeValue(node, o1).get() < graph.edgeValue(node, o2).get()) return 1;
                return -1;
            }).collect(Collectors.toCollection(LinkedHashSet::new));

            for (String adNode : sortedAd) {
                Set<String> u = new LinkedHashSet<>(graph.adjacentNodes(node));
                Set<String> v = new LinkedHashSet<>(graph.adjacentNodes(adNode));
                double association = Sets.intersection(u, v).size() / (double) (Sets.union(u, v).size());
                if (association < 0.2) graph.removeEdge(node, adNode);
            }
        }
        System.out.println("Last graph is writing...");
        StringBuilder br = new StringBuilder();
        for (String q : Sets.intersection(queryTerms, graph.nodes())) {
            if(graph.adjacentNodes(q).size()==0) continue;
            br.append(q + ", " + graph.adjacentNodes(q).toString().replaceAll("[\\[\\]]", ""));
            br.append(System.lineSeparator());

            //System.out.println(q + ", " + graph.adjacentNodes(q).toString().replaceAll("[\\[\\]]", ""));
        }
        System.out.println("Last graph is written");
        return br.toString();
    }

    private Map<String, LinkedHashSet<String>> prefixMap() throws IOException {
        QuerySelector selector = new QuerySelector(dataSet, Tag.NoStem.toString());

        int prefixLength = ((avgTL == null) ? (int) Math.round(getAvgQueryTermLength(selector)) : (int) Math.round(avgTL));
        return getPrefixTermMap(dataSet, selector, prefixLength);
    }

    private Map<String, LinkedHashSet<String>> getPrefixTermMap(DataSet dataSet, QuerySelector selector, int prefixLength) throws IOException {
        Map<String, LinkedHashSet<String>> map = new LinkedHashMap<>();


        for (InfoNeed need : selector.allQueries) {

            for (String t : need.getPartialQuery()) {
                if (t.length() < prefixLength) continue;
                queryTerms.add(t);
                String prefix = StringUtils.left(t, prefixLength);
                if (map.containsKey(prefix)) {
                    LinkedHashSet<String> variants = map.get(prefix);
                    variants.add(t);
                    map.put(prefix, variants);
                } else {
                    LinkedHashSet<String> variants = new LinkedHashSet<>();
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
                        LinkedHashSet<String> variants = map.get(prefix);
                        variants.add(t);
                        map.put(prefix, variants);
                    }
                }
            }


        }
        return map;
    }

    private double LexicalSim(String t1, String t2){
        String min=t1, max=t2;
        if (t1.length() > t2.length()) {
            min=t2;
            max=t1;
        }
        min = StringUtils.rightPad(min,max.length(),null);

        int commonPrefixL = StringUtils.getCommonPrefix(t1,t2).length();
        double sum=0.0;
        for(int i=0; i < max.length();i++) sum+=Math.pow(0.5,i+1)*(min.charAt(i)==max.charAt(i)?1:0);
        return ((double)commonPrefixL/(double)max.length())*sum;
    }

    private double coOccurrenceSim(String t1, String t2) throws IOException, ParseException {
        return pmi.coOccurrenceGupta19(t1,t2);
    }

    private double potentialSuffixPairSim(String t1, String t2) throws IOException {
        String s1=t1.substring(prefixL4suffix);
        String s2=t2.substring(prefixL4suffix);

        if (maxPairCount == null) maxPairCount = getMaxPair() ;
        int count = getOccurrenceOfPair( Pair.create(s1,s2));
        return (double) count/(double)maxPairCount;
    }


    private Long getMaxPair() throws IOException {


        OptionalLong max1 = Generator.combination(suffixes).simple(2).stream()
                .map(c -> new Pair<>(c.get(0), c.get(1))).collect(Collectors.toList()).parallelStream().mapToLong(p -> {
                    try {
                        return getSuffixPairOccurrenceFromIndex(p);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return 0;
                }).max();


        System.out.println("Max pair count: "+max1.getAsLong());
        return max1.getAsLong();
    }

    public int getSuffixPairOccurrenceFromIndex(Pair<String, String> p) throws IOException {
        HashMap<String, Set<String>> map = new HashMap<>();
        for (final Path indexPath : discoverIndexes(dataSet)) {
            String tagC = indexPath.getFileName().toString();
            if (tagC.equals(Tag.NoStem.toString())) {
                IndexReader reader = DirectoryReader.open(FSDirectory.open(indexPath));
                LuceneDictionary ld = new LuceneDictionary(reader, "contents");

                map.put(p.getFirst(), new LinkedHashSet<>(1000));
                map.put(p.getSecond(), new LinkedHashSet<>(1000));


                BytesRefIterator it = ld.getEntryIterator();
                BytesRef spare;
                while ((spare = it.next()) != null) {
                    String t = spare.utf8ToString();
                    if (t.length() < prefixL4suffix) continue;

                    String suffix = StringUtils.substring(t, prefixL4suffix);

                    if (map.containsKey(suffix)) {
                        Set<String> variants = map.get(suffix);
                        variants.add(t.substring(0, prefixL4suffix));
                        map.put(suffix, variants);
                    }
                }

                reader.close();
            }
        }
        return Sets.intersection(dictionary.get(p.getFirst()), dictionary.get(p.getSecond())).size();
    }

    public int getSuffixPairOccurrence(Pair<String, String> p) throws IOException {
        return Sets.intersection(dictionary.get(p.getFirst()), dictionary.get(p.getSecond())).size();
    }




    private int getOccurrenceOfPair(Pair<String, String> pair) throws IOException {
        int count = 0;
        int theta = 5;
        long inters = 0;
        inters = getSuffixPairOccurrence(pair);

        if(inters<theta) return 0;
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

    private double getAvgQueryTermLength(QuerySelector selector) throws IOException {
        long tokens=0;
        long sum=0;
        Set<String> distinctQueryTerms = new LinkedHashSet<>(1024);
        for (InfoNeed need : selector.allQueries) {
            for (String t : need.getPartialQuery()) {
                distinctQueryTerms.add(t);
            }
        }
        for(String t : distinctQueryTerms) {
            tokens++;
            sum += t.length();
        }
        System.out.println("sum: "+ sum +"\tDistinct tokens in query set: " +tokens+"\tavg: "+((double)sum)/tokens);
        return ((double)sum)/tokens;
    }

    public void loadVariantsSuffixes(DataSet dataSet, Map<String, LinkedHashSet<String>> prefixVariants) throws IOException {

        for (Map.Entry<String, LinkedHashSet<String>> e : prefixVariants.entrySet()) {
            for (String t : e.getValue()) {
                if (t.length() < prefixL4suffix) continue;
                String suffix = StringUtils.substring(t, prefixL4suffix);
                if (dictionary.containsKey(suffix)) {
                    LinkedHashSet<String> variants = dictionary.get(suffix);
                    variants.add(t);
                    dictionary.put(suffix, variants);
                } else {
                    LinkedHashSet<String> variants = new LinkedHashSet<>();
                    variants.add(t);
                    dictionary.put(suffix, variants);
                }
            }
        }

        for (final Path indexPath : discoverIndexes(dataSet)) {
            String tagC = indexPath.getFileName().toString();
            if (tagC.equals(Tag.NoStem.toString())) {
                IndexReader reader = DirectoryReader.open(FSDirectory.open(indexPath));
                LuceneDictionary ld = new LuceneDictionary(reader, "contents");
                BytesRefIterator it = ld.getEntryIterator();
                BytesRef spare;
                while ((spare = it.next()) != null) {
                    if(spare.utf8ToString().length()<prefixL4suffix) continue;
                    String suffix = StringUtils.substring(spare.utf8ToString(), prefixL4suffix);
                    if(!dictionary.containsKey(suffix)) continue;

                    LinkedHashSet<String> variants = dictionary.get(suffix);
                    variants.add(spare.utf8ToString());
                    dictionary.put(suffix, variants);

                }
                reader.close();
            }
        }
    }

}
