package edu.anadolu;


import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.kohlschutter.boilerpipe.BoilerpipeProcessingException;
import com.kohlschutter.boilerpipe.extractors.ArticleExtractor;
import com.kohlschutter.boilerpipe.extractors.DefaultExtractor;
import com.kohlschutter.boilerpipe.extractors.LargestContentExtractor;
import org.clueweb09.WarcRecord;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static edu.anadolu.field.SemanticElements.docType;

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
            },20, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            contents="";
            System.err.println("Timeout exception skipping...");
        } catch (InterruptedException e) {
            contents="";
            System.err.println("InterruptedException skipping...");
        }catch (Exception e) {
            contents="";
            System.err.println("Exception skipping...");
        }catch (Throwable e){
            System.err.println("An error skipping...");
            contents="";
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
        }catch (Throwable e){
            System.err.println("An error...");
            contents="";
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
        }catch (Throwable e){
            System.err.println("An error...");
            contents="";
        }

        return contents;

    }


//    public String CustomBoilerPipe(WarcRecord warcRecord){
//        String content=getContent(warcRecord,false);
//        if(content.equals(""))
//            content= articleExtractor(warcRecord);
//        return content;
//    }


    public String getContent(Document jDoc, boolean remove){
        String content = "";

        try {

            if(!docType(jDoc).equals("html5")) return content;

            if(remove){
                Elements es = jDoc.select("footer,nav,aside");
                es.remove();
                content=jDoc.text();
            }else{
                Elements elements = jDoc.select("section,article");
                if(elements.size()==0) return "";
                content = getSections(jDoc.children());
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            return "";
        }

        return content;
    }


    private String getSections(Elements elements){

        StringBuilder builder = new StringBuilder();
        for(Element e : elements){
            if(e.tagName().equals("section") || e.tagName().equals("article")){
                builder.append(e.text() + " ");
                continue;
            }

            builder.append(getSections(e.children()));
        }

        return builder.toString();

    }

    public String CustomBoilerPipeAndJsoup(WarcRecord warcRecord, Document jDoc, boolean remove) {
        String content=getContent(jDoc, remove);
        if(content.equals(""))
            content= articleExtractor(warcRecord);
        if(content.equals(""))
            content=jDoc.text();
        return content;
    }


}
