package edu.anadolu.cmdline;

import edu.anadolu.QuerySelector;
import edu.anadolu.analysis.Analyzers;
import edu.anadolu.analysis.Tag;
import edu.anadolu.datasets.Collection;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.eval.*;
import edu.anadolu.knn.Measure;
import edu.anadolu.qpp.PMI;
import edu.anadolu.stats.TermStats;
import org.apache.commons.lang3.StringUtils;
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
import org.apache.lucene.search.spell.LuceneDictionary;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefIterator;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.clueweb09.InfoNeed;
import org.clueweb09.tracks.Track;
import org.kohsuke.args4j.Option;
import ws.StemmerBuilder;
import ws.stemmer.Stemmer;

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
    protected String models = "BM25k1.2b0.75_DirichletLMc2500.0_LGDc1.0_PL2c1.0_DPH_DFRee_DFIC_DLH13";

    @Option(name = "-metric", required = false, usage = "Effectiveness measure; needed only eval task")
    protected Measure measure = Measure.NDCG20;

    @Option(name = "-collection", required = true, usage = "Collection")
    private Collection collection;

    @Option(name = "-Ignite", required = false, usage = "Max RAM size of ignite in GB when task commonality")
    private long ram=1L;

    //  @Option(name = "-spam", required = false, usage = "manuel spam threshold", metaVar = "10 20 30 .. 90")
    // private int spam = 0;
    @Option(name = "-catB", required = false, usage = "use catB qrels for CW12B and CW09B")
    private boolean catB = false;


    @Option(name = "-task",  metaVar = "[commonality|resultSet]", required = false,
            usage = "commonality: calculates commonality scores of query terms")
    private String task="commonality";

    @Option(name = "-tags", metaVar = "[NoStemTurkish_Zemberek|NoStem_KStem|NoStemTurkish_Zemberek_SnowballTr_F5Stem]", required = false, usage = "For printerWinner Task")
    protected String tags = "NoStem_SynonymSnowballEng_SynonymKStem_SynonymSnowballEngQBS_SynonymKStemQBS_SynonymHPS_SynonymGupta19";

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

        if("npmi".equals(task)){
            Path synonymPath=dataset.collectionPath();
            PMI pmi = new PMI(dataset.indexesPath().resolve(Tag.NoStem.toString()), "contents");
            QuerySelector querySelector = new QuerySelector(dataset, Tag.NoStem.toString());
            for (InfoNeed need : querySelector.allQueries) {

                String queryString = need.query();
                Map<Integer, List<String>> syn = Analyzers.getAnalyzedTokensWithSynonym(queryString, Analyzers.analyzer(Tag.tag(tag), synonymPath));
                List<String> orj = Analyzers.getAnalyzedTokens(queryString, Analyzers.analyzer(Tag.NoStem));

                int j = 0;
                for (Map.Entry<Integer, List<String>> e : syn.entrySet()) {
                    String orjinalTerm = orj.get(j++);
                    List<String> variants = e.getValue();
                    for(int k=0; k<variants.size(); k++){
                        String m = variants.get(k);
                        if(m.equals(orjinalTerm)) continue;
                        double npmi = pmi.npmi(orjinalTerm,m);
                        System.out.println(need.id()+"\t"+orjinalTerm+"\t"+m+"\t"+npmi);
                    }

                }


            }
        }

        if ("HPSTrain".equals(task)) {
            Path indexP=null;
            for (final Path indexPath : discoverIndexes(dataset)) {
                String tagC = indexPath.getFileName().toString();
                if (tagC.equals(tag)) indexP = indexPath;
            }

            File in = indexP.getParent().getParent().resolve("lexicon_" + tag + ".txt").toFile();
            if (in.exists() && !in.isDirectory()) {
                Stemmer stemmer = StemmerBuilder.train(in.getAbsolutePath());
                StemmerBuilder.save(stemmer, "HPS"+collection.toString()+tag+".bin");
            }else {
                printLexicon(dataset,Tag.tag(tag));
                Stemmer stemmer = StemmerBuilder.train(in.getAbsolutePath());
                StemmerBuilder.save(stemmer, "HPS"+collection.toString()+tag+".bin");
            }

        }

        if("printAvgCorpusTermLength".equals(task)){
            long tokens = 0;
            long sum = 0;
            for (final Path indexPath : discoverIndexes(dataset)) {
                String tagC = indexPath.getFileName().toString();
                if (tagC.equals(Tag.NoStem.toString())) {
                    IndexReader reader = DirectoryReader.open(FSDirectory.open(indexPath));
                    LuceneDictionary ld = new LuceneDictionary(reader, "contents");
                    BytesRefIterator it = ld.getEntryIterator();
                    BytesRef spare;

                    while ((spare = it.next()) != null) {
                        String t = spare.utf8ToString();
                        if (!StringUtils.isAlpha(t)) continue;
                        if (t.length() > 23) continue;
                        tokens++;
                        sum += t.length();
                    }
                    reader.close();
                }
            }
            System.out.println("sum: " + sum + "\ttokens: " + tokens + "\tavg: " + ((double) sum) / tokens);
        }

        if("printAvgQueryTermLength".equals(task)){
            QuerySelector querySelector = new QuerySelector(dataset, tag);
            long tokens = 0;
            long sum = 0;
            Set<String> distinctQueryTerms = new LinkedHashSet<>(1024);
            HashMap<String,Set<String>> trackQueryMap = new HashMap<>();
            for (InfoNeed need : querySelector.allQueries) {
                for (String t : Analyzers.getAnalyzedTokens(need.query(), querySelector.analyzer())) {
                    distinctQueryTerms.add(t);
                    if(trackQueryMap.get(need.getWT().toString())==null) trackQueryMap.put(need.getWT().toString(), new HashSet<>(Arrays.asList(t)));
                    else {
                        Set<String> s= trackQueryMap.get(need.getWT().toString());
                        s.add(t);
                        trackQueryMap.put(need.getWT().toString(),s);
                    }
                }
            }
            for (String t : distinctQueryTerms) {
                tokens++;
                sum += t.length();
            }
            for(Map.Entry<String,Set<String>> e: trackQueryMap.entrySet()) {
                System.out.println(e.getKey()+": "+e.getValue().stream().map(l -> l.length()).mapToInt(Integer::intValue).average().getAsDouble()+ " distincts: "+e.getValue().size());
            }
            System.out.println("sum: " + sum + "\tDistinct tokens in query set: " + tokens + "\tavg: " + ((double) sum) / tokens);

        }
        if("printWinner".equals(task)){
            Measure[] measures = {Measure.MAP,Measure.NDCG20,Measure.NDCG100};
            for(Measure measure : measures) {
                String op = "OR";
                String evalDirectory = "evals";

                final String[] tagsArr = tags.split("_");
                Set<String> modelIntersection = new HashSet<>();

                Map<Tag, Evaluator> evaluatorMap = new HashMap<>();

                for (int i = 0; i < tagsArr.length; i++) {
                    String tag = tagsArr[i];
                    final Evaluator evaluator = new Evaluator(dataset, tag, measure, models, evalDirectory, op);
                    evaluator.oracleMax();
                    evaluatorMap.put(Tag.tag(tag), evaluator);
                    //needs = evaluator.getNeeds();

                    if (i == 0)
                        modelIntersection.addAll(evaluator.getModelSet());
                    else
                        modelIntersection.retainAll(evaluator.getModelSet());
                }

                SystemEvaluator systemEvaluator = new SystemEvaluator(evaluatorMap);
                systemEvaluator.printBestCount();
            }
        }

        if("printRunTopicFromEvals".equals(task)){
            XSSFWorkbook workbook = new XSSFWorkbook();

            int[] k_values = {10,20,100};
            Metric[] metrics = {Metric.NDCG,Metric.ERR,Metric.MAP};
            for(Metric metric:metrics) {
                if (Metric.ERR.equals(metric) || Metric.NDCG.equals(metric)) {
                    for (int k : k_values) {

                        List<Path> paths = getPathList(dataset, tag, String.valueOf(k));
                        TreeMap<String, ArrayList<String>> model_scores = mergeTopicScores(dataset, metric, k, paths);

                        exportToExcelSheet(dataset, workbook, metric, k, model_scores);


                    }
                } else if ( Metric.P.equals(metric)) {
                    for (int k : k_values) {
                        List<Path> paths = getPathList(dataset, tag, "trec_eval");
                        TreeMap<String, ArrayList<String>> model_scores = mergeTopicScores(dataset, metric, k, paths);

                        exportToExcelSheet(dataset, workbook, metric, k, model_scores);

                    }
                }
                else if (Metric.MAP.equals(metric) ) {
                        List<Path> paths = getPathList(dataset, tag, "trec_eval");
                        TreeMap<String, ArrayList<String>> model_scores = mergeTopicScores(dataset, metric, 1000, paths);

                        exportToExcelSheet(dataset, workbook, metric, 1000, model_scores);


                }
            }

            FileOutputStream out = new FileOutputStream(new File(collection+"_RunTopicResults.xlsx"));
            workbook.write(out);
            out.close();

        }


        if("printSystemTopic".equals(task)){
            Measure[] measures = {Measure.MAP,Measure.NDCG20,Measure.NDCG100};
            for(Measure measure : measures) {
                String op = "OR";
                String evalDirectory = "evals";
                if (catB && (Collection.CW09B.equals(collection) || Collection.CW12B.equals(collection)))
                    evalDirectory = "catb_evals";
                System.out.println(measure);
                final String[] tagsArr = tags.split("_");
                Set<String> modelIntersection = new HashSet<>();

                Map<Tag, Evaluator> evaluatorMap = new HashMap<>();

                for (int i = 0; i < tagsArr.length; i++) {
                    String tag = tagsArr[i];
                    final Evaluator evaluator = new Evaluator(dataset, tag, measure, models, evalDirectory, op);
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
            }
        }

        if("printAnova".equals(task)){
            Measure[] measures = {Measure.MAP,Measure.NDCG20,Measure.NDCG100};
            for(Measure measure : measures) {
                String op = "OR";
                String evalDirectory = "evals";

                final String[] tagsArr = tags.split("_");
                Set<String> modelIntersection = new HashSet<>();

                Map<Tag, Evaluator> evaluatorMap = new HashMap<>();

                for (int i = 0; i < tagsArr.length; i++) {
                    String tag = tagsArr[i];
                    final Evaluator evaluator = new Evaluator(dataset, tag, measure, models, evalDirectory, op);
                    evaluator.oracleMax();
                    evaluatorMap.put(Tag.tag(tag), evaluator);
                    //needs = evaluator.getNeeds();

                    if (i == 0)
                        modelIntersection.addAll(evaluator.getModelSet());
                    else
                        modelIntersection.retainAll(evaluator.getModelSet());
                }

                SystemEvaluator systemEvaluator = new SystemEvaluator(evaluatorMap);
                systemEvaluator.printAnova(); //ANOVA not complete
            }
        }

        if("printScoreTable".equals(task)){
            Measure[] measures = {Measure.MAP,Measure.NDCG20,Measure.NDCG100};
            for(Measure measure : measures) {
                String op = "OR";
                String evalDirectory = "evals";

                final String[] tagsArr = tags.split("_");
                Set<String> modelIntersection = new HashSet<>();

                Map<Tag, Evaluator> evaluatorMap = new HashMap<>();

                for (int i = 0; i < tagsArr.length; i++) {
                    String tag = tagsArr[i];
                    System.out.println(dataset+" "+tag+" "+measure+" "+models+" "+evalDirectory+" "+op);
                    final Evaluator evaluator = new Evaluator(dataset, tag, measure, models, evalDirectory, op);
                    evaluator.oracleMax();
                    evaluatorMap.put(Tag.tag(tag), evaluator);
                    //needs = evaluator.getNeeds();

                    if (i == 0)
                        modelIntersection.addAll(evaluator.getModelSet());
                    else
                        modelIntersection.retainAll(evaluator.getModelSet());
                }

                SystemEvaluator systemEvaluator = new SystemEvaluator(evaluatorMap);
                systemEvaluator.printScoreTable();
            }
        }

        if ("printLexicon".equals(task)) {
            printLexicon(dataset,Tag.tag(tag));
        }


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

        if("ctiTable".equals(task)){
            final Evaluator evaluator = new Evaluator(dataset, Tag.NoStem.toString(), measure, models, "evals", "OR");

            QuerySelector querySelector = new QuerySelector(dataset, tag);
            Analyzer analyzer = Analyzers.analyzer(Tag.tag(tag));
            for (InfoNeed need : querySelector.allQueries) {
                StringBuilder br = new StringBuilder();
                double ctiSqrt=0.0;
                for (String t : need.getPartialQuery()) {
                    List<String> stems = Analyzers.getAnalyzedTokens(t, analyzer);
                    //if (stems.size() == 0) continue;
                    String keyTerm = stems.get(0);
                    TermStats termStats = querySelector.termStatisticsMap.get(keyTerm);
                    if (termStats == null) {
                        //System.out.println(tag+" index does not contain the term: "+ term);
                        termStats = new TermStats(t,0,0,0);//indexes do not contain query term
                        //throw new RuntimeException("Term stats cannot be null: "+ term );
                    }
                    ctiSqrt+= termStats.cti() * termStats.cti();
                    br.append(termStats.cti() + "\t");
                }
                System.out.println(need.id() + "\t" + need.query() + "\t" +  Math.sqrt(ctiSqrt) + "\t" + br.toString());
            }
            return;
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

        if ("commonalityFast".equals(task)) {
            Analyzer analyzer = Analyzers.analyzer(Tag.tag(tag));
            Map<String,Set<String>> stemVariants = new LinkedHashMap<>();
            System.out.println(dataset);
            QuerySelector selector = new QuerySelector(dataset, Tag.NoStem.toString());

            System.out.println("Query and term commonality");
            for (InfoNeed need : selector.allQueries) {
                for (String t : Analyzers.getAnalyzedTokens(need.query(),Analyzers.analyzer(Tag.NoStem))) {
                        List<String> stems = Analyzers.getAnalyzedTokens(t, analyzer);
                        if (stems.size() == 0) continue;
                        String stem = stems.get(0);

                        if (stemVariants.containsKey(stem)) {
                            Set<String> variants = stemVariants.get(stem);
                            variants.add(t);
                            stemVariants.put(stem, variants);
                        } else {
                            Set<String> variants = new TreeSet<>();
                            variants.add(t);
                            stemVariants.put(stem, variants);
                        }


                }
            }

            //All queries are added, now iterate collection
            for (final Path indexPath : discoverIndexes(dataset)) {
                String tagC = indexPath.getFileName().toString();
                if (tagC.equals(Tag.NoStem.toString())) {
                    IndexReader reader = DirectoryReader.open(FSDirectory.open(indexPath));
                    LuceneDictionary ld = new LuceneDictionary(reader, "contents");
                    BytesRefIterator it = ld.getEntryIterator();
                    BytesRef spare;

                    while ((spare = it.next()) != null) {
                        List<String> stems = Analyzers.getAnalyzedTokens(spare.utf8ToString(), analyzer);
                        if (stems.size() == 0) continue;
                        String stem = stems.get(0);

                        if (stemVariants.containsKey(stem)) {
                            Set<String> variants = stemVariants.get(stem);
                            variants.add(spare.utf8ToString());
                            stemVariants.put(stem, variants);
                        }
                    }
                }
            }
            PrintWriter writer = new PrintWriter("Synonym_"+collection.name()+"_"+tag+".txt");
            for(Map.Entry<String,Set<String>> e :stemVariants.entrySet()){
                if(e.getValue().size()==1) continue;
                StringBuilder br = new StringBuilder();
                String variantsPretty = e.getValue().stream().collect(Collectors.joining(",", "", ""));
                br.append(variantsPretty);
                System.out.println(br.toString());
                writer.println(br.toString());
            }



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
                    for (String t : need.getPartialQuery()) {
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

    private void exportToExcelSheet(DataSet dataset, XSSFWorkbook workbook, Metric metric, int k, TreeMap<String, ArrayList<String>> model_scores) {
        XSSFSheet sheet = workbook.createSheet(metric.toString() + k);
        int sheetRow = 0;
        XSSFRow row = sheet.createRow(sheetRow++);
        row.createCell(0).setCellValue("TID");
        for (int c = 0; c < dataset.getTopics().size(); c++)
            row.createCell(c+1).setCellValue(dataset.getTopics().get(c).id());

        for (Map.Entry<String, ArrayList<String>> e : model_scores.entrySet()) {
            row = sheet.createRow(sheetRow++);
            row.createCell(0).setCellValue(e.getKey());
            for (int c = 0; c < e.getValue().size(); c++)
                row.createCell(c+1).setCellValue(e.getValue().get(c));
        }
    }

    private TreeMap<String, ArrayList<String>> mergeTopicScores(DataSet dataset, Metric metric, int k, List<Path> paths) throws IOException {
        TreeMap<String, List<EvalTool>> model_TrackEvals = new TreeMap<>();
        for (Path p : paths) {
            EvalTool evalTool = toolFactory(p, dataset, metric, String.valueOf(k));
            if (model_TrackEvals.containsKey(p.getFileName().toString())) {
                List<EvalTool> l = model_TrackEvals.get(p.getFileName().toString());
                l.add(evalTool);
                model_TrackEvals.put(p.getFileName().toString(), l);
            } else {
                List<EvalTool> l = new ArrayList<>();
                l.add(evalTool);
                model_TrackEvals.put(p.getFileName().toString(), l);
            }
        }

        TreeMap<String, ArrayList<String>> model_topicScores = new TreeMap<>();
        for (Map.Entry<String, List<EvalTool>> e : model_TrackEvals.entrySet()) {
            //System.out.println(e.getKey());
            ArrayList<String> scores = new ArrayList<>();
            for (InfoNeed need : dataset.getTopics()) {
                String score;
                for (EvalTool tool : e.getValue()) {
                    try {
                        score = tool.getMetric(need, metric);
                        scores.add(score);
                        //System.out.println(need.id() + " : " + tool.getMetric(need, metric));
                    } catch (RuntimeException error) {

                    }
                }
            }

            model_topicScores.put(e.getKey(),scores);
        }
        return model_topicScores;
    }

    private List<Path> getPathList(DataSet dataset, String tag, String k) throws IOException {
        ArrayList<Path> paths = new ArrayList<>();
        for (Track t : dataset.tracks()) {
            Path thePath = Paths.get(dataset.collectionPath().toString(), "synonym_param_evals", tag, t.toString(), k);

            if (!Files.exists(thePath) || !Files.isDirectory(thePath) || !Files.isReadable(thePath))
                throw new IllegalArgumentException(thePath + " does not exist or is not a directory.");

            paths.addAll(Evaluator.discoverTextFiles(thePath, "OR" + "_all.txt")) ;

        }
        return paths;
    }

    private EvalTool toolFactory(Path path, DataSet dataSet,Metric metric, String k) throws IOException {
        if (edu.anadolu.datasets.Collection.MQ07.equals(dataSet.collection()))
            return new StatAP(path, Integer.valueOf(k));
        if (edu.anadolu.datasets.Collection.MQ08.equals(dataSet.collection()))
            return new StatAP(path, Integer.valueOf(k));
        if (edu.anadolu.datasets.Collection.MQ09.equals(dataSet.collection()))
            return new StatAP(path, Integer.valueOf(k));
        else if (Metric.ERR.equals(metric) || Metric.NDCG.equals(metric))
            return new GdEval(path);
        else if (Metric.MAP.equals(metric) || Metric.P.equals(metric))
            return new TrecEval(path, Integer.valueOf(k));
        else
            throw new AssertionError(this);
    }

    public void printLexicon(DataSet dataSet,Tag tag) throws Exception {
        for (final Path indexPath : discoverIndexes(dataSet)) {
            String tagC = indexPath.getFileName().toString();
            if (tagC.equals(tag.toString())) {
                File in = indexPath.getParent().getParent().resolve("lexicon_" + tag + ".txt").toFile();
                in.createNewFile();
                PrintWriter writer = new PrintWriter(in);

                IndexReader reader = DirectoryReader.open(FSDirectory.open(indexPath));
                LuceneDictionary ld = new LuceneDictionary(reader, "contents");
                BytesRefIterator it = ld.getEntryIterator();
                BytesRef spare;

                long count=0;
                while ((spare = it.next()) != null) {
                    if(!StringUtils.isAlphanumeric(spare.utf8ToString())) continue;
                    writer.println(spare.utf8ToString());
                    count++;
                }
                writer.flush();
                writer.close();

                System.out.println("lexiconSize:" + count);
                System.out.println("lexicon "+in.getName()+" is successfully created.");

            }
        }
        System.out.println("printLexicon is done.");
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
