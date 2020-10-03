package edu.anadolu.nlp;

import edu.anadolu.cmdline.CLI;
import edu.anadolu.cmdline.CmdLineTool;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.eval.Evaluator;
import edu.anadolu.knn.Measure;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import org.clueweb09.InfoNeed;
import org.kohsuke.args4j.Option;

import java.util.*;
import java.util.stream.Collectors;

public class POSTaggerTool extends CmdLineTool {
    @Option(name = "-tag", metaVar = "[KStem|KStemAnchor]", required = false, usage = "Index Tag")
    protected String tag = "NoStem";

    /**
     * Terrier's default values
     */
    @Option(name = "-models", required = false, usage = "term-weighting models")
    protected String models = "BM25k1.2b0.75_DirichletLMc2500.0_LGDc1.0_PL2c1.0_DPH_DFRee_DFIC_DLH13";

    @Option(name = "-metric", required = false, usage = "Effectiveness measure")
    protected Measure measure = Measure.NDCG20;

    @Option(name = "-collection", required = true, usage = "Collection")
    protected edu.anadolu.datasets.Collection collection;


    @Option(name = "-task", required = false, usage = "task to be executed: search or eval")
    private String task;

    @Override
    public String getShortDescription() {
        return "Searcher Tool in which you supply values of the hyper parameters";
    }

    @Override
    public String getHelp() {
        return "Following properties must be defined in config.properties for " + CLI.CMD + " " + getName() + " paths.indexes tfd.home";
    }


    StanfordCoreNLP pipeline;

    public POSTaggerTool() {
        Properties props;
        props = new Properties();
        // set the list of annotators to run
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner,parse,depparse,coref");
        // build pipeline
        pipeline = new StanfordCoreNLP(props);
    }

    @Override
    public void run(Properties props) throws Exception {

        if (parseArguments(props) == -1) return;

        final String tfd_home = props.getProperty("tfd.home");

        if (tfd_home == null) {
            System.out.println(getHelp());
            return;
        }

        DataSet dataset = CollectionFactory.dataset(collection, tfd_home);
        final Evaluator evaluator = new Evaluator(dataset, tag, measure, models, "evals", "OR");
        LinkedList<String> POSTags = Arrays.stream(POSTag.values()).map(Enum::toString).collect(Collectors.toCollection(LinkedList::new));
        System.out.println("QID\tQuery\t"+String.join("\t",POSTags)+"\t"+"OrderedTagsToQuery");
        for(InfoNeed infoNeed : evaluator.getNeeds()){
            LinkedHashMap<String,Integer> POSTagCount = new LinkedHashMap<>();
            POSTags.forEach(p->POSTagCount.put(p,0));

            LinkedList<String> tags = new POSTaggerTool().getPOSTag(infoNeed.query());
            tags.forEach(t->POSTagCount.computeIfPresent(t,(k, v)->v+1));

            System.out.println(infoNeed.id()+"\t"+infoNeed.query()+"\t"+
                    POSTagCount.values().stream().map(i->Integer.toString(i)).collect(Collectors.joining("\t")) +"\t"+
                    String.join(" ", tags));
        }

    }

    public  LinkedList<String> getPOSTag(String text){
        CoreDocument document = new CoreDocument(text);
        pipeline.annotate(document);
        document.wrapAnnotations();
        System.out.println(document.sentences().size());
        List<List<String>> tagsOfSentence =  document.sentences().stream().map(CoreSentence::posTags).collect(Collectors.toList());
        LinkedList<String> tags =new LinkedList<String>();
        tagsOfSentence.forEach(tags::addAll);
        return tags;
    }

    public static void main(String[] args) {
        LinkedList<String> tags = new POSTaggerTool().getPOSTag("U.S. oil industry history");
        System.out.println(tags);

        tags = new POSTaggerTool().getPOSTag("www.eskisehir.edu.tr");
        System.out.println(tags);
    }
}
