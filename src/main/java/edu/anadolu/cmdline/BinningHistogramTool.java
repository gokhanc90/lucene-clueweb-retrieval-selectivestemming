package edu.anadolu.cmdline;

import edu.anadolu.datasets.Collection;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.freq.FreqBinning;
import org.apache.lucene.index.*;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.kohsuke.args4j.Option;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Properties;

/**
 * Histogram of document frequencies of the terms in the indexes for ge given binning count
 */
final class BinningHistogramTool extends CmdLineTool {

    @Option(name = "-collection", required = true, usage = "Collection")
    protected Collection collection;

    @Option(name = "-task", required = false, usage = "task [binning]")
    private String task;

    @Option(name = "-field", required = false, usage = "Lucene field")
    private String field = "contents";

    @Option(name = "-numBin", metaVar = "[50|100|...|7500|10000]", required = false, usage = "Non-negative integer spam threshold")
    protected int numBin = 50;

    @Override
    public String getShortDescription() {
        return "Histogram of document frequencies of the terms in the indexes for ge given binning count";
    }

    @Override
    public String getHelp() {
        return "Following properties must be defined in config.properties for " + CLI.CMD + " " + getName() + " tfd.home";
    }

    @Override
    public void run(Properties props) throws Exception {

        if (parseArguments(props) == -1) return;

        String tfd_home = props.getProperty("tfd.home");
        if (tfd_home == null) {
            System.out.println("tfd.home is mandatory for optimize tool!");
            return;
        }

        DataSet dataset = CollectionFactory.dataset(collection, tfd_home);

        for (Path path : discoverIndexes(dataset)) {
            if ("binning".equals(task))
                binning(path);
        }
    }

    /**
     * Prints the number of unique terms in a Lucene index
     *
     * @param indexPath Lucene index
     * @throws IOException should not happen
     */
    private void binning(Path indexPath) throws IOException {

        System.out.println("Opening Lucene index directory '" + indexPath.toAbsolutePath() + "'...");

        try (final Directory dir = FSDirectory.open(indexPath);
             IndexReader reader = DirectoryReader.open(dir)) {

            IndexSearcher searcher = new IndexSearcher(reader);
            CollectionStatistics statistics = searcher.collectionStatistics(field);

            final Terms terms = MultiFields.getTerms(reader, field);
            if (terms == null) {
                System.out.println("MultiFields.getTerms returns null. Wrong field ? " + field);
                return;
            }

            FreqBinning binner = new FreqBinning(numBin,reader.maxDoc());
            int[] histogram = new int[numBin+1]; //0 index will be empty
            Arrays.fill(histogram,0);

            TermsEnum termsEnum = terms.iterator();
            BytesRef ref;
            if ((ref = termsEnum.next()) != null) {
                int df = termsEnum.docFreq();
                int binNumber = binner.calculateBinValue(df);
                histogram[binNumber] += 1;
            }

            System.out.println("Dataset : " + indexPath);
            System.out.println("The number of bins : " + numBin);
            System.out.println("The number of documents : " + statistics.docCount());
            System.out.println("The number of terms : " + statistics.sumTotalTermFreq());
            for(int i=1;i<histogram.length;i++)
                System.out.printf("%d\t%d",i,histogram[i]);


        } catch (IndexNotFoundException e) {
            System.out.println("IndexNotFound in " + indexPath.toAbsolutePath());
        }
    }
}
