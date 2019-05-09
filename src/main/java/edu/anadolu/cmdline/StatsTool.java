package edu.anadolu.cmdline;

import edu.anadolu.QueryBank;
import edu.anadolu.analysis.Analyzers;
import edu.anadolu.analysis.Tag;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.stats.CorpusStatistics;
import edu.anadolu.stats.QueryStatistics;
import org.kohsuke.args4j.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * ClueWeb09 Collection Statistic Tool
 */
final class StatsTool extends CmdLineTool {

    @Option(name = "-tag", metaVar = "[KStem|KStemAnchor]", required = false, usage = "Index Tag")
    protected String tag = null;

    @Override
    public String getShortDescription() {
        return "ClueWeb09 Collection Statistic Tool";
    }

    @Override
    public String getHelp() {
        return "Following properties must be defined in config.properties for " + CLI.CMD + " " + getName() + " paths.stats paths.indexes one.term.queries freq.fields";
    }

    @Option(name = "-collection", required = true, usage = "Collection")
    private edu.anadolu.datasets.Collection collection;

    @Option(name = "-task", required = false, usage = "task to be executed")
    private String task;

    @Override
    public void run(Properties props) throws Exception {

        if (parseArguments(props) == -1) return;

        final String tfd_home = props.getProperty("tfd.home");

        if (tfd_home == null) {
            System.out.println(getHelp());
            return;
        }

        DataSet dataset = CollectionFactory.dataset(collection, tfd_home);

        final long start = System.nanoTime();

        final String[] fields = props.getProperty("freq.fields", "description,keywords,title,contents").split(",");

        final Path statsPath = dataset.collectionPath().resolve("stats");

        if (!Files.exists(statsPath))
            Files.createDirectories(statsPath);

        if ("query".equals(task)) {
            QueryStatistics queryStatistics = new QueryStatistics(dataset);
            queryStatistics.saveLaTexStats();
            queryStatistics.queryLengthHistogram();
            return;
        }

        for (Path indexPath : discoverIndexes(dataset)) {
            final String tag = indexPath.getFileName().toString();

            // search for a specific tag, skip the rest
            if (this.tag != null && !tag.equals(this.tag)) continue;

            try (CorpusStatistics statistics = new CorpusStatistics(indexPath, statsPath)) {
                statistics.saveFieldStats(fields);
                statistics.saveLaTexStats(fields);
                Tag t = Tag.tag(indexPath.getFileName().toString());
                System.out.println("StatsTool Tag: " + t.name());
                if (Tag.Script.equals(t) || Tag.KStemField.equals(t)) continue;
                for (String field : fields)
                    statistics.saveTermStatsForWords(field, new QueryBank(dataset).distinctTerms(Analyzers.analyzer(t)), field + "_term_stats.csv");

                for (String field : fields)
                    statistics.saveTermStatsForWords(field, StopWordTool.getStopWords(props), field + "_stop_stats.csv");
            }
        }

        System.out.println("Stats completed in " + execution(start));
    }
}