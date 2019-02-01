package edu.anadolu.cmdline;

import edu.anadolu.QuerySelector;
import edu.anadolu.Searcher;
import edu.anadolu.analysis.Analyzers;
import edu.anadolu.analysis.Tag;
import edu.anadolu.datasets.Collection;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.eval.Evaluator;
import edu.anadolu.knn.Measure;
import edu.anadolu.similarities.DFIC;
import edu.anadolu.similarities.DFRee;
import edu.anadolu.similarities.DLH13;
import edu.anadolu.similarities.DPH;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.configuration.WALMode;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.similarities.ModelBase;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.search.spell.LuceneDictionary;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefIterator;
import org.clueweb09.InfoNeed;
import org.clueweb09.tracks.Track;
import org.kohsuke.args4j.Option;

import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class AdHocExpTool extends CmdLineTool {

    @Option(name = "-tag", metaVar = "[KStem|KStemAnchor]", required = false, usage = "Index Tag")
    protected String tag = null;

    /**
     * Terrier's default values
     */
    @Option(name = "-models", metaVar = "[all|...]", required = false, usage = "term-weighting models")
    protected String models = "BM25k1.2b0.75_DirichletLMc2500.0_LGDc1.0_PL2c1.0";

    @Option(name = "-metric", required = false, usage = "Effectiveness measure; needed only eval task")
    protected Measure measure = Measure.NDCG20;

    @Option(name = "-collection", required = true, usage = "Collection")
    private Collection collection;

    @Option(name = "-Ignite", required = false, usage = "Max RAM size of ignite in GB")
    private long ram=1L;

    //  @Option(name = "-spam", required = false, usage = "manuel spam threshold", metaVar = "10 20 30 .. 90")
    // private int spam = 0;


    @Option(name = "-task",  metaVar = "[commonality|...]", required = false,
            usage = "commonality: calculates commonality scores of query terms")
    private String task="commonality";

    @Override
    public String getShortDescription() {
        return "Includes several adhoc methods";
    }

    @Override
    public String getHelp() {
        return "Following properties must be defined in config.properties for " + CLI.CMD + " " + getName() + " paths.indexes tfd.home";
    }

    Ignite ignite=null;

    @Override
    public void run(Properties props) throws Exception {

        if (parseArguments(props) == -1) return;

        final String tfd_home = props.getProperty("tfd.home");

        if (tfd_home == null) {
            System.out.println(getHelp());
            return;
        }

        DataSet dataset = CollectionFactory.dataset(collection, tfd_home);

        final int numThreads = Integer.parseInt(props.getProperty("numThreads", "2"));

        if ("resultSet".equals(task)) {
            final Set<ModelBase> modelBaseSet = Arrays.stream(models.split("_"))
                    .map(ParamTool::string2model)
                    .collect(Collectors.toSet());

            modelBaseSet.add(new DFIC());
            modelBaseSet.add(new DPH());
            modelBaseSet.add(new DLH13());
            modelBaseSet.add(new DFRee());

            for (final Path indexPath : discoverIndexes(dataset)) {

                final String tag = indexPath.getFileName().toString();

                // search for a specific tag, skip the rest
                if (this.tag != null && !tag.equals(this.tag)) continue;
                for (final Track track : dataset.tracks()) {

                    final Path path = Paths.get(dataset.collectionPath().toString(), "runs", tag, track.toString());
                    for(Similarity similarity:modelBaseSet) {
                        final String runTag = toString(similarity, QueryParser.Operator.OR, "contents",tag, 0);
                        Path runFile = path.resolve(runTag + ".txt");
                        //Read this file
                    }
                }
            }
            return;
        }

        if ("commonality".equals(task)) {
            IgniteCache<String, LinkedList<String>> stemVariants=null;
            try {
                stemVariants = getEmptyMap();
                System.out.println("map is got");
                Set<String> lexicon = getLexicon(dataset, Tag.NoStem);
                System.out.println("lexiconSize= " + lexicon.size());
                Analyzer analyzer = Analyzers.analyzer(Tag.tag(tag));

                for (String term : lexicon) {
                    List<String> stems = Analyzers.getAnalyzedTokens(term, analyzer);
                    if (stems.size() == 0) continue;
                    String stem = stems.get(0);

                    if (stemVariants.containsKey(stem)) {
                        LinkedList<String> variants = stemVariants.get(stem);
                        variants.add(term);
                        stemVariants.put(stem, variants);
                    } else {
                        LinkedList<String> variants = new LinkedList<>();
                        variants.add(term);
                        stemVariants.put(stem, variants);
                    }

                }
                System.out.println("Stem_Variants map is created");
                lexicon.clear();
                QuerySelector selector = new QuerySelector(dataset, Tag.NoStem.toString());

                System.out.println("Query and term commonality");
                for (InfoNeed need : selector.allQueries) {
                    StringBuilder br = new StringBuilder();
                    for (String t : need.distinctSet) {
                        List<String> stems = Analyzers.getAnalyzedTokens(t, analyzer);
                        if (stems.size() == 0) continue;
                        String keyTerm = stems.get(0);
                        if (!stemVariants.containsKey(keyTerm)) {
                            br.append(0 + "\t");
                            continue;
                        }
                        int variantsSize = stemVariants.get(keyTerm).size();
                        br.append(variantsSize + "\t");
                    }
                    System.out.println(need.id() + "\t" + need.query() + "\t" + br.toString());
                }
                System.out.println("Terms and variants");
                System.out.println("Query terms size: "+selector.loadTermStatsMap().keySet().size());
                for (String t : selector.loadTermStatsMap().keySet()) {
                    StringBuilder br = new StringBuilder();
                    List<String> stems = Analyzers.getAnalyzedTokens(t, analyzer);
                    if (stems.size() == 0) continue;
                    String keyTerm = stems.get(0);
                    if (!stemVariants.containsKey(keyTerm)) {
                        br.append(keyTerm + "={}\t");
                        System.out.println(br.toString());
                        continue;
                    }
                    String variantsPretty = keyTerm + "=" + stemVariants.get(keyTerm).stream().collect(Collectors.joining(";", "{", "}"));
                    br.append(variantsPretty + "\t");
                    System.out.println(br.toString());
                }
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                if(stemVariants!=null)stemVariants.close();
                if(ignite!=null)ignite.close();
            }
        }

    }
    public Set<String> getLexicon(DataSet dataSet,Tag tag) throws Exception {
        for (final Path indexPath : discoverIndexes(dataSet)) {
            String tagC = indexPath.getFileName().toString();
            if (tagC.equals(tag.toString())) {
                File in = indexPath.getParent().getParent().resolve("lexicon_" + tag + ".dat").toFile();
                if(in.exists() && !in.isDirectory()) {
                    ObjectInputStream ois = new ObjectInputStream(new FileInputStream(in));
                    return (HashSet<String>) ois.readObject();
                }
                IndexReader reader = DirectoryReader.open(FSDirectory.open(indexPath));
                LuceneDictionary ld = new LuceneDictionary(reader, "contents");
                BytesRefIterator it = ld.getEntryIterator();
                BytesRef spare;

                HashSet<String> lexicon = new HashSet<>();
                while ((spare = it.next()) != null) {
                    lexicon.add(spare.utf8ToString());
                }
                //dump lexicon
                ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(in));
                oos.writeObject(lexicon);
                oos.close();
                System.out.println("lexiconSize:" + lexicon.size());
                System.out.println("lexicon "+in.getName()+" is successfully created.");
                return lexicon;
            }
        }
        return null;
        //throw new Exception(tag +" not found in dataset:"+dataSet.toString());
    }

    public IgniteCache<String, LinkedList<String>> getEmptyMap(){
        IgniteConfiguration cfg = new IgniteConfiguration();

        DataStorageConfiguration storageCfg = new DataStorageConfiguration();
        storageCfg.getDefaultDataRegionConfiguration().setMaxSize(ram * 1024 * 1024 * 1024);//20GB
        //storageCfg.getDefaultDataRegionConfiguration().setPersistenceEnabled(true); //use disk
        cfg.setDataStorageConfiguration(storageCfg);
        //cfg.setSystemThreadPoolSize(2);



        IgniteCache<String, LinkedList<String>> cache=null;
        try {
            System.out.println("Ignite is starting");
            ignite = Ignition.start(cfg);
            System.out.println("Ignite is started");
            //ignite.cluster().active(true);
            //System.out.println("Ignite cluster is activated");

            System.out.println("Caches: "+ignite.cacheNames());
            if(ignite.cacheNames().contains("stemVariants")) {
                System.out.println("Cache stemVariants is removing");
                ignite.destroyCache("stemVariants");
                System.out.println("Cache stemVariants is removed");
            }
            cache = ignite.createCache("stemVariants");
            System.out.println("Cache stemVariants is created");
            cache = cache.withExpiryPolicy(new CreatedExpiryPolicy(new Duration(TimeUnit.HOURS,3)));

        }catch (Exception e){
            e.printStackTrace();
            if(cache!=null)cache.close();
            if(ignite!=null)ignite.close();
        }
        return cache;
    }
    public String toString(Similarity similarity, QueryParser.Operator operator, String field,String tag, int part) {
        String p = part == 0 ? "all" : Integer.toString(part);
        return similarity.toString().replaceAll(" ", "_") + "_" + field + "_" + tag + "_" + operator.toString() + "_" + p;
    }
}
