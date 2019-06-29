package edu.anadolu.field;

import edu.anadolu.Boilerpipe;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;
import org.junit.Assert;
import org.junit.Test;

import static org.jsoup.Jsoup.parseBodyFragment;

public class MetaTest {

    private final Document jDoc;

    public MetaTest() {
        jDoc = Jsoup.parse(html);
    }

    private final String html = "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head>\n" +
            "  <title>Title of the document</title>\n" +
            "  <meta charset=\"UTF-8\">\n" +
            "  <meta name=\"description\" content=\"Free Web tutorials\">\n" +
            "  <meta name=\"keywords\" content=\"HTML,CSS,XML,JavaScript\">\n" +
            "  <meta name=\"author\" content=\"John Doe\">\n" +
            "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
            "  <meta name=\"   \" content=\"\">\n" +
            "  <meta name=\"test\">\n" +
            "</head>\n" +
            "<body>\n" +
            "\n" +
            "<nav>\n" +
            "  <a href=\"/html/\">HTML</a> |\n" +
            "  <a href=\"/css/\">CSS</a> |\n" +
            "  <a href=\"/js/\">JavaScript</a> |\n" +
            "  <a href=\"/jquery/\">jQuery</a>\n" +
            "</nav>\n"+
            "<p>All meta information goes in the head section...</p>\n" +
            "sdfds sdf dsf ds f dsf ds f dsf"+
            "\n" +
            "<aside>\n" +
            "  <h4>Epcot Center</h4>\n" +
            "  <p>The Epcot Center is a theme park in Disney World, Florida.</p>\n" +
            "</aside>\n"+
            "<section>\n" +
            "\n" +
            "  <h1>Forest elephants</h1> \n" +
            "\n" +
            "  <section>\n" +
            "<header>\n" +
            "    <h1>What Does WWF Do?</h1>\n" +
            "    <p>WWF's mission:</p>\n" +
            "  </header>"+
            "    <h1>Introduction</h1>\n" +
            "    <p>In this section, we discuss the lesser known forest elephants.</p>\n" +
            "  </section>\n" +
            "\n" +
            "  <section>\n" +
            "    <h1>Habitat</h1>\n" +
            "    <p>Forest elephants do not live in trees but among them.</p>\n" +
            "  </section>\n" +
            "\n" +
            "  <aside>\n" +
            "    <p>advertising block</p>\n" +
            "  </aside>\n" +
            "\n" +
            "</section>\n" +
            "\n" +
            "<footer>\n" +
            "  <p>(c) 2010 The Example company</p>\n" +
            "</footer>"+
            "</body>\n" +
            "</html>";

    @Test
    public void testJSoupRemove() {
        Document jDoc = Jsoup.parse(html);
        System.out.println(jDoc.text());
        Elements es = jDoc.select("footer,nav,aside");
        for(Element e:es)
           // System.out.println(e.text());
        es.remove();
        System.out.println(jDoc.text());
        System.out.println(new StringBuilder().toString());
    }

    @Test
    public void testJSoupGetTExt() {
        Document jDoc = Jsoup.parse(html);
        String stripped = jDoc.text();
        System.out.println(jDoc.title());

        String b = new Boilerpipe().articleExtractor(html);

        System.out.println(b);
    }

    @Test
    public void testMetaNames() {

        Assert.assertEquals("Title of the document All meta information goes in the head section...", jDoc.text());

        String metaNames = MetaTag.metaTagsWithNameAttribute(jDoc);

        Assert.assertEquals("description keywords author viewport test", metaNames);

        Document dirty = parseBodyFragment(html, "");
        Cleaner cleaner = new Cleaner(Whitelist.none());
        Document clean = cleaner.clean(dirty);

        String htmlStripped = clean.text();
        Assert.assertEquals("Title of the document All meta information goes in the head section...", htmlStripped);
    }

    @Test
    public void testKeywords() {
        String keywords = MetaTag.enrich2(jDoc, "keywords");
        Assert.assertEquals("HTML,CSS,XML,JavaScript", keywords);
    }

    @Test
    public void testDescription() {
        String description = MetaTag.enrich2(jDoc, "description");
        Assert.assertEquals("Free Web tutorials", description);
    }

    @Test
    public void testNullContent() {
        String test = MetaTag.enrich2(jDoc, "non");
        Assert.assertNull(test);
    }

    @Test
    public void testEmptyContent() {
        String test = MetaTag.enrich2(jDoc, "test");
        Assert.assertNotNull(test);
        Assert.assertTrue(test.isEmpty());
    }
}
