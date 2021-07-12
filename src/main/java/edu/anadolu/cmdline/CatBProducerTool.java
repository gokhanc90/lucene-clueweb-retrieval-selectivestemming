package edu.anadolu.cmdline;

import edu.anadolu.Indexer;
import edu.anadolu.analysis.Tag;
import edu.anadolu.datasets.Collection;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Bits;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.StringArrayOptionHandler;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class CatBProducerTool extends CmdLineTool {

    @Option(name = "-collection", required = true, usage = "Collection")
    private Collection collection;

    @Option(name = "-DocNameIndex",  required = true, usage = "Enter the doc name column number in qrels")
    protected Integer k;


    @Option(name = "-files", handler =  StringArrayOptionHandler.class, required = true, usage = "qrels files")
    protected List<String> files;

    @Override
    public void run(Properties props) throws Exception {
        if (parseArguments(props) == -1) return;

        final String tfd_home = props.getProperty("tfd.home");

        if (tfd_home == null) {
            System.out.println(getHelp());
            return;
        }

        DataSet dataset = CollectionFactory.dataset(collection, tfd_home);
        TreeSet<String> ids = new TreeSet<>();
        for (final Path path : discoverIndexes(dataset)) {

            final String tag = path.getFileName().toString();

            if (!tag.equals(Tag.NoStem.toString())) continue;

            IndexReader reader = DirectoryReader.open(FSDirectory.open(path));
            Bits liveDocs = MultiFields.getLiveDocs(reader);
            for (int i=0; i<reader.maxDoc(); i++) {
                if (liveDocs != null && !liveDocs.get(i))
                    continue;

                org.apache.lucene.document.Document doc = reader.document(i);
                ids.add(doc.get(Indexer.FIELD_ID));
            }

            System.out.println("MaxDoc: "+reader.maxDoc() + " retrieved Id count: "+ids.size());
            reader.close();
        }

        for(String file : files) {
            List<String> lines = Files.readAllLines(Paths.get(file));
            List<String> newLines = lines.stream().filter(
                    l -> ids.contains(l.split("\\s+")[k].trim()))
                    .collect(Collectors.toList());

            Files.write(Paths.get(Paths.get(file).getParent().toString(), Paths.get(file).getFileName().toString() + ".catB"), newLines);

            System.out.println(lines.size() + " " + newLines.size());

        }

    }

    @Override
    public String getHelp() {
        return null;
    }

    @Override
    public String getShortDescription() {
        return null;
    }

}
