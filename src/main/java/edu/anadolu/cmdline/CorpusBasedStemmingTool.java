package edu.anadolu.cmdline;

import edu.anadolu.QuerySelector;
import edu.anadolu.analysis.Analyzers;
import edu.anadolu.analysis.Tag;
import edu.anadolu.datasets.Collection;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.knn.Measure;
import edu.anadolu.qpp.PMI;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.spell.LuceneDictionary;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefIterator;
import org.clueweb09.InfoNeed;
import org.kohsuke.args4j.Option;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class CorpusBasedStemmingTool extends CmdLineTool{

    @Option(name = "-collection", required = true, usage = "Collection")
    private Collection collection;

    @Option(name = "-task",  metaVar = "[CBSGupta19|]", required = false)
    private String task="CBSGupta19|";

    @Option(name = "-avgTL",  metaVar = "[CBSGupta19|]", required = false, usage = "Average Term Length")
    private Double avgTL;


    @Override
    public String getShortDescription() {
        return "Generates equivalence class of query terms for stemming";
    }

    @Override
    public String getHelp() {
        return "Following properties must be defined in config.properties for " + CLI.CMD + " " + getName() + " paths.indexes tfd.home";
    }

    private PMI pmi;
    @Override
    public void run(Properties props) throws Exception {

        if (parseArguments(props) == -1) return;

        final String tfd_home = props.getProperty("tfd.home");

        if (tfd_home == null) {
            System.out.println(getHelp());
            return;
        }

        DataSet dataset = CollectionFactory.dataset(collection, tfd_home);
        this.pmi= new PMI(dataset.indexesPath().resolve(Tag.NoStem.toString()), "contents");

        if ("CBSGupta19".equals(task)) {
            Map<String, Set<String>> prefixVariants = prefixMap(dataset);


        }

    }

    private Map<String, Set<String>> prefixMap(DataSet dataSet) throws IOException {
        QuerySelector selector = new QuerySelector(dataSet, Tag.NoStem.toString());

        int prefixLength = ((avgTL == null) ? (int) Math.round(getAvgTermLength(dataSet,selector)) : (int) Math.round(avgTL));
        Map<String, Set<String>> prefixTermMap = new LinkedHashMap<>();


        for (InfoNeed need : selector.allQueries) {

            for (String t : need.getPartialQuery()) {
                if (t.length() < prefixLength) continue;

                String prefix = StringUtils.left(t, prefixLength);
                if (prefixTermMap.containsKey(prefix)) {
                    Set<String> variants = prefixTermMap.get(prefix);
                    variants.add(t);
                    prefixTermMap.put(prefix, variants);
                } else {
                    Set<String> variants = new TreeSet<>();
                    variants.add(t);
                    prefixTermMap.put(prefix, variants);
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
                        String t =spare.utf8ToString();
                        if (t.length() < prefixLength) continue;

                        String prefix = StringUtils.left(t, prefixLength);

                        if (prefixTermMap.containsKey(prefix)) {
                            Set<String> variants = prefixTermMap.get(prefix);
                            variants.add(t);
                            prefixTermMap.put(prefix, variants);
                        }
                    }
                }

            }
        }
        return prefixTermMap;
    }

    private double LexicalSim(String t1, String t2){
        int minL = (t1.length() <= t2.length() ? t1 : t2).length();
        int maxL = (t1.length() > t2.length() ? t1 : t2).length();

        int commonPrefixL = StringUtils.getCommonPrefix(t1,t2).length();
        double sum=0.0;
        for(int i=1; i<=commonPrefixL;i++) sum+=Math.pow(0.5,i)*(t1.charAt(i)==t2.charAt(i)?1:0);
        return ((double)commonPrefixL/(double)maxL)*sum;
    }

    private double coOccurrenceSim(String t1, String t2) throws IOException, ParseException {
        return pmi.coOccurrenceGupta19(t1,t2);
    }

    private double potentialSuffixPairSim(){
        return 0;
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
                    tokens++;
                    sum+= spare.utf8ToString().length();
                }
            }
        }
        System.out.println("sum: "+ sum +"\ttokens: " +tokens);
        return ((double)sum)/tokens;
    }

}
