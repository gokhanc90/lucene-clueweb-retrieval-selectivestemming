package edu.anadolu;

import com.google.common.util.concurrent.SimpleTimeLimiter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class JsoupParserWithTimeLimiter {
    private Document contents = null;

    public Document tryJsoupParse(String HTML){
        SimpleTimeLimiter sp = SimpleTimeLimiter.create(Executors.newSingleThreadExecutor());

        try {
            sp.runWithTimeout(() -> contents = Jsoup.parse(HTML),15, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            System.err.println("Timeout exception skipping...");
        } catch (InterruptedException e) {
            System.err.println("InterruptedException skipping...");
        }catch (Exception e) {
            System.err.println("Exception skipping...");
        }catch (Throwable e){
            System.err.println("An error...");
        }

        return contents;
    }
}
