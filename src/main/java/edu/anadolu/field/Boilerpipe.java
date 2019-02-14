package edu.anadolu.field;


import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.kohlschutter.boilerpipe.extractors.ArticleExtractor;
import com.kohlschutter.boilerpipe.extractors.DefaultExtractor;
import com.kohlschutter.boilerpipe.extractors.LargestContentExtractor;
import org.clueweb09.WarcRecord;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Helper class to extract HTML5 Semantic Elements
 * <a href="https://www.w3schools.com/html/html5_semantic_elements.asp">HTML5 Semantic Elements</a>
 */
public class Boilerpipe {

    /**
     * Indexes HTML5 Semantic Elements as tokens.
     *
     * @param wDoc WarcRecord
     * @return Lucene Document having semantic tags are
     */
    private String contents="";
    public  String articleExtractor(WarcRecord wDoc) {
        return articleExtractor(wDoc.content());
    }
    public  String articleExtractor(String HTML) {
        SimpleTimeLimiter sp = SimpleTimeLimiter.create(Executors.newSingleThreadExecutor());

        try {
            sp.runWithTimeout(() -> {
                try {
                    contents = new ArticleExtractor().getText(HTML);
                } catch (Exception ignored) {
                    contents="";
                }
            },15, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            contents="";
            System.err.println("Timeout exception skipping...");
        } catch (InterruptedException e) {
            contents="";
            System.err.println("InterruptedException skipping...");
        }catch (Exception e) {
            contents="";
            System.err.println("Exception skipping...");
        }

        return contents;

    }
    public  String LCExtractor(WarcRecord wDoc) {
        return LCExtractor(wDoc.content());
    }

    public  String LCExtractor(String HTML) {
        SimpleTimeLimiter sp = SimpleTimeLimiter.create(Executors.newSingleThreadExecutor());

        try {
            sp.runWithTimeout(() -> {
                try {
                    contents = new LargestContentExtractor().getText(HTML);
                } catch (Exception ignored) {
                    contents="";
                }
            },15, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            contents="";
            System.err.println("Timeout exception skipping...");
        } catch (InterruptedException e) {
            contents="";
            System.err.println("InterruptedException skipping...");
        }catch (Exception e) {
            contents="";
            System.err.println("Exception skipping...");
        }

        return contents;

    }



    public  String defaultExtractor(WarcRecord wDoc) {
        return defaultExtractor(wDoc.content());
    }

    public  String defaultExtractor(String HTML) {
        SimpleTimeLimiter sp = SimpleTimeLimiter.create(Executors.newSingleThreadExecutor());

        try {
            sp.runWithTimeout(() -> {
                try {
                    contents = new DefaultExtractor().getText(HTML);
                } catch (Exception ignored) {
                    contents="";
                }
            },15, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            contents="";
            System.err.println("Timeout exception skipping...");
        } catch (InterruptedException e) {
            contents="";
            System.err.println("InterruptedException skipping...");
        }catch (Exception e) {
            contents="";
            System.err.println("Exception skipping...");
        }

        return contents;

    }

}
