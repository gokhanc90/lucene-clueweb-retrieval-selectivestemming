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
import org.apache.commons.math3.stat.correlation.KendallsCorrelation;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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

    @Option(name = "-Ignite", required = false, usage = "Max RAM size of ignite in GB when task commonality")
    private long ram=1L;

    //  @Option(name = "-spam", required = false, usage = "manuel spam threshold", metaVar = "10 20 30 .. 90")
    // private int spam = 0;


    @Option(name = "-task",  metaVar = "[commonality|resultSet]", required = false,
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

        if ("riskGraph".equals(task)) {
            final Evaluator evaluator = new Evaluator(dataset, Tag.NoStem.toString(), measure, models, "evals", "OR");
            final Evaluator evaluatorS = new Evaluator(dataset, tag, measure, models, "evals", "OR");
            List<InfoNeed> needs = evaluator.getNeeds();

            for(String model:evaluator.getModelSet()) {
                double[] N = evaluator.scoreArray(model, needs);
                double[] S = evaluatorS.scoreArray(model, needs);
                System.out.println(model);
                System.out.println("NoStem\t"+tag);
                int counterSame=0;
                int counterN=0;
                int counterS=0;
                for(int i=0;i<N.length;i++){
                    if(N[i]>S[i]) counterN++;
                    else if(N[i]==S[i]) counterSame++;
                    else counterS++;
                    System.out.println(N[i]+"\t"+S[i]);

                }
                System.out.println("Number of Same:\t"+counterSame);
                System.out.println("Number of NoStem:\t"+counterN);
                System.out.println("Number of "+tag+":\t"+counterS);
            }
        }


        if ("resultSet".equals(task)) {
            final Evaluator evaluator = new Evaluator(dataset, Tag.NoStem.toString(), measure, models, "evals", "OR");
            final Evaluator evaluatorS = new Evaluator(dataset, tag, measure, models, "evals", "OR");
            Set<String> modelIntersection = new HashSet<>();
            modelIntersection.addAll(evaluator.getModelSet());
            modelIntersection.retainAll(evaluatorS.getModelSet());


            final ArrayList<Path> thePathsN = new ArrayList<>();
            final ArrayList<Path> thePaths = new ArrayList<>();

            for (final Track track : dataset.tracks())
                thePaths.add(Paths.get(dataset.collectionPath().toString(), "runs", tag, track.toString()));

            for (final Track track : dataset.tracks())
                thePathsN.add(Paths.get(dataset.collectionPath().toString(), "runs", Tag.NoStem.toString(), track.toString()));


           // List<Path> paths = Evaluator.discoverTextFiles(thePath, op + "_all.txt");

            //final Path path = Paths.get(dataset.collectionPath().toString(), "runs", tag);
            //final Path pathN = Paths.get(dataset.collectionPath().toString(), "runs", Tag.NoStem.toString());
            for(String similarity:modelIntersection) {
                System.out.println(similarity);
                System.out.println("qid\tTau_Cor\tintersection\tunion");
                TreeMap<Integer,ArrayList<Long>> runMap = new TreeMap<>(); //qid, relevantdocs without alpha character
                final String runTag = toString(similarity, QueryParser.Operator.OR, "contents",tag, 0);
                for(Path thePath:thePaths) {
                    Path runFile = thePath.resolve(runTag + ".txt");
                    for (String line : Files.readAllLines(runFile, StandardCharsets.US_ASCII)) {
                        String[] parts = line.split("\\s+");
                        Integer qid = Integer.parseInt(parts[0]);
                        String docid = parts[2];
                        String docidToNumber = docid.replaceAll("[a-zA-Z\\-]", "");
                        if (runMap.containsKey(qid)) {
                            ArrayList l = runMap.get(qid);
                            l.add(Long.parseLong(docidToNumber));
                            runMap.put(qid, l);
                        } else {
                            ArrayList l = new ArrayList<Integer>();
                            l.add(Long.parseLong(docidToNumber));
                            runMap.put(qid, l);
                        }

                    }
                }

                final String runTagN = toString(similarity, QueryParser.Operator.OR, "contents",Tag.NoStem.toString(), 0);
                TreeMap<Integer,ArrayList<Long>> runMapN = new TreeMap<>();
                for(Path thePath:thePathsN) {
                    Path runFileN = thePath.resolve(runTagN + ".txt");
                    for (String line : Files.readAllLines(runFileN, StandardCharsets.US_ASCII)) {
                        String[] parts = line.split("\\s+");
                        Integer qid = Integer.parseInt(parts[0]);
                        String docid = parts[2];
                        String docidToNumber = docid.replaceAll("[a-zA-Z\\-]", "");
                        if (runMapN.containsKey(qid)) {
                            ArrayList l = runMapN.get(qid);
                            l.add(Long.parseLong(docidToNumber));
                            runMapN.put(qid, l);
                        } else {
                            ArrayList l = new ArrayList<Integer>();
                            l.add(Long.parseLong(docidToNumber));
                            runMapN.put(qid, l);
                        }

                    }
                }

                if(!runMap.keySet().containsAll(runMapN.keySet()) || !runMapN.keySet().containsAll(runMap.keySet()))
                    throw new RuntimeException("Qid sets are different!");
                for(Integer key : runMap.keySet()){
                    ArrayList<Long> ls = runMap.get(key);
                    ArrayList<Long> ln = runMapN.get(key);
                    KendallsCorrelation corr = new KendallsCorrelation();
                    int min = ls.size();
                    if(ln.size()<min) min = ln.size();
                    double[] v1 = ln.stream().mapToDouble(i->i).limit(min).toArray();
                    double[] v2 = ls.stream().mapToDouble(i->i).limit(min).toArray();

                    double t = corr.correlation(v1,v2);

                    Set<Long> intersection = new HashSet<>(ln); // use the copy constructor
                    intersection.retainAll(ls);

                    Set<Long> union = new HashSet<>(ln); // use the copy constructor
                    union.addAll(ls);

                    System.out.printf("%d\t%.2f\t%d\t%d\t",key,t,intersection.size(),union.size());
                    System.out.println();

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
    public String toString(String similarity, QueryParser.Operator operator, String field,String tag, int part) {
        String p = part == 0 ? "all" : Integer.toString(part);
        return similarity.replaceAll(" ", "_") + "_" + field + "_" + tag + "_" + operator.toString() + "_" + p;
    }
}
