package edu.anadolu.exp;

import edu.anadolu.Indexer;
import edu.anadolu.analysis.Analyzers;
import edu.anadolu.analysis.Tag;
import edu.anadolu.ltr.Traverser;
import edu.anadolu.similarities.MetaTerm;
import org.apache.commons.compress.compressors.z.ZCompressorInputStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.ConcurrentMergeScheduler;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Enumeration;
import java.util.function.BiPredicate;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import static edu.anadolu.Indexer.BUFFER_SIZE;

public class WSJIndexer {

    public static int index(String dataDir, final String indexPath, Tag tag, boolean addHL) throws IOException {
        Path iPath = Paths.get(indexPath, tag.toString());
        Path dPath = Paths.get(dataDir, "TipsterVol_1","wsj");
        Path dPath2 = Paths.get(dataDir, "TipsterVol_2","wsj");

        Deque<Path> zFiles = discoverZFiles(dPath,".z");
        zFiles.addAll(discoverZFiles(dPath2,".z"));


        if (!Files.exists(iPath))
            Files.createDirectories(iPath);

        System.out.println("Indexing to directory '" + iPath.toAbsolutePath() + "'...");

        final Directory dir = FSDirectory.open(iPath);

        final IndexWriterConfig iwc = new IndexWriterConfig(Analyzers.analyzer(tag));

        iwc.setSimilarity(new MetaTerm());
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        iwc.setRAMBufferSizeMB(512.0);
        iwc.setUseCompoundFile(false);
        iwc.setMergeScheduler(new ConcurrentMergeScheduler());

        final IndexWriter writer = new IndexWriter(dir, iwc);
        long docC = 0;
        while (!zFiles.isEmpty()) {
            Path path =zFiles.remove();
            ZCompressorInputStream zIn = new ZCompressorInputStream(new DataInputStream(Files.newInputStream(path, StandardOpenOption.READ)));
            org.jsoup.nodes.Document doc = Jsoup.parse(zIn, "UTF8", "", Parser.xmlParser());
            Elements elements = doc.select("DOC");
            docC += elements.size();
            for (int i = 0; i < elements.size(); i++) {
                StringBuilder contents = new StringBuilder();
                if (addHL && elements.get(i).select("HL").size()>0)
                    contents.append(elements.get(i).select("HL").get(0).text()).append(" ");
                if (elements.get(i).select("LP").size()>0)
                    contents.append(elements.get(i).select("LP").get(0).text()).append(" ");
                contents.append(elements.get(i).select("TEXT").get(0).text());
                String id = elements.get(i).select("DOCNO").get(0).text();

                Document document = new Document();

                // document ID
                document.add(new StringField(Indexer.FIELD_ID, id, Field.Store.YES));

                // entire document
                if(contents.toString().trim().length() == 0  ) System.out.println(id);
                document.add(new Indexer.NoPositionsTextField(Indexer.FIELD_CONTENTS, contents.toString().trim()));

                try {
                    writer.addDocument(document);
                } catch (IOException e) {
                    System.err.println(id);
                }
            }


        }
        int numIndexed = writer.getDocStats().maxDoc;
        writer.commit();
        writer.close();
        System.out.println(docC);
        return numIndexed;
    }

    public static Deque<Path> discoverZFiles(Path p, final String suffix) {

        final Deque<Path> stack = new ArrayDeque<>();

        FileVisitor<Path> fv = new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {

                Path name = file.getFileName();
                if (name != null && name.toString().endsWith(suffix))
                    stack.add(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if ("OtherData".equals(dir.getFileName().toString())) {
                    System.out.printf("Skipping %s\n", dir);
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException ioe) {
                System.out.printf("Visiting failed for %s\n", file);
                return FileVisitResult.SKIP_SUBTREE;
            }
        };

        try {
            Files.walkFileTree(p,fv);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stack;
    }



}
