package edu.anadolu.field;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;


public class SemanticStats {
    private volatile static SemanticStats semanticStats = new SemanticStats();

    private Analyzer analyzer;

    private  HashMap<String,Integer> docTypeCounter = new HashMap<>();
    private  HashMap<Pair,Integer> textTitleMapperAcronym = new HashMap<>();
    private  HashMap<Pair,Integer> textTitleMapperAbbr = new HashMap<>();
    private  HashMap<String,Integer> tagTFCounter = new HashMap<>();
    private  HashMap<String,Integer> tagDFCounter = new HashMap<>();
    private  HashMap<Pair,Integer> textdfnMapper = new HashMap<>();

    private SemanticStats(){
        try {
            this.analyzer = MetaTag.whitespaceAnalyzer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static SemanticStats getSemanticObject(){
        return semanticStats;
    }

    public synchronized Integer incDocType(String type){
        type=getNormalizedText(type);
        if(docTypeCounter.containsKey(type)){
            docTypeCounter.put(type,docTypeCounter.get(type)+1);
        }else docTypeCounter.put(type,1);
        return docTypeCounter.get(type);
    }

    public synchronized Integer incTagTF(String tag){
        tag=getNormalizedText(tag);
        if(tagTFCounter.containsKey(tag)){
            tagTFCounter.put(tag,tagTFCounter.get(tag)+1);
        }else tagTFCounter.put(tag,1);
        return tagTFCounter.get(tag);
    }

    public synchronized Integer incTagTFby(String tag,Integer x){
        tag=getNormalizedText(tag);
        if(tagTFCounter.containsKey(tag)){
            tagTFCounter.put(tag,tagTFCounter.get(tag)+x);
        }else tagTFCounter.put(tag,x);
        return tagTFCounter.get(tag);
    }

    public synchronized Integer incTagDF(String tag){
        tag=getNormalizedText(tag);
        if(tagDFCounter.containsKey(tag)){
            tagDFCounter.put(tag,tagDFCounter.get(tag)+1);
        }else tagDFCounter.put(tag,1);
        return tagDFCounter.get(tag);
    }

    public synchronized Pair putTextWithDfn(String dfn,String text){
        dfn=getNormalizedText(dfn);
        text=getNormalizedText(text);
        Pair<String,String> p = Pair.of(dfn,text);
        if(!textdfnMapper.keySet().contains(p)) {
            textdfnMapper.put(p, 1);
        }else{
            textdfnMapper.put(p,textdfnMapper.get(p)+1);
        }
        return p;
    }

    public synchronized Pair putTextWithTitleAcronym(String title,String text){
        title=getNormalizedText(title);
        text=getNormalizedText(text);
        Pair<String,String> p = Pair.of(title,text);
        if(!textTitleMapperAcronym.keySet().contains(p)) {
            textTitleMapperAcronym.put(p, 1);
        }else{
            textTitleMapperAcronym.put(p,textTitleMapperAcronym.get(p)+1);
        }
        return p;
    }

    public synchronized Pair putTextWithTitleAbbr(String title,String text){
        title=getNormalizedText(title);
        text=getNormalizedText(text);
        Pair<String,String> p = Pair.of(title,text);
        if(!textTitleMapperAbbr.keySet().contains(p)) {
            textTitleMapperAbbr.put(p, 1);
        }else{
            textTitleMapperAbbr.put(p,textTitleMapperAbbr.get(p)+1);
        }
        return p;
    }

    private String getNormalizedText(String text){
        TokenStream stream = analyzer.tokenStream(null, text);
        StringBuilder builder = new StringBuilder();
        try {
            stream.reset();
            while (stream.incrementToken()) {
                builder.append(stream.getAttribute(CharTermAttribute.class).toString()+" ");
            }
            stream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return builder.toString().trim();
    }
    public void printSemanticStats(){
        try {
            FileWriter writer = new FileWriter("SemanticStats.txt");
            StringBuilder builder = new StringBuilder();

            builder.append("DocTypes" + System.lineSeparator());
            docTypeCounter.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).limit(50).
                    forEach(entry -> builder.append(entry.getKey() + "\t" + entry.getValue() + System.lineSeparator()));


            builder.append("Tag TF Amounts" + System.lineSeparator());
            tagTFCounter.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).
                    forEach(entry -> builder.append(entry.getKey() + "\t" + entry.getValue() + System.lineSeparator()));

            builder.append("Tag DF Amounts" + System.lineSeparator());
            tagDFCounter.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).
                    forEach(entry -> builder.append(entry.getKey() + "\t" + entry.getValue() + System.lineSeparator()));


            builder.append("Dfn-Definition Pairs" + System.lineSeparator());
            textdfnMapper.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).limit(1000).
                    forEach(entry -> builder.append(entry.getKey().getLeft() + "\t" + entry.getKey().getRight() + "\t" + entry.getValue() + System.lineSeparator()));

            builder.append("Title-Text Acronym Pairs" + System.lineSeparator());
            textTitleMapperAcronym.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).limit(1000).
                    forEach(entry -> builder.append(entry.getKey().getLeft() + "\t" + entry.getKey().getRight() + "\t" + entry.getValue() + System.lineSeparator()));


            builder.append("Title-Text Abbr Pairs" + System.lineSeparator());
            textTitleMapperAbbr.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).limit(1000).
                    forEach(entry -> builder.append(entry.getKey().getLeft() + "\t" + entry.getKey().getRight() + "\t" + entry.getValue() + System.lineSeparator()));


            writer.write(builder.toString());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
