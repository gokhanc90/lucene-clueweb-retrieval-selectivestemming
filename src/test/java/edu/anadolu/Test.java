package edu.anadolu;


import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;
import edu.anadolu.analysis.Analyzers;
import edu.anadolu.analysis.Tag;
import edu.anadolu.cmdline.CLI;
import edu.anadolu.cmdline.SpamTool;
import edu.anadolu.datasets.Collection;
import edu.anadolu.datasets.CollectionFactory;
import edu.anadolu.datasets.DataSet;
import edu.anadolu.eval.Evaluator;
import edu.anadolu.eval.SystemEvaluator;
import edu.anadolu.freq.FreqBinning;
import edu.anadolu.knn.Measure;
import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.stat.inference.ChiSquareTest;
import org.apache.commons.math3.util.Pair;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.clueweb09.ClueWeb12WarcRecord;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.zip.GZIPInputStream;

import static edu.anadolu.Indexer.BUFFER_SIZE;
import static java.util.concurrent.TimeUnit.SECONDS;


public class Test {
    @org.junit.Test
    public void math() throws IOException {
        System.out.println(Math.ceil(16*6.0/100));
        System.out.println(Math.ceil(17*6.0/100));
        System.out.println(Math.ceil(33*6.0/100));
        System.out.println(Math.ceil(34*6.0/100));
        System.out.println(Math.ceil(90*6.0/100));
        System.out.println(Math.ceil(83*6.0/100));
        System.out.println(Math.ceil(84*6.0/100));
        System.out.println(Math.ceil(100*6.0/100));
        FreqBinning freqBinning = new FreqBinning(6,100);
        System.out.println(freqBinning.calculateBinValue(16));
        System.out.println(freqBinning.calculateBinValue(17));
        System.out.println(freqBinning.calculateBinValue(33));
        System.out.println(freqBinning.calculateBinValue(34));
        System.out.println(freqBinning.calculateBinValue(90));
        System.out.println(freqBinning.calculateBinValue(83));
        System.out.println(freqBinning.calculateBinValue(84));
        System.out.println(freqBinning.calculateBinValue(100));

    }

    private static final Runnable GOOD_RUNNABLE =
            new Runnable() {
                @Override
                public void run() {
                    try {
                        SECONDS.sleep(1);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            };
    @org.junit.Test
    public void testTimeLimiter() throws IOException {
        ExecutorService executor = Executors.newFixedThreadPool(3);
        TimeLimiter service= SimpleTimeLimiter.create(executor);
        try {
            for(int i=0;i<3;i++)
                service.runWithTimeout(new Thread(GOOD_RUNNABLE), 3, SECONDS);
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    @org.junit.Test
    public void testWarcFile() throws IOException {
        int i = 0;
        Path path40 = Paths.get("0200wb-38.warc.gz");

        try (DataInputStream inStream = new DataInputStream(new GZIPInputStream(Files.newInputStream(path40, StandardOpenOption.READ), BUFFER_SIZE))) {
            // iterate through our stream
            ClueWeb12WarcRecord wDoc;
            while ((wDoc = ClueWeb12WarcRecord.readNextWarcRecord(inStream)) != null) {

                System.out.println(wDoc.id());
                if("clueweb12-0200wb-38-08218".equals(wDoc.id()))
                {
                    //System.out.println(wDoc.content());
                    BufferedWriter wr = new BufferedWriter(new FileWriter(wDoc.id()));
                    wr.write(wDoc.content());
                    wr.close();
                    System.out.println("----------");
                }
                Document jDoc = Jsoup.parse(wDoc.content());
                if(jDoc==null){
                    System.err.println("jdoc exception " + wDoc.id());
                    continue;
                }

                if(wDoc.id()==null){
                    String type = wDoc.type();
                    String content=new Boilerpipe().CustomBoilerPipeAndJsoup(wDoc,jDoc,false);
                    //System.out.println(content);
                }
                else if(wDoc.id().equals("clueweb12-1008wb-40-24989") || wDoc.id().equals("clueweb12-0009wb-11-30310") || wDoc.id().equals("clueweb12-1008wb-46-11212")
                        || wDoc.id().equals("clueweb12-1008wb-46-11227")|| wDoc.id().equals("clueweb12-1008wb-52-13619")){

                }else
                    new Boilerpipe().CustomBoilerPipeAndJsoup(wDoc,jDoc,false);
            }
        }
    }

    @org.junit.Test
    public void testBoilerpipe(){
        String HTML2="";

        String HTML ="<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">\n" +
                "<html><head>\n" +
                "<title>Working in Switzerland - Work disputes - Strikes - AngloINFO Geneva, in the Geneva region (Switzerland)</title>\n" +
                "<META name=\"title\" content=\"Working in Switzerland - Work disputes - Strikes - AngloINFO Geneva, in the Geneva region\">\n" +
                "<META name=\"description\" content=\"Comprehensive, independent, accurate and up-to-date information on Working in Switzerland - Work disputes - Strikes, from the definitive English-language guide to Geneva and the nearby regions of Switzerland and France.\"><meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\"><link rel=\"meta\" href=\"http://geneva.angloinfo.com/labels.xml\" type=\"application/rdf+xml\" title=\"ICRA labels\"><meta http-equiv=\"pics-Label\" content='(pics-1.1 \"http://www.icra.org/pics/vocabularyv03/\" l gen true for \"http://geneva.angloinfo.com\" r (n 0 s 0 v 0 l 0 oa 0 ob 0 oc 0 od 0 oe 0 of 0 og 0 oh 0 c 1) gen true for \"http://geneva.angloinfo.com\" r (n 0 s 0 v 0 l 0 oa 0 ob 0 oc 0 od 0 oe 0 of 0 og 0 oh 0 c 1))'><META name=\"robots\" content=\"index, follow\"><META name=\"Copyright\" content=\"Copyright ɠAngloINFO Limited, All Rights Reserved\"><META name=\"DC.title\" content=\"Working in Switzerland - Work disputes - Strikes - AngloINFO Geneva, in the Geneva region, in the Geneva region, Switzerland\"><META name=\"DC.format\" content=\"text/html\"><meta name=\"DC.publisher\" content=\"AngloINFO Limited\"><meta name=\"DC.rights\" content=\"http://geneva.angloinfo.com/central/copyright.asp\"><meta name=\"DC.language\" content=\"en\">\n" +
                "<meta name=\"SKYPE_TOOLBAR\" content=\"SKYPE_TOOLBAR_PARSER_COMPATIBLE\">\n" +
                "<meta name=\"viewport\" content=\"width=971\">\n" +
                "<meta name=\"DC.coverage\" content=\"Geneva, Switzerland\"><link rel=\"start\" href=\"/\"><link rel=\"contents\" href=\"/information.asp\"><link rel=\"copyright\" href=\"/central/copyright.asp\"><link rel=\"shortcut icon\" href=\"/favicon.ico\"><link rel=\"stylesheet\" type=\"text/css\" href=\"/_include/stylesheet.asp\">\n" +
                "<link rel=\"image_src\" href=\"http://angloinfo.com/images/aifbthumb.jpg\">\n" +
                "\n" +
                "<script type='text/javascript'><!--// <![CDATA[\n" +
                "var OA_zones = {\n" +
                "'zone_1' : 1,\n" +
                "'zone_3' : 3,\n" +
                "'zone_5' : 5,\n" +
                "'zone_23' : 23\n" +
                "}\n" +
                "// ]]> --></script>\n" +
                "<script type=\"text/javascript\" src=\"http://mediax.angloinfo.com/www/delivery/spcjs.php?id=1&amp;cs=geneva&amp;cc=Switzerland&amp;cg=0&amp;cz=\"></script>\n" +
                "\n" +
                "<script type=\"text/javascript\"><!--\n" +
                "function openCentralWindow(url) { \n" +
                "\t\tpopupWin = window.open(url,'central_page','width=640,height=555,scrollbars=yes') \n" +
                "} \n" +
                "function toggleon(obj) {\n" +
                "\t\tvar el = document.getElementById(obj);\n" +
                "\t\tel.style.display = '';\n" +
                "\t\t}\n" +
                "function toggleoff(obj) {\n" +
                "\t\tvar el = document.getElementById(obj);\n" +
                "\t\tel.style.display = 'none';\n" +
                "\t\t}\n" +
                "//--> </script>\n" +
                "<script type=\"text/javascript\">\n" +
                "var _gaq = _gaq || [];\n" +
                "_gaq.push(['_setAccount', 'UA-291793-1']);\n" +
                "_gaq.push(['_setDomainName', '.angloinfo.com']);\n" +
                "_gaq.push(['_trackPageview']);\n" +
                "(function() {\n" +
                "var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;\n" +
                "ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';\n" +
                "var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);\n" +
                " })();\n" +
                "</script>\n" +
                "</head><body style=\"margin:0px;\" onload=\"if (top!= self) top.location.replace(self.location.href);\"><a name=\"top\"></a><div align=\"center\"><div class=\"topliquidtable\"><table width=\"100%\" cellpadding=\"2\" cellspacing=\"0\" border=\"0\" align=\"center\"><tr><td align=\"left\" valign=\"top\" style=\"padding-left:5px\"><a href=\"/\" class=\"logolink\" title=\"Click here to go to \n" +
                "AngloINFO Geneva's front page\"><img border=\"0\" src=\"/images/ailogo_240x67.png\" width=\"240\" height=\"67\" title=\"AngloINFO, the global expat network.\" alt=\"AngloINFO Logo\"><div class=\"logoname\">Geneva</div></a></td><td align=\"right\" style=\"padding-right:5px\"><div class=\"screenonly\"><script type='text/javascript'><!--// <![CDATA[\n" +
                "OA_show('zone_1');\n" +
                "// ]]> --></script>\n" +
                "<script type='text/javascript'>\n" +
                "if (document.getElementById('noAd1')) {\n" +
                "document.write('<a href=\"/information.asp\"><img style=\"border:solid 1px #CCCCCC\" border=\"0\" src=\"/images/08information.gif\" width=\"194\" height=\"48\" alt=\"Reference INFOrmation Pages\"><\\/a>');\n" +
                "}\n" +
                "</script>\n" +
                "</div></td></tr></table>\n" +
                "<div class=\"breadcrumb\"><a href=\"/\">AngloINFO HOME</a> <img src=\"/images/9dagger.gif\" alt=\">\" border=\"0\" width=\"9\" height=\"9\"> <a href=\"/information.asp\">INFOrmation</a> <img src=\"/images/9dagger.gif\" alt=\" border = \" width=\"9\" height=\"9\"> Working in Switzerland - Work disputes - Strikes <img src=\"/images/9dagger.gif\" alt=\" border = \" width=\"9\" height=\"9\"></div></div><div class=\"liquidtable\"><table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr><td valign=\"top\" class=\"LHCol\"><div class=\"LHBox\"><div class=\"BoxTop\">Inside</div><p class=\"bulletMenu\"><a href=\"/\">Home</a></p><p class=\"bulletMenu\"><a href=\"http://blogs.angloinfo.com/?rid=011\">Blogs</a></p><p class=\"bulletMenu\"><a href=\"/classifieds.asp\">Classifieds</a></p><p class=\"bulletMenu\"><a href=\"/anglofile.asp\">Directory</a></p><p class=\"bulletMenu\"><a href=\"/forum/\">Forum</a></p><p class=\"bulletMenu\"><a href=\"/information.asp\">INFOrmation</a></p><p class=\"bulletMenu\"><a href=\"/information/11/movies.asp\">Movies</a></p><p class=\"bulletMenu\"><a href=\"/information/whatsontoday.asp\">What's On</a></p></div><div class=\"LHBox\"><div class=\"BoxTop\">More</div><p class=\"bulletMenu\"><a href=\"/sales/\">Advertising</a></p><p class=\"bulletMenu\"><a href=\"/bookshop/\">Bookshop</a></p><p class=\"bulletMenu\"><a href=\"/information/forex.asp\">Exchange Rates</a></p><p class=\"bulletMenu\"><a href=\"/games/\">Games</a></p><p class=\"bulletMenu\"><a href=\"/submit/getcategory.asp\">Get Listed!</a></p><p class=\"bulletMenu\"><a href=\"/information/linktous.asp\">Link To Us</a></p><p class=\"bulletMenu\"><a href=\"/maps/\">Maps</a></p><p class=\"bulletMenu\"><a href=\"/information/aa.asp\">Mobile Web</a></p><p class=\"bulletMenu\"><a href=\"/news/\">News</a></p><p class=\"bulletMenu\"><a href=\"/information/update.asp\">Newsletter</a></p><p class=\"bulletMenu\"><a href=\"/postcards/\">Postcards</a></p><p class=\"bulletMenu\"><a href=\"/search/\">Search</a></p><p class=\"bulletMenu\"><a href=\"/tellafriend.asp\">Tell A Friend</a></p><p class=\"bulletMenu\"><a href=\"/weather/\">Weather</a></p><p class=\"bulletMenu\"><a href=\"http://switzerland.angloinfo.com/\">(AngloINFO Switzerland)</a></p></div><script type='text/javascript'><!--// <![CDATA[\n" +
                "OA_show('zone_5');\n" +
                "// ]]> --></script>\n" +
                "<iframe src=\"http://www.facebook.com/plugins/activity.php?site=geneva.angloinfo.com&amp;width=160&amp;height=300&amp;header=true&amp;colorscheme=light&amp;font&amp;border_color\" scrolling=\"no\" frameborder=\"0\" style=\"border:none; overflow:hidden; width:160px; height:300px;\" allowTransparency=\"true\"></iframe>\n" +
                "</td><td valign=\"top\" class=\"CColLiquid\"><div class=\"AFCrossHed\">Geneva Local Reference INFOrmation</div><div class=\"IPbody\"><script LANGUAGE=\"JavaScript\" TYPE=\"text/javascript\">\n" +
                "<!-- \n" +
                "function shareWin(url) { \n" +
                "popupWin = window.open(url,'new_page','width=450,height=555') \n" +
                "if (popupWin.opener == null) popupWin.opener = self;popupWin.focus();} \n" +
                "-->\n" +
                "</script>\n" +
                "<div id=\"topbarsharer\" class=\"SharePrint\"><div style=\"display:none\" id=\"shareTitle\">Working in Switzerland - Work disputes - Strikes</div><a title=\"Click here to share this page with a friend or colleague\" href=\"javascript:shareWin('/share.asp?http://angloinfo.com/uau433');\"><img src=\"/images/08e.gif\" border=\"0\" height=\"13\" width=\"17\" alt=\"Share\"> Share</a> | <a title=\"Click here to print this page\" href=\"javascript:if (window.print != null) { window.print(); } else { alert('Your browser does not support this facility.  Please select Print from the File menu of your browser.'); }\"><img src=\"/images/08p.gif\" border=\"0\" height=\"13\" width=\"13\" alt=\"Print\"> Print</a> | <a href=\"#\" id=\"a8w\" onClick=\"if(this.innerText!='-'){document.getElementById('spbms').style.display='block';}else{document.getElementById('spbms').style.display='none';};return false;\"><img src=\"/images/08w2.gif\" border=\"0\" height=\"13\" width=\"25\" alt=\"Web bookmarks\"> <b>+</b></a><div id=\"spbms\" style=\"right:0px;text-align:left;display:none; position: absolute;background:white;border: solid 1px #666666;padding:5px;margin:2px\" onMouseOver=\"this.style.display='block';\" onMouseOut=\"this.style.display='none';\"><div class=\"buzz2\"><a id=\"buzz2\" title=\"Buzz this!\" href=\"/r.asp?http://www.google.com/buzz/post?url=http://www.google.com/buzz/post?url=http%3A%2F%2Fgeneva%2Eangloinfo%2Ecom%2Fcountries%2Fswitzerland%2Fwork16%2Easp\" target=\"_blank\">Buzz</a></div><div class=\"delicious2\"><a id=\"delicious2\" title=\"Post this page to Delicious\" href=\"/r.asp?http://del.icio.us/post?url=http://angloinfo.com/uau433&amp;title=Working+in+Switzerland+%2D+Work+disputes+%2D+Strikes\" target=\"_blank\">Delicious</a></div><div class=\"digg2\"><a id=\"digg2\" title=\"Post this page to Digg\" href=\"/r.asp?http://digg.com/submit?url=http://angloinfo.com/uau433&amp;title=Working+in+Switzerland+%2D+Work+disputes+%2D+Strikes\" target=\"_blank\">Digg</a></div><div class=\"facebook2\"><a id=\"facebook2\" title=\"Post this page to Facebook\" href=\"/r.asp?http://www.facebook.com/sharer.php?u=http%3A%2F%2Fgeneva%2Eangloinfo%2Ecom%2Fcountries%2Fswitzerland%2Fwork16%2Easp&amp;t=Working+in+Switzerland+%2D+Work+disputes+%2D+Strikes\" target=\"_blank\">Facebook</a></div><div class=\"googlesb2\"><a id=\"goolesb2\" title=\"Bookmark this page with Google\" href=\"/r.asp?http://www.google.com/bookmarks/mark?op=add&amp;bkmk=http://angloinfo.com/uau433&amp;title=Working+in+Switzerland+%2D+Work+disputes+%2D+Strikes\" target=\"_blank\">Google</a></div><div class=\"propeller2\"><a id=\"propeller2\" title=\"Bookmark this page on Propeller\" href=\"/r.asp?http://www.propeller.com/submit/?U=http://angloinfo.com/uau433&amp;T=Working+in+Switzerland+%2D+Work+disputes+%2D+Strikes\" target=\"_blank\">Propeller</a></div><div class=\"reddit2\"><a id=\"reddit2\" title=\"Post this page to reddit\" href=\"/r.asp?http://reddit.com/submit?url=http://angloinfo.com/uau433&amp;title=\" target=\"_blank\">reddit</a></div><div class=\"stumbleupon2\"><a id=\"stumbleupon2\" title=\"Post this page to StumbleUpon\" href=\"/r.asp?http://www.stumbleupon.com/submit?url=http://angloinfo.com/uau433&amp;title=Working+in+Switzerland+%2D+Work+disputes+%2D+Strikes\" target=\"_blank\">StumbleUpon</a></div><div class=\"twitter2\"><a id=\"twitter2\" title=\"Tweet this!\" href=\"http://twitter.com/intent/tweet?text=Working+in+Switzerland+%2D+Work+disputes+%2D+Strikes : &url=http://angloinfo.com/uau433\" target=\"_blank\">Twitter</a></div><div class=\"yahoosb2\"><a id=\"yahoosb2\" title=\"Bookmark this page with Yahoo!\" href=\"/r.asp?http://myweb2.search.yahoo.com/myresults/bookmarklet?u=http://angloinfo.com/uau433&amp;t=Working+in+Switzerland+%2D+Work+disputes+%2D+Strikes\" target=\"_blank\">Yahoo!</a></div></div></div><table><tr><td valign=\"top\"><img src=\"/images/i.gif\" height=\"36\" width=\"36\" alt=\"INFOrmation\" align=\"left\"></td><td valign=\"top\"><h1>Working in Switzerland - Work disputes - Strikes</h1></td></tr></table><div style=\"font-size:15px;line-height:130%;float:right;clear:right;margin-left:50px;position:relative;right:25px;bottom:20px;margin-bottom:0px;\"><div><span class=\"FacebookBM\"> <a href=\"/r.asp?http://www.facebook.com/sharer.php?u=http%3A%2F%2Fgeneva%2Eangloinfo%2Ecom%2Fcountries%2Fswitzerland%2Fwork16%2Easp&amp;t=Working in Switzerland - Work disputes - Strikes\" target=\"_blank\">Share on Facebook</a></span></div><div style=\"margin-top:5px;\"><span class=\"TwitterBM\"> <a href=\"/r.asp?http://twitter.com/intent/tweet?text=Working+in+Switzerland+%2D+Work+disputes+%2D+Strikes : &url=http://angloinfo.com/uau433\" target=\"_blank\">Tweet this!</a></span></div></div><!--b-->\n" +
                "\n" +
                "<table>\n" +
                "  <tr><td align=\"left\" valign=\"top\" class=\"graybox\"><table width=\"200\" valign=\"top\" cellpadding=\"0\"><tr><td class=\"crossbar3\">Contents:</td></tr><tr><td><b><a href=\"living.asp\">Introduction</a></b><br><br><b>Moving to Switzerland</b><br><small><b><a href=\"moving.asp\">Registration Procedures</a></b></small><br><small><b><a href=\"moving2.asp\">Moving Goods & Capital</a></b></small><br><small><b><a href=\"moving5.asp\">Moving of Cars</a></b></small><br><small><b><a href=\"moving7.asp\">Finding Accommodation</a></b></small><br><small><b><a href=\"moving8.asp\">Finding a School</a></b></small><br><small><b><a href=\"moving9.asp\">Moving Checklist</a></b></small><br><br><b>Living in Switzerland</b><br><small><b><a href=\"life.asp\">The System</a></b></small><br><small><b><a href=\"life1.asp\">Incomes & Taxes</a></b></small><br><small><b><a href=\"life2.asp\">Cost of Living</a></b></small><br><small><b><a href=\"life4.asp\">Accommodation</a></b></small><br><small><b><a href=\"life5.asp\">Cultural & Social Life</a></b></small><br><small><b><a href=\"life6.asp\">Educational System</a></b></small><br><small><b><a href=\"life7.asp\">Private Life</a></b></small><br><small><b><a href=\"life8.asp\">Transport</a></b></small><br><small><b><a href=\"life9.asp\">The Health System</a></b></small><br><br><b>Social Security</b><br><small><b><a href=\"socsec.asp\">Social Security in Europe</a></b></small><br><small><b><a href=\"socsec1.asp\">E forms: General Overview</a></b></small><br><small><b><a href=\"socsec2.asp\">General Organisation</a></b></small><br><small><b><a href=\"socsec3.asp\">Sickness Insurance</a></b></small><br><small><b><a href=\"socsec4.asp\">Family and Maternity Benefits</a></b></small><br><small><b><a href=\"socsec7.asp\">EU Health Card</a></b></small><br><small><b><a href=\"socsec8.asp\">Unemployment Benefits</a></b></small><br><br><b>Working in Switzerland</b><br><small><b><a href=\"work.asp\">Finding a Job</a></b></small><br><small><b><a href=\"work1.asp\">Applications</a></b></small><br><small><b><a href=\"work2.asp\">Recognition of Qualifications</a></b></small><br><small><b><a href=\"work3.asp\">Kinds of Employment</a></b></small><br><small><b><a href=\"work4.asp\">Employment Contracts</a></b></small><br><small><b><a href=\"work5.asp\">Remuneration</a></b></small><br><small><b><a href=\"work6.asp\">Working Time</a></b></small><br><small><b><a href=\"work7.asp\">Vocational Training</a></b></small><br><small><b><a href=\"work8.asp\">Leave</a></b></small><br><small><b><a href=\"work10.asp\">End of Employment</a></b></small><br><small><b><a href=\"work12.asp\">Special Categories</a></b></small><br><small><b><a href=\"work14.asp\">Self-employment</a></b></small><br><small><b><a href=\"work15.asp\">Representation of Workers</a></b></small><br><small><b>Work Disputes</b></small><br></td></tr></table>      \t</td>\n" +
                "      \t<td width=\"100%\" valign=top>\n" +
                "      \t<Table width=\"100%\">\n" +
                "      \t<tr>\n" +
                "<td class=\"crossbar2\" width=\"100%\">Work disputes - Strikes\n" +
                "       </td>\n" +
                "       </tr><tr><td>\n" +
                "\n" +
                "\n" +
                "The cantonal civil courts (usually the labour courts) have jurisdiction in\n" +
                "disputes arising from individual contracts of employment. Collective labour\n" +
                "disputes are settled differently in each canton. All cantons have conciliation\n" +
                "offices that deal with such disputes. The Federal Conciliation Office has\n" +
                "jurisdiction in disputes extending beyond the territory of a single canton, but\n" +
                "acts only if explicitly requested to do so by the parties (employers or\n" +
                "employersҠassociations and trade unions) and only if attempts to reach\n" +
                "agreement by direct negotiation have failed.<br>\n" +
                "<br>\n" +
                "The lawfulness of strikes and lockouts is inscribed in the Federal Constitution\n" +
                "as an expression of free association, although strikes can be prohibited under\n" +
                "the Constitution for specific categories of persons. Strikes and lockouts are\n" +
                "lawful only if they concern employment contracts, do not conflict with the\n" +
                "requirement to maintain peaceful labour relations or to negotiate a settlement,\n" +
                "and conform to the principle of proportionality. Participation in a legal strike\n" +
                "and the consequent stoppage of work do not constitute non-observance of the\n" +
                "contractual obligation to work. On the other hand, employers are not required to\n" +
                "pay strikers for the duration of a stoppage.\n" +
                "<p align=\"right\">Text last edited on: 06/2009\n" +
                "<p><small>Source: European Union<br>\n" +
                "  ɠEuropean Communities, 1995-2010<br>\n" +
                "<a href=\"/r.asp?http://europa.eu/geninfo/legal_notices_en.htm\">Reproduction is authorised</a>.</small>\n" +
                "          </td></tr><tr><td>\n" +
                "\n" +
                "\n" +
                "          </td></tr><tr><td>\n" +
                "\n" +
                "\n" +
                "          </td></tr></table></td></tr></table>\n" +
                "          \n" +
                "          <!--e--></div><div class=\"screenonly\"><center><script type='text/javascript'><!--// <![CDATA[\n" +
                "OA_show('zone_3');\n" +
                "// ]]> --></script></center>\n" +
                "</div></td>\n" +
                "<td valign=\"top\" class=\"RHCOL\"><div class=\"RHBOX\" id=\"myangloinfo\"><div class=\"BoxTop\">myAngloINFO</div><div><p class=\"bulletMenu\"><a href=\"/forum/login.asp?continue=/countries/switzerland/work16.asp\">Sign in</a></p><p class=\"bulletMenu\"><a href=\"/forum/policy.asp\">Become a member NOW!</a></p><div class=\"innerSidebar\"><form method=\"POST\" action=\"/information/11/aiu.asp\" name=\"update1\" style=\"margin-bottom:0px; margin-top:0px\"><b>Stay INFOrmed!</b> with our weekly <a href=\"/information/update.asp\">newsletter</a>.<div style=\"margin-top:5px;text-align:center\"><input type=\"text\" name=\"e1\" style=\"font-size:11px;width:150px\" value=\"Enter e-mail address\" onClick=\"if(this.value=='Enter e-mail address')this.value='';\"><br><button type=\"submit\" style=\"margin-bottom:0px; margin-top:3px;font-size:11px;HEIGHT:22PX\">Sign me up!</button></div></form></div></div></div><div id=\"today\" class=\"RHBOX\"><div class=\"BoxTopBright\">Today</div><div class=\"innerSidebar\">\n" +
                "\n" +
                "<a href=\"/weather/\" style=\"color:black\"><img src=\"http://www.angloinfo.com/images/weather/50x50/partlycloudy.gif\" width=\"64\" height=\"64\" align=\"right\" border=\"0\" alt=\"-10.6&deg;C and scattered clouds\"><div class=\"AFweather\"><b>Sunrise:</b> 7:47<br><b>Sunset:</b> 17:53<br/><b>The Weather:</b><br>-10.6&deg;C (12&deg;F) and scattered clouds in Geneva (at 22:37)</a></div><div><p><img src=\"/images/9dagger.gif\" alt=\">\" border=\"0\" width=\"9\" height=\"9\"> <a href=\"/weather/\">Forecasts...</a><!--46.1,6--></div></div></div><center><script type='text/javascript'><!--// <![CDATA[\n" +
                "OA_show('zone_23');\n" +
                "// ]]> --></script></center>\n" +
                "<div class=\"RHBOX\"><div class=\"BoxTop\">Take a break</div><div class=\"innerSidebar\"><div id=\"games\" style=\"text-align:center;margin-left:-3px;margin-top:2px;height:111px\"><a href=\"/games/\" style=\"font-size:11px\"><img src=\"/images/sudokusm.gif\" border=\"0\" width=\"100\" height=\"94\" alt=\"Sudoku\" align=\"left\" style=\"margin:4px\">Relax for a few minutes in our online leisure zone<br></a></div></div></div><div id=\"regionmenurhb\" class=\"RHBOX\"><div class=\"BoxTop\">Essential INFOrmation</div><div class=\"innerSidebarTrans\"><form action=\"/search/infosearch.asp\" style=\"margin:0px\"><input type=\"text\" name=\"q\" size=\"14\" title=\"Type your search terms here\" class=\"ForumSearchBox\">&nbsp;<button type=\"submit\" title=\"Click here to search\" class=\"ForumSearchBtn\">Search</button></form></div><div class=\"BoxSub\">Living In the Geneva region</div><div>\n" +
                "<p class=\"TightbulletMenu\"><a href=\"/information/11/natadmin.asp\">Administration</a>\n" +
                "<p class=\"TightbulletMenu\"><a href=\"/information/11/airports.asp\">Airports</a>\n" +
                "<p class=\"TightbulletMenu\"><a href=\"/information/11/busses.asp\">Bus & Tram</a>\n" +
                "<p class=\"TightbulletMenu\"><a href=\"/information/11/disabled.asp\">Disabled</a>\n" +
                "<p class=\"TightbulletMenu\"><a href=\"/countries/switzerland/driving.asp\">Driving</a>\n" +
                "<p class=\"TightbulletMenu\"><a href=\"/information/11/em_no.asp\">Emergencies</a>\n" +
                "<p class=\"TightbulletMenu\"><a href=\"/countries/switzerland/animals.asp\">Pets Essentials</a>\n" +
                "<p class=\"TightbulletMenu\"><a href=\"/countries/switzerland/phonebooks.asp\">Phone Books</a>\n" +
                "<p class=\"TightbulletMenu\"><a href=\"/countries/switzerland/postcodes.asp\">Postal Codes</a>\n" +
                "<p class=\"TightbulletMenu\"><a href=\"/information/11/trains.asp\">Railways</a>\n" +
                "<p class=\"TightbulletMenu\"><a href=\"/countries/switzerland/cia.asp\">CIA Factsheets</a>\n" +
                "<p class=\"TightbulletMenu\"><a href=\"/countries/switzerland/living.asp\">EU Factsheets</a>\n" +
                "<p class=\"TightbulletMenu\"><a href=\"/countries/switzerland/moving.asp\">&nbsp;&nbsp;&nbsp; Moving to Switzerland</a>\n" +
                "<p class=\"TightbulletMenu\"><a href=\"/countries/switzerland/life.asp\">&nbsp;&nbsp;&nbsp; Living in Switzerland</a>\n" +
                "<p class=\"TightbulletMenu\"><a href=\"/countries/switzerland/work.asp\">&nbsp;&nbsp;&nbsp; Working in Switzerland</a>\n" +
                "<p class=\"TightbulletMenu\"><a href=\"/countries/switzerland/socsec.asp\">&nbsp;&nbsp;&nbsp; Social Security</a>\n" +
                "<p class=\"TightbulletMenu\"><a href=\"/information/11/pub_hols.asp\">Public Holidays</a>\n" +
                "<p class=\"TightbulletMenu\"><a href=\"/information/11/schoolhols.asp\">School Terms</a>\n" +
                "<p class=\"TightbulletMenu\"><a href=\"/information/11/schooling.asp\">Schooling</a>\n" +
                "<p class=\"TightbulletMenu\"><a href=\"/countries/switzerland/translate.asp\">Translate Online</a></div><div class=\"BoxSub\">Moving to the Geneva region</div><div><p class=\"TightbulletMenu\"><a href=\"/countries/switzerland/housebuy.asp\">Buying a House or Property in Switzerland</a><p class=\"TightbulletMenu\"><a href=\"/countries/switzerland/banking.asp\">Opening a Bank Account</a><p class=\"TightbulletMenu\"><a href=\"/countries/switzerland/pettravel.asp\">Moving Pets to Switzerland</a><p class=\"TightbulletMenu\"><a href=\"/information/11/propjargon.asp\">Real Estate Jargon</a><p class=\"TightbulletMenu\"><a href=\"/information/11/re_rental.asp\">Renting in Switzerland</a><p class=\"TightbulletMenu\"><a href=\"/countries/switzerland/residency.asp\">Residency in Switzerland</a><p class=\"TightbulletMenu\"><a href=\"/countries/switzerland/telecoms.asp\">Telephone & Internet Connections</a></div><div class=\"BoxSub\">Life in the Geneva region</div><div><p class=\"TightbulletMenu\"><a href=\"/information/whatsontoday.asp\">What's On</a><p class=\"TightbulletMenu\"><a href=\"/af/281/\">Kids Days' Out</a><p class=\"TightbulletMenu\"><a href=\"/af/292/\">Museums</a><p class=\"TightbulletMenu\"><a href=\"/af/234/\">Holiday Accommodation</a><p class=\"TightbulletMenu\"><a href=\"/information/11/markets.asp\">Produce Markets</a><p class=\"TightbulletMenu\"><a href=\"/af/36/\">Boats &amp; Sailing</a><p class=\"TightbulletMenu\"><a href=\"/information/11/ltourism.asp\">Local Tourism</a><p class=\"TightbulletMenu\"><a href=\"/information/11/outdoors.asp\">The Great Outdoors</a><p class=\"TightbulletMenu\"><a href=\"/bookshop/default.asp?id=1\">Geneva Travel Guides</a></div><div align=\"right\"><p class=\"Tightbulletmenu\"><a href=\"/information.asp\">More INFOrmation Pages...</a></p></div></div></td></tr></table><div id=\"socialBookmarks\" class=\"socialBookmarks\">Bookmark with: <span class=\"buzz\"><a id=\"buzz\" title=\"Buzz this!\" href=\"/r.asp?http://www.google.com/buzz/post?url=http://www.google.com/buzz/post?url=http%3A%2F%2Fgeneva%2Eangloinfo%2Ecom%2Fcountries%2Fswitzerland%2Fwork16%2Easp\" target=\"_blank\">Buzz</a></span><span class=\"delicious\"><a id=\"delicious\" title=\"Post this page to Delicious\" href=\"/r.asp?http://del.icio.us/post?url=http://angloinfo.com/uau433&amp;title=Working+in+Switzerland+%2D+Work+disputes+%2D+Strikes\" target=\"_blank\">Delicious</a></span><span class=\"digg\"><a id=\"digg\" title=\"Post this page to Digg\" href=\"/r.asp?http://digg.com/submit?url=http://angloinfo.com/uau433&amp;title=Working in Switzerland - Work disputes - Strikes\" target=\"_blank\">Digg</a></span><span class=\"facebook\"><a id=\"facebook\" title=\"Post this page to Facebook\" href=\"/r.asp?http://www.facebook.com/sharer.php?u=http%3A%2F%2Fgeneva%2Eangloinfo%2Ecom%2Fcountries%2Fswitzerland%2Fwork16%2Easp&amp;t=Working+in+Switzerland+%2D+Work+disputes+%2D+Strikes\" target=\"_blank\">Facebook</a></span><span class=\"googlesb\"><a id=\"goolesb\" title=\"Bookmark this page with Google\" href=\"/r.asp?http://www.google.com/bookmarks/mark?op=add&amp;bkmk=http://angloinfo.com/uau433&amp;title=Working+in+Switzerland+%2D+Work+disputes+%2D+Strikes\" target=\"_blank\">Google</a></span><span class=\"propeller\"><a id=\"propeller\" title=\"Bookmark this page on Propeller\" href=\"/r.asp?http://www.propeller.com/submit/?U=http://angloinfo.com/uau433&amp;T=Working+in+Switzerland+%2D+Work+disputes+%2D+Strikes\" target=\"_blank\">Propeller</a></span><span class=\"reddit\"><a id=\"reddit\" title=\"Post this page to reddit\" href=\"/r.asp?http://reddit.com/submit?url=http://angloinfo.com/uau433&amp;title=Working+in+Switzerland+%2D+Work+disputes+%2D+Strikes\" target=\"_blank\">reddit</a></span><span class=\"stumbleupon\"><a id=\"stumbleupon\" title=\"Post this page to StumbleUpon\" href=\"/r.asp?http://www.stumbleupon.com/submit?url=http://angloinfo.com/uau433&amp;title=Working+in+Switzerland+%2D+Work+disputes+%2D+Strikes\" target=\"_blank\">StumbleUpon</a></span><span class=\"twitter\"><a id=\"twitter\" title=\"Tweet this!\" href=\"http://twitter.com/intent/tweet?text=Working+in+Switzerland+%2D+Work+disputes+%2D+Strikes : &url=http://angloinfo.com/uau433\" target=\"_blank\">Twitter</a></span><span class=\"yahoosb\"><a id=\"yahoosb\" title=\"Bookmark this page with Yahoo!\" href=\"/r.asp?http://myweb2.search.yahoo.com/myresults/bookmarklet?u=http://angloinfo.com/uau433&amp;t=Working+in+Switzerland+%2D+Work+disputes+%2D+Strikes\" target=\"_blank\">Yahoo!</a></span></div><p align=\"center\" class=\"screenOnly\"><a href=\"#top\"><small>^ Top of Page ^</small></a></p><hr width=\"723\" align=\"center\" class=\"screenOnly\"><div class=\"newshed\"><center><span style=\"color: #666666\">Page generated at 01:13; Saturday 11 February 2012 Share as: http://angloinfo.com/uau433</span><br><a href=\"http://www.angloinfo.com/copyright/\" target=\"_blank\">Copyright</a> &copy; 2000-2012<span class=\"screenOnly\"> <a href=\"http://www.angloinfo.com/\">AngloINFO Limited</a>. All rights reserved. <a href=\"http://www.angloinfo.com/privacy/\" target=\"_blank\" class=\"screenOnly\">Privacy Policy</a>, <a href=\"http://www.angloinfo.com/terms-of-use/\" target=\"_blank\" class=\"screenOnly\">Terms of Use</a>, <a href=\"http://www.angloinfo.com/about/what-is-angloinfo/\" target=\"_blank\" class=\"screenOnly\">About</a>, <a href=\"/sales/\" class=\"screenOnly\">Advertising</a>, <a href=\"javascript:openCentralWindow('/central/contact.asp')\" class=\"screenOnly\">Contact</a>.<br><span style=\"color: #666666\"></span>AngloINFO: Everyday life in Switzerland, in English</span><br><b><a href=\"http://switzerland.angloinfo.com/\" class=\"screenOnly\">Find out more about AngloINFO in Switzerland...</a></b></center></div>\n" +
                "</div></div></body></html>";
        String contents;

        contents = new Boilerpipe().LCExtractor(HTML);

        System.out.println(contents);
    }

    @org.junit.Test
    public void testIgnite() {
        //Ignite ignite = Ignition.ignite();
        IgniteConfiguration cfg = new IgniteConfiguration();

        DataStorageConfiguration storageCfg = new DataStorageConfiguration();
        storageCfg.getDefaultDataRegionConfiguration().setMaxSize(2L * 1024 * 1024 * 1024);//20GB
       // storageCfg.getDefaultDataRegionConfiguration().setPersistenceEnabled(true); //use disk
        cfg.setDataStorageConfiguration(storageCfg);

        Ignite ignite=null;
        IgniteCache<String, LinkedList<String>> cache=null;
        try {
            ignite = Ignition.start(cfg);
            //ignite.cluster().active(true);


            ignite.destroyCache("stemVariants");
            cache = ignite.createCache("stemVariants");
            cache = cache.withExpiryPolicy(new CreatedExpiryPolicy(new Duration(TimeUnit.MINUTES,1)));

            System.out.println("Start");
            LinkedList<String> ls = new LinkedList<String>();
            for (int i = 0; i < 10; i++)
                ls.add(Integer.toString(i));
            cache.put("a", ls);

            LinkedList<String> l = cache.get("a");
            l.add("sad");
            cache.put("a", l);
            System.out.println(cache.get("a"));
            ignite.destroyCache("stemVariants");
            System.out.println("Finish");
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if(cache!=null)cache.close();
            if(ignite!=null)ignite.close();
        }
    }

    @org.junit.Test
    public void testChiSquare() {
        long[] obs1 = {1057591  ,8131162  ,2261521  };
        long[] obs2 = {1050256  ,7362290  ,1617979  };

        ChiSquareTest chi = new ChiSquareTest();
        //boolean isSig=chi.chiSquareTest(exp,obs,0.05);
        double pval=chi.chiSquareDataSetsComparison(obs1,obs2);
        boolean isSig=chi.chiSquareTestDataSetsComparison(obs1,obs2,0.05);
        System.out.println(isSig+" "+pval);

    }


    @org.junit.Test
    public void testAnalyzer() {
        Analyzer analyzer = Analyzers.analyzer(Tag.KStem);
        try (TokenStream ts = analyzer.tokenStream("contents", new StringReader("lick"))) {

            final CharTermAttribute termAtt = ts.addAttribute(CharTermAttribute.class);
            ts.reset(); // Resets this stream to the beginning. (Required)
            while (ts.incrementToken())
                System.out.println(termAtt.toString());

            ts.end();   // Perform end-of-stream operations, e.g. set the final offset.
        } catch (IOException ioe) {
            throw new RuntimeException("happened during string analysis", ioe);
        }

    }

    @org.junit.Test
    public void testTirt() {
        //Aggregate.Variance v = new Aggregate.Variance();
        long[] obs1DF = {2,0,4,0,6};
        long[] obs1TF = {0,3,4,9,0};
        System.out.println(Math.round(4.764654234));
        long[] obs1 = Arrays.stream(obs1DF).map(v -> v==0 ? 1:v).toArray();
        Arrays.stream(obs1).forEach(v->System.out.print(v));
        //double v1IDF[] = Arrays.stream(a).map(d->new Aggregate.Average().aggregate(d)).toArray();
        //Arrays.stream(v1IDF).forEach(System.out::println);

       // System.out.println(new Aggregate.GeometricMean().aggregate(a));

        //System.out.println(new Aggregate.HarmonicMean().aggregate(a));
        String s = "GX048-70-3719574";
        System.out.println();
        System.out.println(s.replaceAll("[a-zA-Z\\-]",""));
    }

//    @org.junit.Test
//    public void testSolr() throws IOException, SolrServerException {
//
//        Properties props=CLI.readProperties();
//        final String tfd_home = props.getProperty("tfd.home");
//        SpamTool.CW09_spam_link = props.getProperty("link.spam.CW09");
//        SpamTool.CW12_spam_link = props.getProperty("link.spam.CW12");
//
//
//        final HttpSolrClient solr = SpamTool.getSpamSolr(Collection.MQ09);
//
//        int p = SpamTool.percentile(solr, "clueweb09-en0010-79-02218");
//        System.out.println(p);
//    }

    @org.junit.Test
    public void test() throws Exception {
        Integer[] spam = new Integer[] { 1, 2, 3,8,10,45,7,4,9,12 };
        List<Integer> t= Arrays.asList(spam);
        t.sort((t1, t2) -> Integer.compare(t1, t2));
        System.out.println(t.toString());
        //String[] args ={"SelectiveStemming","-collection","MC"};
        //CLI.main(args);
    }

    @org.junit.Test
    public void CLITest() throws Exception {
     //   String[] args ={"TFDistribution","-collection","MC","-task","term"};
      //  String[] args = {"SelectiveStemming", "-collection", "MC", "-tags", "NoStemTurkish_Zemberek", "-metric","NDCG20", "-spam", "0", "-selection", "MSTDF", "-binDF","10"};
        String[] args ={"Stats","-collection","MQ07","-task","queryLength"};
      //  String[] args ={"AdHocExp","-collection","MQ09", "-tag","SnowballEng","-task","resultSet", "-models", "BM25k1.3b0.5_PL2c4.0_LGDc2.0_DirichletLMc500.0_DPH_DFIC_DFRee_DLH13"};
     //  String[] args ={"SystemEvaluator","-collection","MC","-metric","NDCG20","-tags","NoStemTurkish_Zemberek_SnowballTr_F5Stem"};
//        String[] args ={"Indexer","-collection","MC","-tag","Zemberek"};
     //   String[] args ={"Searcher","-collection","MC","-task","param"};

        CLI.main(args);
    }

    @org.junit.Test
    public void testRandom() throws Exception {
        String[] tags = {"NoStemTurkish","F5Stem","Snowball","Zemberek"};
        List<Pair<String,Double>> weights = new ArrayList();

            weights.add(new Pair(tags[0], 0.25));
        weights.add(new Pair(tags[1], 0.25));
        weights.add(new Pair(tags[2], 0.25));
        weights.add(new Pair(tags[3], 0.25));

        for(int i=0;i<20;i++) {
            String selectedItem = new EnumeratedDistribution(weights).sample().toString();
            System.out.println(selectedItem);
        }
    }
    @org.junit.Test
    public void testEvaluator(){
        final String tfd_home = "C:\\Data\\TFD_HOME";
        Collection collection=Collection.MC;
        boolean catB = false;
        String tags = "NoStemTurkish_Zemberek_SnowballTr_F5Stem";
        Measure measure = Measure.NDCG20;
        String op = "OR";

        DataSet dataSet = CollectionFactory.dataset(collection, tfd_home);
        int spam =0;
        String evalDirectory = spam == 0 ? "evals" : "spam_" + spam + "_evals";

        if (catB && (Collection.CW09B.equals(collection) || Collection.CW12B.equals(collection)))
            evalDirectory = "catb_evals";

        final String[] tagsArr = tags.split("_");
        //if(tagsArr.length!=2) return;

        Set<String> modelIntersection = new HashSet<>();

        Map<Tag, Evaluator> evaluatorMap = new HashMap<>();

        for (int i = 0; i < tagsArr.length; i++) {
            String tag = tagsArr[i];
            final Evaluator evaluator = new Evaluator(dataSet, tag, measure, "all", evalDirectory, op);
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
        systemEvaluator.printTopicModelSortedByVariance();
        systemEvaluator.printCountMap();


        System.out.println("=========  Mean and MeanWT ===========");
        systemEvaluator.printMean();
        systemEvaluator.printMeanWT();

        System.out.println("=========  Random and Oracle ===========");
        systemEvaluator.printRandom();
        systemEvaluator.printRandomMLE();
        systemEvaluator.printRandomX();
        //System.out.println("OracleMin : " + evaluator.oracleMin());
        systemEvaluator.printOracleMax();
        systemEvaluator.printHighestScoresWithCoV(false);

        System.out.println("========= Facets ===========");
        systemEvaluator.printFacets();

        //needs = evaluatorMap.get(tagsArr[0]).residualNeeds()
        /*
        needs = evaluatorMap.get(tagsArr[0]).getNeeds();
        Integer needSize = needs.size();
        for (String model : modelIntersection) {
            double avgBestScores=0.0;
            System.out.println(model);
            System.out.println("Query\tTag\t"+measure);
            for(int i=0; i<needSize;i++){
                String bestTag="";
                double bestScore = Double.NEGATIVE_INFINITY;
                for(String tag:tagsArr) {
                    double score = evaluatorMap.get(tag).score(needs.get(i), model);
                    if(score>bestScore){
                        bestScore=score;
                        bestTag=tag;
                    }
                }
                System.out.println(needs.get(i).id()+"\t"+bestTag+"\t"+bestScore);
                avgBestScores+=bestScore;
            }
            System.out.println("Average\t"+avgBestScores/needSize);
            System.out.println("========\t========\t=======");

        }
        */
    }

}
