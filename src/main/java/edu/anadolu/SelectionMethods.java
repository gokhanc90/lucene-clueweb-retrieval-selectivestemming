package edu.anadolu;

import edu.anadolu.qpp.Aggregate;
import edu.anadolu.stats.TermStats;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.correlation.KendallsCorrelation;
import java.util.*;
import java.util.stream.Collectors;

/**
 * If a change is occurred either order or specific term due to the freq. , select NoStem.
 */
public class SelectionMethods {
    public static double CorrThreshold=0.99; // set by selective stemming tool

    public enum SelectionTag {

        MSTTF, MSTDF, LSTDF, LSTTF, TFOrder, DFOrder, KendallTauTFOrder, KendallTauDFOrder,MSTTFBinning,MSTDFBinning,
        TFOrderBinning, DFOrderBinning, KendallTauTFOrderBinning, KendallTauDFOrderBinning, CosineSim, Features;

        public static SelectionTag tag(String selectionTag) {
           return valueOf(selectionTag);
        }
    }

    public static class TermTFDF {
        public static int NumberOfBIN = 10; // set by selective stemming tool
        public static long maxDF; // set by selective stemming tool
        public static long maxTF; //


        private int indexID;
        private long TF;
        private long DF;
        private int binTF;
        private int binDF;

        public TermTFDF(int indexID) {
            this.indexID = indexID;
        }

        public int getIndexID() {
            return indexID;
        }

        public long getTF() {
            return TF;
        }

        public void setTF(long TF) {
            this.TF = TF;
            setBinTF(TF);
        }

        public long getDF() {
            return DF;
        }

        public void setDF(long DF) {
            this.DF = DF;
            setBinDF(DF);
        }

        public int getBinTF() {
            return binTF;
        }

        public void setBinTF(long TF) {
            int size=(int)maxTF/NumberOfBIN;
            this.binTF=(int)TF/size;
        }

        public int getBinDF() {
            return binDF;
        }

        public void setBinDF(long DF) {
            int size=(int)maxDF/NumberOfBIN;
            this.binDF=(int)DF/size;
        }

        @Override
        public String toString() {
            return  String.valueOf(indexID);
        }
    }

    public static String getPredictedTag(String selectionTag,Map<String, ArrayList<TermStats>> tagTermTermStats, String[] tagsArr){
/*        if(tagTermTermStats.get(tagsArr[0]).size() == 1){
            System.err.print(String.format("%s\t","NotChanged One-Term"));
            return tagsArr[0]; //One-term Stem
        }*/
        switch (SelectionTag.tag(selectionTag)) {
            case MSTTF: return MSTTermFreq(tagTermTermStats,tagsArr);
            case MSTDF: return MSTDocFreq(tagTermTermStats, tagsArr);
            case LSTDF: return LSTDocFreq(tagTermTermStats, tagsArr);
            case LSTTF: return LSTTermFreq(tagTermTermStats, tagsArr);
            case TFOrder: return TFOrder(tagTermTermStats, tagsArr);
            case DFOrder: return DFOrder(tagTermTermStats, tagsArr);
            case KendallTauTFOrder: return KendallTauTFOrder(tagTermTermStats, tagsArr);
            case KendallTauDFOrder: return KendallTauDFOrder(tagTermTermStats, tagsArr);
            case MSTTFBinning: return MSTTFBinning(tagTermTermStats, tagsArr);
            case MSTDFBinning: return MSTDFBinning(tagTermTermStats, tagsArr);
            case TFOrderBinning: return TFOrderBinning(tagTermTermStats, tagsArr);
            case DFOrderBinning: return DFOrderBinning(tagTermTermStats, tagsArr);
            case KendallTauTFOrderBinning: return KendallTauTFOrderBinning(tagTermTermStats, tagsArr);
            case KendallTauDFOrderBinning: return KendallTauDFOrderBinning(tagTermTermStats, tagsArr);
            case CosineSim: return CosineSim(tagTermTermStats, tagsArr);
            case Features: return Features(tagTermTermStats, tagsArr);

            default: throw new AssertionError(SelectionMethods.class);
        }
    }

    private static String MSTDFBinning(Map<String, ArrayList<TermStats>> tagTermTermStats, String[] tagsArr) {
        ArrayList<TermTFDF> listTermTag1 = new ArrayList<TermTFDF>();
        ArrayList<TermTFDF> listTermTag2 = new ArrayList<TermTFDF>();

        ArrayList<TermStats> tsList = tagTermTermStats.get(tagsArr[0]);
        for (int i = 0; i < tsList.size(); i++) {
            TermTFDF termTFDF = new TermTFDF(i);
            termTFDF.setDF(tsList.get(i).docFreq());
            listTermTag1.add(termTFDF);
        }

        tsList = tagTermTermStats.get(tagsArr[1]);
        for (int i = 0; i < tsList.size(); i++) {
            TermTFDF termTFDF = new TermTFDF(i);
            termTFDF.setDF(tsList.get(i).docFreq());
            listTermTag2.add(termTFDF);
        }

        listTermTag1.sort((t1, t2) -> Integer.compare(t1.getBinDF(), t2.getBinDF()));
        listTermTag2.sort((t1, t2) -> Integer.compare(t1.getBinDF(), t2.getBinDF()));

        if (listTermTag1.get(0).getIndexID() != listTermTag2.get(0).getIndexID()){
            System.err.print(String.format("%s\t","Changed")); //print part1
            return tagsArr[0]; //Nostem
        }
        System.err.print(String.format("%s\t","NotChanged")); //print part1
        return tagsArr[1];
    }

    private static String MSTTFBinning(Map<String, ArrayList<TermStats>> tagTermTermStats, String[] tagsArr) {
        ArrayList<TermTFDF> listTermTag1 = new ArrayList<TermTFDF>();
        ArrayList<TermTFDF> listTermTag2 = new ArrayList<TermTFDF>();

        ArrayList<TermStats> tsList = tagTermTermStats.get(tagsArr[0]);
        for (int i = 0; i < tsList.size(); i++) {
            TermTFDF termTFDF = new TermTFDF(i);
            termTFDF.setTF(tsList.get(i).totalTermFreq());
            listTermTag1.add(termTFDF);
        }

        tsList = tagTermTermStats.get(tagsArr[1]);
        for (int i = 0; i < tsList.size(); i++) {
            TermTFDF termTFDF = new TermTFDF(i);
            termTFDF.setTF(tsList.get(i).totalTermFreq());
            listTermTag2.add(termTFDF);
        }

        listTermTag1.sort((t1, t2) -> Integer.compare(t1.getBinTF(), t2.getBinTF()));
        listTermTag2.sort((t1, t2) -> Integer.compare(t1.getBinTF(), t2.getBinTF()));

        if (listTermTag1.get(0).getIndexID() != listTermTag2.get(0).getIndexID()) {
            System.err.print(String.format("%s\t","Changed")); //print part1
            return tagsArr[0]; //Nostem
        }
        System.err.print(String.format("%s\t","NotChanged")); //print part1
        return tagsArr[1];
    }

    private static String KendallTauDFOrderBinning(Map<String, ArrayList<TermStats>> tagTermTermStats, String[] tagsArr) {
        ArrayList<TermTFDF> listTermTag1 = new ArrayList<TermTFDF>();
        ArrayList<TermTFDF> listTermTag2 = new ArrayList<TermTFDF>();

        ArrayList<TermStats> tsList = tagTermTermStats.get(tagsArr[0]);
        for (int i = 0; i < tsList.size(); i++) {
            TermTFDF termTFDF = new TermTFDF(i);
            termTFDF.setDF(tsList.get(i).docFreq());
            listTermTag1.add(termTFDF);
        }

        tsList = tagTermTermStats.get(tagsArr[1]);
        for (int i = 0; i < tsList.size(); i++) {
            TermTFDF termTFDF = new TermTFDF(i);
            termTFDF.setDF(tsList.get(i).docFreq());
            listTermTag2.add(termTFDF);
        }

        listTermTag1.sort((t1, t2) -> Integer.compare(t1.getBinDF(), t2.getBinDF()));
        listTermTag2.sort((t1, t2) -> Integer.compare(t1.getBinDF(), t2.getBinDF()));

//        if(listTermTag1.size() == 1){
//            System.err.print(String.format("%s\t","NotChanged One-Term"));
//            return tagsArr[0]; //One-term Stem
//        }
//        else {
            double val;
            if((val=KendallVal(listTermTag1,listTermTag2,1))-CorrThreshold >=0 ){
                System.err.print(String.format("%s\tKendalVal: %f\t","NotChanged",val));
                return tagsArr[1]; //korelasyon var KStem
            }
            else{
                System.err.print(String.format("%s\tKendalVal: %f\t","Changed",val));
                return tagsArr[0];
            }
//        }

    }

    private static String KendallTauTFOrderBinning(Map<String, ArrayList<TermStats>> tagTermTermStats, String[] tagsArr) {
        ArrayList<TermTFDF> listTermTag1 = new ArrayList<TermTFDF>();
        ArrayList<TermTFDF> listTermTag2 = new ArrayList<TermTFDF>();

        ArrayList<TermStats> tsList = tagTermTermStats.get(tagsArr[0]);
        for (int i = 0; i < tsList.size(); i++) {
            TermTFDF termTFDF = new TermTFDF(i);
            termTFDF.setTF(tsList.get(i).totalTermFreq());
            listTermTag1.add(termTFDF);
        }

        tsList = tagTermTermStats.get(tagsArr[1]);
        for (int i = 0; i < tsList.size(); i++) {
            TermTFDF termTFDF = new TermTFDF(i);
            termTFDF.setTF(tsList.get(i).totalTermFreq());
            listTermTag2.add(termTFDF);
        }

        listTermTag1.sort((t1, t2) -> Integer.compare(t1.getBinTF(), t2.getBinTF()));
        listTermTag2.sort((t1, t2) -> Integer.compare(t1.getBinTF(), t2.getBinTF()));

//        if(listTermTag1.size() == 1){
//            System.err.print(String.format("%s\t","NotChanged One-Term"));
//            return tagsArr[1]; //One-term Stem
//        }
//        else {
            double val;
            if((val=KendallVal(listTermTag1,listTermTag2,1))-CorrThreshold >=0 ){
                System.err.print(String.format("%s\tKendalVal: %f\t","NotChanged",val));
                return tagsArr[1]; //korelasyon var KStem
            }
            else{
                System.err.print(String.format("%s\tKendalVal: %f\t","Changed",val));
                return tagsArr[0];
            }
//        }

    }

    private static String DFOrderBinning(Map<String, ArrayList<TermStats>> tagTermTermStats, String[] tagsArr) {
        ArrayList<TermTFDF> listTermTag1 = new ArrayList<TermTFDF>();
        ArrayList<TermTFDF> listTermTag2 = new ArrayList<TermTFDF>();

        ArrayList<TermStats> tsList = tagTermTermStats.get(tagsArr[0]);
        for (int i = 0; i < tsList.size(); i++) {
            TermTFDF termTFDF = new TermTFDF(i);
            termTFDF.setDF(tsList.get(i).docFreq());
            listTermTag1.add(termTFDF);
        }

        tsList = tagTermTermStats.get(tagsArr[1]);
        for (int i = 0; i < tsList.size(); i++) {
            TermTFDF termTFDF = new TermTFDF(i);
            termTFDF.setDF(tsList.get(i).docFreq());
            listTermTag2.add(termTFDF);
        }

        listTermTag1.sort((t1, t2) -> Integer.compare(t1.getBinDF(), t2.getBinDF()));
        listTermTag2.sort((t1, t2) -> Integer.compare(t1.getBinDF(), t2.getBinDF()));

        boolean orderChanged = false;
        for(int i=0; i<listTermTag1.size(); i++){
            if(listTermTag1.get(i).getIndexID()!= listTermTag2.get(i).getIndexID()){
                orderChanged = true;
                break;
            }
        }
        if(orderChanged){
            System.err.print(String.format("%s\t","Changed"));
            return tagsArr[0]; //NoStem
        }
        else{
            System.err.print(String.format("%s\t","NotChanged"));
            return tagsArr[1];
        }
    }

    private static String TFOrderBinning(Map<String, ArrayList<TermStats>> tagTermTermStats, String[] tagsArr) {
        ArrayList<TermTFDF> listTermTag1 = new ArrayList<TermTFDF>();
        ArrayList<TermTFDF> listTermTag2 = new ArrayList<TermTFDF>();

        ArrayList<TermStats> tsList = tagTermTermStats.get(tagsArr[0]);
        for (int i = 0; i < tsList.size(); i++) {
            TermTFDF termTFDF = new TermTFDF(i);
            termTFDF.setTF(tsList.get(i).totalTermFreq());
            listTermTag1.add(termTFDF);
        }

        tsList = tagTermTermStats.get(tagsArr[1]);
        for (int i = 0; i < tsList.size(); i++) {
            TermTFDF termTFDF = new TermTFDF(i);
            termTFDF.setTF(tsList.get(i).totalTermFreq());
            listTermTag2.add(termTFDF);
        }
        listTermTag1.sort((t1, t2) -> Integer.compare(t1.getBinTF(), t2.getBinTF()));
        listTermTag2.sort((t1, t2) -> Integer.compare(t1.getBinTF(), t2.getBinTF()));

        boolean orderChanged = false;
        for(int i=0; i<listTermTag1.size(); i++){
            if(listTermTag1.get(i).getIndexID()!= listTermTag2.get(i).getIndexID()){
                orderChanged = true;
                break;
            }
        }
        if(orderChanged){
            System.err.print(String.format("%s\t","Changed"));
            return tagsArr[0]; //NoStem
        }
        else{
            System.err.print(String.format("%s\t","NotChanged"));
            return tagsArr[1];
        }
    }

    private static String KendallTauDFOrder(Map<String, ArrayList<TermStats>> tagTermTermStats, String[] tagsArr) {
        ArrayList<TermTFDF> listTermTag1 = new ArrayList<TermTFDF>();
        ArrayList<TermTFDF> listTermTag2 = new ArrayList<TermTFDF>();

        ArrayList<TermStats> tsList = tagTermTermStats.get(tagsArr[0]);
        for (int i = 0; i < tsList.size(); i++) {
            TermTFDF termTFDF = new TermTFDF(i);
            termTFDF.setDF(tsList.get(i).docFreq());
            listTermTag1.add(termTFDF);
        }

        tsList = tagTermTermStats.get(tagsArr[1]);
        for (int i = 0; i < tsList.size(); i++) {
            TermTFDF termTFDF = new TermTFDF(i);
            termTFDF.setDF(tsList.get(i).docFreq());
            listTermTag2.add(termTFDF);
        }

        listTermTag1.sort((t1, t2) -> Long.compare(t1.getDF(), t2.getDF()));
        listTermTag2.sort((t1, t2) -> Long.compare(t1.getDF(), t2.getDF()));

//        if(listTermTag1.size() == 1){
//            System.err.print(String.format("%s\t","NotChanged One-Term"));
//            return tagsArr[1]; //One-term Stem
//        }
//        else {
            double val;
            if((val=KendallVal(listTermTag1,listTermTag2,1))-CorrThreshold >=0 ){
                System.err.print(String.format("%s\tKendalVal: %f\t","NotChanged",val));
                return tagsArr[1]; //korelasyon var KStem
            }
            else{
                System.err.print(String.format("%s\tKendalVal: %f\t","Changed",val));
                return tagsArr[0];
            }
//        }

    }

    private static String KendallTauTFOrder(Map<String, ArrayList<TermStats>> tagTermTermStats, String[] tagsArr) {
        ArrayList<TermTFDF> listTermTag1 = new ArrayList<TermTFDF>();
        ArrayList<TermTFDF> listTermTag2 = new ArrayList<TermTFDF>();

        ArrayList<TermStats> tsList = tagTermTermStats.get(tagsArr[0]);
        for (int i = 0; i < tsList.size(); i++) {
            TermTFDF termTFDF = new TermTFDF(i);
            termTFDF.setTF(tsList.get(i).totalTermFreq());
            listTermTag1.add(termTFDF);
        }

        tsList = tagTermTermStats.get(tagsArr[1]);
        for (int i = 0; i < tsList.size(); i++) {
            TermTFDF termTFDF = new TermTFDF(i);
            termTFDF.setTF(tsList.get(i).totalTermFreq());
            listTermTag2.add(termTFDF);
        }

        listTermTag1.sort((t1, t2) -> Long.compare(t1.getTF(), t2.getTF()));
        listTermTag2.sort((t1, t2) -> Long.compare(t1.getTF(), t2.getTF()));

//        if(listTermTag1.size() == 1){
//            System.err.print(String.format("%s\t","NotChanged One-Term"));
//            return tagsArr[1]; //One-term KStem
//        }
//        else {
            double val;
            if((val=KendallVal(listTermTag1,listTermTag2,1))-CorrThreshold >=0 ){
                System.err.print(String.format("%s\tKendalVal: %f\t","NotChanged",val));
                return tagsArr[1]; //korelasyon var Deigisim yok KStem
            }
            else{
                System.err.print(String.format("%s\tKendalVal: %f\t","Changed",val));
                return tagsArr[0];
            }
//        }
    }

    private static String DFOrder(Map<String, ArrayList<TermStats>> tagTermTermStats, String[] tagsArr) {
        ArrayList<TermTFDF> listTermTag1 = new ArrayList<TermTFDF>();
        ArrayList<TermTFDF> listTermTag2 = new ArrayList<TermTFDF>();

        ArrayList<TermStats> tsList = tagTermTermStats.get(tagsArr[0]);
        for (int i = 0; i < tsList.size(); i++) {
            TermTFDF termTFDF = new TermTFDF(i);
            termTFDF.setDF(tsList.get(i).docFreq());
            listTermTag1.add(termTFDF);
        }

        tsList = tagTermTermStats.get(tagsArr[1]);
        for (int i = 0; i < tsList.size(); i++) {
            TermTFDF termTFDF = new TermTFDF(i);
            termTFDF.setDF(tsList.get(i).docFreq());
            listTermTag2.add(termTFDF);
        }

        listTermTag1.sort((t1, t2) -> Long.compare(t1.getDF(), t2.getDF()));
        listTermTag2.sort((t1, t2) -> Long.compare(t1.getDF(), t2.getDF()));

        boolean orderChanged = false;
        for(int i=0; i<listTermTag1.size(); i++){
            if(listTermTag1.get(i).getIndexID()!= listTermTag2.get(i).getIndexID()){
                orderChanged = true;
                break;
            }
        }
        if(orderChanged){
            System.err.print(String.format("%s\t","Changed")); //print part1
            return tagsArr[0]; //NoStem
        }
        else{
            System.err.print(String.format("%s\t","NotChanged")); //print part1
            return tagsArr[1];
        }
    }

    private static String TFOrder(Map<String, ArrayList<TermStats>> tagTermTermStats, String[] tagsArr) {
        ArrayList<TermTFDF> listTermTag1 = new ArrayList<TermTFDF>();
        ArrayList<TermTFDF> listTermTag2 = new ArrayList<TermTFDF>();

        ArrayList<TermStats> tsList = tagTermTermStats.get(tagsArr[0]);
        for (int i = 0; i < tsList.size(); i++) {
            TermTFDF termTFDF = new TermTFDF(i);
            termTFDF.setTF(tsList.get(i).totalTermFreq());
            listTermTag1.add(termTFDF);
        }

        tsList = tagTermTermStats.get(tagsArr[1]);
        for (int i = 0; i < tsList.size(); i++) {
            TermTFDF termTFDF = new TermTFDF(i);
            termTFDF.setTF(tsList.get(i).totalTermFreq());
            listTermTag2.add(termTFDF);
        }
        listTermTag1.sort((t1, t2) -> Long.compare(t1.getTF(), t2.getTF()));
        listTermTag2.sort((t1, t2) -> Long.compare(t1.getTF(), t2.getTF()));


        boolean orderChanged = false;
        for(int i=0; i<listTermTag1.size(); i++){
            if(listTermTag1.get(i).getIndexID()!= listTermTag2.get(i).getIndexID()){
                orderChanged = true;
                break;
            }
        }
        if(orderChanged){
            System.err.print(String.format("%s\t","Changed"));
            return tagsArr[0]; //NoStem
        }
        else{
            System.err.print(String.format("%s\t","NotChanged"));
            return tagsArr[1];
        }

    }

    /**
        Least specific term due to DF
     **/
    private static String LSTDocFreq(Map<String, ArrayList<TermStats>> tagTermTermStats, String[] tagsArr) {
        ArrayList<TermTFDF> listTermTag1 = new ArrayList<TermTFDF>();
        ArrayList<TermTFDF> listTermTag2 = new ArrayList<TermTFDF>();

        ArrayList<TermStats> tsList = tagTermTermStats.get(tagsArr[0]);
        for (int i = 0; i < tsList.size(); i++) {
            TermTFDF termTFDF = new TermTFDF(i);
            termTFDF.setDF(tsList.get(i).docFreq());
            listTermTag1.add(termTFDF);
        }

        tsList = tagTermTermStats.get(tagsArr[1]);
        for (int i = 0; i < tsList.size(); i++) {
            TermTFDF termTFDF = new TermTFDF(i);
            termTFDF.setDF(tsList.get(i).docFreq());
            listTermTag2.add(termTFDF);
        }

        listTermTag1.sort((t1, t2) -> Long.compare(t2.getDF(), t1.getDF()));
        listTermTag2.sort((t1, t2) -> Long.compare(t2.getDF(), t1.getDF()));

        if (listTermTag1.get(0).getIndexID() != listTermTag2.get(0).getIndexID()) {
            System.err.print(String.format("%s\t","Changed")); //print part1
            return tagsArr[0]; //Nostem
        }
        System.err.print(String.format("%s\t","NotChanged")); //print part1
        return tagsArr[1];

    }

    private static String MSTDocFreq(Map<String, ArrayList<TermStats>> tagTermTermStats, String[] tagsArr) {
        ArrayList<TermTFDF> listTermTag1 = new ArrayList<TermTFDF>();
        ArrayList<TermTFDF> listTermTag2 = new ArrayList<TermTFDF>();

        ArrayList<TermStats> tsList = tagTermTermStats.get(tagsArr[0]);
        for (int i = 0; i < tsList.size(); i++) {
            TermTFDF termTFDF = new TermTFDF(i);
            termTFDF.setDF(tsList.get(i).docFreq());
            listTermTag1.add(termTFDF);
        }

        tsList = tagTermTermStats.get(tagsArr[1]);
        for (int i = 0; i < tsList.size(); i++) {
            TermTFDF termTFDF = new TermTFDF(i);
            termTFDF.setDF(tsList.get(i).docFreq());
            listTermTag2.add(termTFDF);
        }

        listTermTag1.sort((t1, t2) -> Long.compare(t1.getDF(), t2.getDF()));
        listTermTag2.sort((t1, t2) -> Long.compare(t1.getDF(), t2.getDF()));

        if (listTermTag1.get(0).getIndexID() != listTermTag2.get(0).getIndexID()) {
            System.err.print(String.format("%s\t","Changed"));
            return tagsArr[0]; //NoStem
        }
        System.err.print(String.format("%s\t","NotChanged"));
        return tagsArr[1];

    }

    private static String LSTTermFreq(Map<String, ArrayList<TermStats>> tagTermTermStats, String[] tagsArr) {
        ArrayList<TermTFDF> listTermTag1 = new ArrayList<TermTFDF>();
        ArrayList<TermTFDF> listTermTag2 = new ArrayList<TermTFDF>();

        ArrayList<TermStats> tsList = tagTermTermStats.get(tagsArr[0]);
//        System.out.print(tagsArr[0]);
        for (int i = 0; i < tsList.size(); i++) {
            TermTFDF termTFDF = new TermTFDF(i);
            termTFDF.setTF(tsList.get(i).totalTermFreq());
            listTermTag1.add(termTFDF);
//            System.out.print(" "+tsList.get(i).term().utf8ToString());
        }
//        System.out.println();

        tsList = tagTermTermStats.get(tagsArr[1]);
//        System.out.print(tagsArr[1]);
        for (int i = 0; i < tsList.size(); i++) {
            TermTFDF termTFDF = new TermTFDF(i);
            termTFDF.setTF(tsList.get(i).totalTermFreq());
            listTermTag2.add(termTFDF);
//            System.out.print(" "+tsList.get(i).term().utf8ToString());
        }
//        System.out.println();
//        System.out.println("======= Before ======");
//        listTermTag1.stream().forEach(t -> System.out.println(t.indexID + " " + t.getTF()));
//        listTermTag2.stream().forEach(t -> System.out.println(t.indexID + " " + t.getTF()));

        listTermTag1.sort((t1, t2) -> Long.compare(t2.getTF(), t1.getTF()));
        listTermTag2.sort((t1, t2) -> Long.compare(t2.getTF(), t1.getTF()));

//        System.out.println("======= Afer ======");
//        listTermTag1.stream().forEach(t -> System.out.println(t.indexID + " " + t.getTF()));
//        listTermTag2.stream().forEach(t -> System.out.println(t.indexID + " " + t.getTF()));

        if (listTermTag1.get(0).getIndexID() != listTermTag2.get(0).getIndexID()) {
            System.err.print(String.format("%s\t","Changed")); //print part1
            return tagsArr[0]; //Nostem

        }
        System.err.print(String.format("%s\t", "NotChanged")); //print part1
        return tagsArr[1];

    }

    /**
     * This method returns one of the given IndexSearchers due to the changing of the most specific term in term frequency
     * If the most specific term changes in term frequency, the method returns stemmed tag
     */
    private static String MSTTermFreq(Map<String, ArrayList<TermStats>> tagTermTermStats, String[] tagsArr) {
        ArrayList<TermTFDF> listTermTag1 = new ArrayList<TermTFDF>();
        ArrayList<TermTFDF> listTermTag2 = new ArrayList<TermTFDF>();

        ArrayList<TermStats> tsList = tagTermTermStats.get(tagsArr[0]);
//        System.out.print(tagsArr[0]);
        for (int i = 0; i < tsList.size(); i++) {
            TermTFDF termTFDF = new TermTFDF(i);
            termTFDF.setTF(tsList.get(i).totalTermFreq());
            listTermTag1.add(termTFDF);
//            System.out.print(" "+tsList.get(i).term().utf8ToString());
        }
//        System.out.println();

        tsList = tagTermTermStats.get(tagsArr[1]);
//        System.out.print(tagsArr[1]);
        for (int i = 0; i < tsList.size(); i++) {
            TermTFDF termTFDF = new TermTFDF(i);
            termTFDF.setTF(tsList.get(i).totalTermFreq());
            listTermTag2.add(termTFDF);
//            System.out.print(" "+tsList.get(i).term().utf8ToString());
        }
//        System.out.println();
//        System.out.println("======= Before ======");
//        listTermTag1.stream().forEach(t -> System.out.println(t.indexID + " " + t.getTF()));
//        listTermTag2.stream().forEach(t -> System.out.println(t.indexID + " " + t.getTF()));

        listTermTag1.sort((t1, t2) -> Long.compare(t1.getTF(), t2.getTF()));
        listTermTag2.sort((t1, t2) -> Long.compare(t1.getTF(), t2.getTF()));

//        System.out.println("======= Afer ======");
//        listTermTag1.stream().forEach(t -> System.out.println(t.indexID + " " + t.getTF()));
//        listTermTag2.stream().forEach(t -> System.out.println(t.indexID + " " + t.getTF()));

        if (listTermTag1.get(0).getIndexID() != listTermTag2.get(0).getIndexID()) {
            System.err.print(String.format("%s\t","Changed")); //print part1
            return tagsArr[0]; //Nostem

        }
        System.err.print(String.format("%s\t", "NotChanged")); //print part1
        return tagsArr[1];

    }

    private static String CosineSim(Map<String, ArrayList<TermStats>> tagTermTermStats, String[] tagsArr) {
        ArrayList<TermTFDF> listTermTag1 = new ArrayList<TermTFDF>();
        ArrayList<TermTFDF> listTermTag2 = new ArrayList<TermTFDF>();

        ArrayList<TermStats> tsList = tagTermTermStats.get(tagsArr[0]);
        for (int i = 0; i < tsList.size(); i++) {
            TermTFDF termTFDF = new TermTFDF(i);
            termTFDF.setDF(tsList.get(i).docFreq());
            termTFDF.setTF(tsList.get(i).totalTermFreq());
            listTermTag1.add(termTFDF);
        }

        tsList = tagTermTermStats.get(tagsArr[1]);
        for (int i = 0; i < tsList.size(); i++) {
            TermTFDF termTFDF = new TermTFDF(i);
            termTFDF.setDF(tsList.get(i).docFreq());
            termTFDF.setTF(tsList.get(i).totalTermFreq());
            listTermTag2.add(termTFDF);
        }

        double[] v1 = new double[2*listTermTag1.size()];
        double[] v2 = new double[2*listTermTag2.size()];

        double tfSum1=0.0,tfSum2=0.0,dfSum1=0.0,dfSum2=0.0;

        for(int i=0;i<listTermTag1.size();i++){
            v1[i]=listTermTag1.get(i).getDF()/(double)TermTFDF.maxDF;
            dfSum1+=v1[i];
            v1[i+1]=listTermTag1.get(i).getTF()/(double)TermTFDF.maxTF;
            tfSum1+=v1[i+1];
        }

        for(int i=0;i<listTermTag2.size();i++){
            v2[i]=listTermTag2.get(i).getDF()/(double)TermTFDF.maxDF;
            dfSum2+=v2[i];
            v2[i+1]=listTermTag2.get(i).getTF()/(double)TermTFDF.maxTF;
            tfSum2+=v2[i+1];
        }

        double[] v11 = {dfSum1,tfSum1};
        double[] v22 = {dfSum2,tfSum2};

        double cosine_sim = Utils.cosineSim(v11, v22);

        System.err.print(Arrays.deepToString(ArrayUtils.toObject(v11))); //print part1
        System.err.print(Arrays.deepToString(ArrayUtils.toObject(v22))); //print part1


        if(cosine_sim<CorrThreshold){
            System.err.print(String.format("\t%s\t%f\t","NoStem: cosine_sim",cosine_sim)); //print part1
            return tagsArr[0]; //NoStem
        }
        else{
            System.err.print(String.format("\t%s\t%f\t","KStem: cosine_sim",cosine_sim)); //print part1
            return tagsArr[1]; //Stem
        }

    }

    private static String Features(Map<String, ArrayList<TermStats>> tagTermTermStats, String[] tagsArr) {
        ArrayList<TermTFDF> listTermTag1 = new ArrayList<TermTFDF>();
        ArrayList<TermTFDF> listTermTag2 = new ArrayList<TermTFDF>();

        ArrayList<TermStats> tsList = tagTermTermStats.get(tagsArr[0]);
        for (int i = 0; i < tsList.size(); i++) {
            TermTFDF termTFDF = new TermTFDF(i);
            termTFDF.setTF(tsList.get(i).totalTermFreq());
            termTFDF.setDF(tsList.get(i).docFreq());
            listTermTag1.add(termTFDF);
        }

        tsList = tagTermTermStats.get(tagsArr[1]);
        for (int i = 0; i < tsList.size(); i++) {
            TermTFDF termTFDF = new TermTFDF(i);
            termTFDF.setTF(tsList.get(i).totalTermFreq());
            termTFDF.setDF(tsList.get(i).docFreq());
            listTermTag2.add(termTFDF);
        }


        listTermTag1.sort((t1, t2) -> Integer.compare(t1.getBinTF(), t2.getBinTF()));
        listTermTag2.sort((t1, t2) -> Integer.compare(t1.getBinTF(), t2.getBinTF()));
        double valBinTF=KendallVal(listTermTag1,listTermTag2,1);

        listTermTag1.sort((t1, t2) -> Integer.compare(t1.getBinDF(), t2.getBinDF()));
        listTermTag2.sort((t1, t2) -> Integer.compare(t1.getBinDF(), t2.getBinDF()));
        double valBinDF=KendallVal(listTermTag1,listTermTag2,1);

        listTermTag1.sort((t1, t2) -> Long.compare(t1.getTF(), t2.getTF()));
        listTermTag2.sort((t1, t2) -> Long.compare(t1.getTF(), t2.getTF()));
        double valTF=KendallVal(listTermTag1,listTermTag2,1);

        listTermTag1.sort((t1, t2) -> Long.compare(t1.getDF(), t2.getDF()));
        listTermTag2.sort((t1, t2) -> Long.compare(t1.getDF(), t2.getDF()));
        double valDF=KendallVal(listTermTag1,listTermTag2,1);


        String MSTTF = MSTTermFreq(tagTermTermStats, tagsArr);
        String MSTDF = MSTDocFreq(tagTermTermStats, tagsArr);
        String LSTDF = LSTDocFreq(tagTermTermStats, tagsArr);
        String LSTTF = LSTTermFreq(tagTermTermStats, tagsArr);
        String TFOrder = TFOrder(tagTermTermStats, tagsArr);
        String DFOrder = DFOrder(tagTermTermStats, tagsArr);
        String KendallTauTFOrder = KendallTauTFOrder(tagTermTermStats, tagsArr);
        String KendallTauDFOrder = KendallTauDFOrder(tagTermTermStats, tagsArr);
        String MSTTFBinning = MSTTFBinning(tagTermTermStats, tagsArr);
        String MSTDFBinning = MSTDFBinning(tagTermTermStats, tagsArr);
        String TFOrderBinning = TFOrderBinning(tagTermTermStats, tagsArr);
        String DFOrderBinning = DFOrderBinning(tagTermTermStats, tagsArr);
        String KendallTauTFOrderBinning = KendallTauTFOrderBinning(tagTermTermStats, tagsArr);
        String KendallTauDFOrderBinning = KendallTauDFOrderBinning(tagTermTermStats, tagsArr);
        System.err.print(String.format("\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%f\t%s\t%f\t%s\t%s\t%s\t%s\t%s\t%f\t%s\t%f\t",
                MSTTF,MSTDF,LSTDF,LSTTF,TFOrder,DFOrder,KendallTauTFOrder,valTF,KendallTauDFOrder,valDF,MSTTFBinning,MSTDFBinning,
                TFOrderBinning,DFOrderBinning,KendallTauTFOrderBinning,valBinTF,KendallTauDFOrderBinning,valBinDF)); //print part1
        return tagsArr[0];
    }
    /*
    private static String Features(Map<String, ArrayList<TermStats>> tagTermTermStats, String[] tagsArr) {
        ArrayList<TermTFDF> listTermTag1 = new ArrayList<TermTFDF>();
        ArrayList<TermTFDF> listTermTag2 = new ArrayList<TermTFDF>();

        ArrayList<TermStats> tsList = tagTermTermStats.get(tagsArr[0]);
        for (int i = 0; i < tsList.size(); i++) {
            TermTFDF termTFDF = new TermTFDF(i);
            termTFDF.setDF(tsList.get(i).docFreq());
            termTFDF.setTF(tsList.get(i).totalTermFreq());
            listTermTag1.add(termTFDF);
        }

        tsList = tagTermTermStats.get(tagsArr[1]);
        for (int i = 0; i < tsList.size(); i++) {
            TermTFDF termTFDF = new TermTFDF(i);
            termTFDF.setDF(tsList.get(i).docFreq());
            termTFDF.setTF(tsList.get(i).totalTermFreq());
            listTermTag2.add(termTFDF);
        }

        long v1DF[] = new long[listTermTag1.size()];
        long v2DF[] = new long[listTermTag2.size()];
        long v1TF[] = new long[listTermTag1.size()];
        long v2TF[] = new long[listTermTag2.size()];


        long tfSum1=0,tfSum2=0,dfSum1=0,dfSum2=0;

        for(int i=0;i<listTermTag1.size();i++){
            v1DF[i]=listTermTag1.get(i).getDF();
            dfSum1+=v1DF[i];

            v1TF[i]=listTermTag1.get(i).getTF();
            tfSum1+=v1TF[i];
        }

        for(int i=0;i<listTermTag2.size();i++){
            v2DF[i]=listTermTag2.get(i).getDF();
            dfSum2+=v2DF[i];

            v2TF[i]=listTermTag2.get(i).getTF();
            tfSum2+=v2TF[i];
        }

        double v1IDF[] = Arrays.stream(v1DF).mapToDouble(d->Utils.idf(TermTFDF.maxDF,d)).toArray();
        double v2IDF[] = Arrays.stream(v2DF).mapToDouble(d->Utils.idf(TermTFDF.maxDF,d)).toArray();


        int queryLength = listTermTag1.size();
        double cosine_sim = Utils.cosineSim(v1IDF, v2IDF);
        double angular_sim = Utils.angularSim(v1IDF, v2IDF);
        double avgv1IDF = new Aggregate.Average().aggregate(v1IDF);
        double avgv2IDF = new Aggregate.Average().aggregate(v2IDF);

        double harmv1IDF = new Aggregate.HarmonicMean().aggregate(v1IDF);
        double harmv2IDF = new Aggregate.HarmonicMean().aggregate(v2IDF);

        double covv1IDF = new Aggregate.CoVar().aggregate(v1IDF);
        double covv2IDF = new Aggregate.CoVar().aggregate(v2IDF);

        double varv1IDF = new Aggregate.Variance().aggregate(v1IDF);
        double varv2IDF = new Aggregate.Variance().aggregate(v2IDF);

        System.err.print(String.format("\t%d\t%f\t%f\t%f\t%f\t%f\t%f\t%f\t%f\t%f\t%f\t",
                queryLength,cosine_sim,angular_sim,avgv1IDF,avgv2IDF,harmv1IDF,harmv2IDF,covv1IDF,covv2IDF,varv1IDF,varv2IDF)); //print part1
        return tagsArr[0];
    }

*/


    private static double KendallVal(ArrayList<TermTFDF> l1, ArrayList<TermTFDF> l2, int opt){
        double[] v1 = new double[l1.size()];
        double[] v2 = new double[l2.size()];

        if(opt==1){
            for (int i = 0; i < v1.length; ++i) v1[i] = l1.get(i).getIndexID();
            for (int i = 0; i < v2.length; ++i) v2[i] = l2.get(i).getIndexID();
        }
        else if(opt==2) {
            for (int i = 0; i < v1.length; ++i) v1[i] = l1.get(i).getBinDF();
            for (int i = 0; i < v2.length; ++i) v2[i] = l2.get(i).getBinDF();
        }else if(opt==3) {
            for (int i = 0; i < v1.length; ++i) v1[i] = l1.get(i).getBinTF();
            for (int i = 0; i < v2.length; ++i) v2[i] = l2.get(i).getBinTF();
        }
        if(Arrays.equals(v1, v2)) return 1;
        //if(Arrays.stream(v1).allMatch(d -> d == v1[0])) v1[0]=v1[0]-1;
        //if(Arrays.stream(v2).allMatch(d -> d==v2[0])) v2[0]=v2[0]-1;
        KendallsCorrelation corr = new KendallsCorrelation();
        double tau = corr.correlation(v1,v2);
        return tau;
    }
}
