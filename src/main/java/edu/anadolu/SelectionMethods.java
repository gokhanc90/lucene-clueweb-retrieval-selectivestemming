package edu.anadolu;

import com.google.common.collect.Ordering;
import edu.anadolu.eval.SystemScore;
import edu.anadolu.freq.FreqBinning;
import edu.anadolu.qpp.Aggregate;
import edu.anadolu.stats.TermStats;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.correlation.KendallsCorrelation;
import org.apache.commons.math3.stat.inference.ChiSquareTest;
import org.apache.commons.math3.stat.inference.TTest;
import org.paukov.combinatorics3.Generator;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * If a change is occurred either order or specific term due to the freq. , select NoStem.
 */
public class SelectionMethods {
    public static double CorrThreshold=0.99; // set by selective stemming tool

    public enum SelectionTag {

        MSTTF, MSTDF, LSTDF, LSTTF, TFOrder, DFOrder, CTIOrder,KendallTauTFOrder, KendallTauDFOrder,MSTTFBinning,MSTDFBinning,
        TFOrderBinning, DFOrderBinning, DFOrderBinningTie, TTestDF, KendallTauTFOrderBinning, KendallTauDFOrderBinning, CosineSim, ChiSqureDF,
        ChiSqureTF, ChiSqureTFIDF,ChiSqureAggDFTF, ComLOS, ComLOD, ComMajor,Features;

        public static SelectionTag tag(String selectionTag) {
           return valueOf(selectionTag);
        }
    }

    public static class TermTFDF {
        public static int NumberOfBIN; // set by selective stemming tool
        public static long maxDF; // set by selective stemming tool
        public static long maxTF; //


        private int indexID;
        private long TF;
        private long DF;
        private int binTF;
        private int binDF;
        private double cti;
        private String term;

        public String getTerm() {
            return term;
        }

        public void setTerm(String term) {
            this.term = term;
        }

        public double getCti() {
            return cti;
        }

        public void setCti(double cti) {
            this.cti = cti;
        }

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
            this.binTF=new FreqBinning(NumberOfBIN,maxTF).calculateBinValue(TF);
        }

        public int getBinDF() {
            return binDF;
        }

        public void setBinDF(long DF) {
            this.binDF=new FreqBinning(NumberOfBIN,maxDF).calculateBinValue(DF);
        }

        @Override
        public String toString() {
            return "TermTFDF{" +
                    "indexID=" + indexID +
                    ", TF=" + TF +
                    ", DF=" + DF +
                    ", binTF=" + binTF +
                    ", binDF=" + binDF +
                    ", cti=" + cti +
                    '}';
        }
    }

    public static String getPredictedTag(String selectionTag,Map<String, ArrayList<TermStats>> tagTermTermStats, String[] tagsArr){
        if(tagTermTermStats.get(tagsArr[0]).size() == 1 && !selectionTag.equals("Features")){
            System.err.print(String.format("%s\t","NotChanged One-Term"));
            return tagsArr[0]; //One-term Stem
        }
        switch (SelectionTag.tag(selectionTag)) {
            case MSTTF: return MSTTermFreq(tagTermTermStats,tagsArr);
            case MSTDF: return MSTDocFreq(tagTermTermStats, tagsArr);
            case LSTDF: return LSTDocFreq(tagTermTermStats, tagsArr);
            case LSTTF: return LSTTermFreq(tagTermTermStats, tagsArr);
            case TFOrder: return TFOrder(tagTermTermStats, tagsArr);
            case DFOrder: return DFOrder(tagTermTermStats, tagsArr);
            case CTIOrder: return CTIOrder(tagTermTermStats, tagsArr);
            case KendallTauTFOrder: return KendallTauTFOrder(tagTermTermStats, tagsArr);
            case KendallTauDFOrder: return KendallTauDFOrder(tagTermTermStats, tagsArr);
            case MSTTFBinning: return MSTTFBinning(tagTermTermStats, tagsArr);
            case MSTDFBinning: return MSTDFBinning(tagTermTermStats, tagsArr);
            case TFOrderBinning: return TFOrderBinning(tagTermTermStats, tagsArr);
            case DFOrderBinning: return DFOrderBinning(tagTermTermStats, tagsArr);
            case DFOrderBinningTie: return DFOrderBinningTie(tagTermTermStats, tagsArr);
            case KendallTauTFOrderBinning: return KendallTauTFOrderBinning(tagTermTermStats, tagsArr);
            case KendallTauDFOrderBinning: return KendallTauDFOrderBinning(tagTermTermStats, tagsArr);
            case ChiSqureDF: return ChiSqureDF(tagTermTermStats, tagsArr);
            case TTestDF: return TTestDF(tagTermTermStats, tagsArr);
            case ComLOS: return ComLOS(tagTermTermStats, tagsArr);
            case ComLOD: return ComLOD(tagTermTermStats, tagsArr);
            case ComMajor: return ComMajor(tagTermTermStats, tagsArr);
            case ChiSqureTF: return ChiSqureTF(tagTermTermStats, tagsArr);
            case ChiSqureTFIDF: return ChiSqureTFIDF(tagTermTermStats, tagsArr);
            case ChiSqureAggDFTF: return ChiSqureAggDFTF(tagTermTermStats, tagsArr);
            case CosineSim: return CosineSim(tagTermTermStats, tagsArr);
            case Features: return Features(tagTermTermStats, tagsArr);

            default: throw new AssertionError(SelectionMethods.class);
        }
    }

    private static String ComMajor(Map<String, ArrayList<TermStats>> tagTermTermStats, String[] tagsArr) {
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
        listTermTag1.sort((t1, t2) -> Integer.compare(t1.getBinDF(), t2.getBinDF()));
        listTermTag2.sort((t1, t2) -> Integer.compare(t1.getBinDF(), t2.getBinDF()));

        if(combinatorics(listTermTag1,listTermTag2,"majoritySame"))  return tagsArr[1]; //stem
        else  return tagsArr[0]; //nostem

    }


    private static String ComLOS(Map<String, ArrayList<TermStats>> tagTermTermStats, String[] tagsArr) {
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
        listTermTag1.sort((t1, t2) -> Integer.compare(t1.getBinDF(), t2.getBinDF()));
        listTermTag2.sort((t1, t2) -> Integer.compare(t1.getBinDF(), t2.getBinDF()));

        if(combinatorics(listTermTag1,listTermTag2,"atLeastOneSame"))  return tagsArr[1]; //stem
        else  return tagsArr[0]; //nostem

    }

    private static String ComLOD(Map<String, ArrayList<TermStats>> tagTermTermStats, String[] tagsArr) {
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
        listTermTag1.sort((t1, t2) -> Integer.compare(t1.getBinDF(), t2.getBinDF()));
        listTermTag2.sort((t1, t2) -> Integer.compare(t1.getBinDF(), t2.getBinDF()));

        if(combinatorics(listTermTag1,listTermTag2,"atLeastOneDiff"))  return tagsArr[0]; //notem
        else  return tagsArr[1]; //stem

    }

    private static String ChiSqureAggDFTF(Map<String, ArrayList<TermStats>> tagTermTermStats, String[] tagsArr) {

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

        long[] obs1DF = listTermTag1.stream().mapToLong(t -> t.DF).toArray();
        long[] obs2DF = listTermTag2.stream().mapToLong(t -> t.DF).toArray();

        long[] obs1TF = listTermTag1.stream().mapToLong(t -> t.TF).toArray();
        long[] obs2TF = listTermTag2.stream().mapToLong(t -> t.TF).toArray();

        long[] obs1 = Arrays.stream(ArrayUtils.addAll(obs1DF, obs1TF)).map(v -> v==0 ? 1:v).toArray();
        long[] obs2 = Arrays.stream(ArrayUtils.addAll(obs2DF, obs2TF)).map(v -> v==0 ? 1:v).toArray();

        ChiSquareTest chi = new ChiSquareTest();
        double pval=chi.chiSquareTestDataSetsComparison(obs1,obs2);
        double ChiS=chi.chiSquareDataSetsComparison(obs1,obs2);
        boolean isSig=chi.chiSquareTestDataSetsComparison(obs1,obs2,0.05);

        //If p_val is lower than 0.05, then two list is significantly different (order change); so return No_Stem
        System.err.print(String.format("pVal: \t%f\t",pval));
        System.err.print(String.format("ChiS: \t%f\t",ChiS));
        if(isSig) return tagsArr[0]; //No_Stem
        return tagsArr[1];
    }

    private static String ChiSqureTFIDF(Map<String, ArrayList<TermStats>> tagTermTermStats, String[] tagsArr) {

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

        double[] obs1IDF = listTermTag1.stream().mapToDouble(t -> Utils.idf(TermTFDF.maxDF, t.DF)).toArray();
        double[] obs2IDF = listTermTag2.stream().mapToDouble(t -> Utils.idf(TermTFDF.maxDF, t.DF)).toArray();

        long[] obs1TF = listTermTag1.stream().mapToLong(t -> t.TF).map(v -> v==0 ? 1:v).toArray();
        long[] obs2TF = listTermTag2.stream().mapToLong(t->t.TF).map(v -> v==0 ? 1:v).toArray();

        long[] obs1 = new long[listTermTag1.size()];
        for(int i=0;i<obs1.length;i++){
            obs1[i]=Math.round(obs1IDF[i] * obs1TF[i]);
        }

        long[] obs2 = new long[listTermTag1.size()];
        for(int i=0;i<obs2.length;i++){
            obs2[i]=Math.round(obs2IDF[i]*obs2TF[i]);
        }

        ChiSquareTest chi = new ChiSquareTest();
        double pval=chi.chiSquareTestDataSetsComparison(obs1,obs2);
        double ChiS=chi.chiSquareDataSetsComparison(obs1,obs2);
        boolean isSig=chi.chiSquareTestDataSetsComparison(obs1,obs2,0.05);

        //If p_val is lower than 0.05, then two list is significantly different (order change); so return No_Stem
        System.err.print(String.format("pVal: \t%f\t",pval));
        System.err.print(String.format("ChiS: \t%f\t",ChiS));
        if(isSig) return tagsArr[0]; //No_Stem
        return tagsArr[1];
    }

    private static String ChiSqureTF(Map<String, ArrayList<TermStats>> tagTermTermStats, String[] tagsArr) {
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

        long[] obs1 = new long[listTermTag1.size()];
        int order=1;
        obs1[0]=order;
        for(int i=1;i<listTermTag1.size();i++) {
            if (listTermTag1.get(i).getBinTF() == listTermTag1.get(i - 1).getBinTF())
                obs1[i] = order;
            else
                obs1[i]=++order;
        }

        long[] obs2 = new long[listTermTag2.size()];
        order=1;
        obs2[0]=order;
        for(int i=1;i<listTermTag2.size();i++) {
            if (listTermTag2.get(i).getBinTF() == listTermTag2.get(i - 1).getBinTF())
                obs2[i] = order;
            else
                obs2[i]=++order;
        }


        ChiSquareTest chi = new ChiSquareTest();
        double pval=chi.chiSquareTestDataSetsComparison(obs1,obs2);
        double ChiS=chi.chiSquareDataSetsComparison(obs1,obs2);
        boolean isSig=chi.chiSquareTestDataSetsComparison(obs1,obs2,0.05);

        //If p_val is lower than 0.05, then two list is significantly different (order change); so return No_Stem
        System.err.print(String.format("pVal: \t%f\t",pval));
        System.err.print(String.format("ChiS: \t%f\t",ChiS));
        if(isSig) return tagsArr[0]; //No_Stem
        return tagsArr[1];
    }

    private static String TTestDF(Map<String, ArrayList<TermStats>> tagTermTermStats, String[] tagsArr) {
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

        listTermTag1.sort((t1, t2) -> Integer.compare(t1.getBinDF(), t2.getBinDF()));
        listTermTag2.sort((t1, t2) -> Integer.compare(t1.getBinDF(), t2.getBinDF()));

        double[] obs1 = new double[listTermTag1.size()];
        int order=1;
        obs1[0]=order;
        for(int i=1;i<listTermTag1.size();i++) {
            if (listTermTag1.get(i).getBinDF() == listTermTag1.get(i - 1).getBinDF())
                obs1[i] = order;
            else
                obs1[i]=++order;
        }

        double[] obs2 = new double[listTermTag2.size()];
        order=1;
        obs2[0]=order;
        for(int i=1;i<listTermTag2.size();i++) {
            if (listTermTag2.get(i).getBinDF() == listTermTag2.get(i - 1).getBinDF())
                obs2[i] = order;
            else
                obs2[i]=++order;
        }

        //long[] obs1 = listTermTag1.stream().mapToLong(t -> t.getIndexID()+1).toArray();
        //long[] obs2 = listTermTag2.stream().mapToLong(t -> t.getIndexID()+1).toArray();
        TTest tTest = new TTest();
        boolean isSig = tTest.pairedTTest(obs1, obs2, 0.05);

        if(isSig) return tagsArr[0]; //No_Stem
        return tagsArr[1];

    }


    private static String ChiSqureDF(Map<String, ArrayList<TermStats>> tagTermTermStats, String[] tagsArr) {
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

        listTermTag1.sort((t1, t2) -> Integer.compare(t1.getBinDF(), t2.getBinDF()));
        listTermTag2.sort((t1, t2) -> Integer.compare(t1.getBinDF(), t2.getBinDF()));

        long[] obs1 = new long[listTermTag1.size()];
        int order=1;
        obs1[0]=order;
        for(int i=1;i<listTermTag1.size();i++) {
            if (listTermTag1.get(i).getBinDF() == listTermTag1.get(i - 1).getBinDF())
                obs1[i] = order;
            else
                obs1[i]=++order;
        }

        long[] obs2 = new long[listTermTag2.size()];
        order=1;
        obs2[0]=order;
        for(int i=1;i<listTermTag2.size();i++) {
            if (listTermTag2.get(i).getBinDF() == listTermTag2.get(i - 1).getBinDF())
                obs2[i] = order;
            else
                obs2[i]=++order;
        }

        //long[] obs1 = listTermTag1.stream().mapToLong(t -> t.getIndexID()+1).toArray();
        //long[] obs2 = listTermTag2.stream().mapToLong(t -> t.getIndexID()+1).toArray();

        ChiSquareTest chi = new ChiSquareTest();
        double pval=chi.chiSquareTestDataSetsComparison(obs1,obs2);
        double ChiS=chi.chiSquareDataSetsComparison(obs1,obs2);
        boolean isSig=chi.chiSquareTestDataSetsComparison(obs1,obs2,0.05);

        //If p_val is lower than 0.05, then two list is significantly different (order change); so return No_Stem
        System.err.print(Arrays.toString(obs1)+"\t"+Arrays.toString(obs2)+"\t");
        System.err.print(String.format("pVal: \t%f\t",pval));
        System.err.print(String.format("ChiS: \t%f\t",ChiS));
        if(isSig) return tagsArr[0]; //No_Stem
        return tagsArr[1];
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
            if((val=KendallVal(listTermTag1,listTermTag2,4))-CorrThreshold >=0 ){
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
            if((val=KendallVal(listTermTag1,listTermTag2,5))-CorrThreshold >=0 ){
                System.err.print(String.format("%s\tKendalVal: %f\t","NotChanged",val));
                return tagsArr[1]; //korelasyon var KStem
            }
            else{
                System.err.print(String.format("%s\tKendalVal: %f\t","Changed",val));
                return tagsArr[0];
            }
//        }

    }

    private static String DFOrderBinningTie(Map<String, ArrayList<TermStats>> tagTermTermStats, String[] tagsArr) {
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

        boolean tie=false;
        for(int i=1; i<listTermTag1.size(); i++){
            if(listTermTag1.get(i-1).getBinDF()== listTermTag1.get(i).getBinDF()){
                tie=true;
                break;
            }
        }

        if(!tie) {
            for (int i = 1; i < listTermTag2.size(); i++) {
                if (listTermTag2.get(i-1).getBinDF() == listTermTag2.get(i).getBinDF()) {
                    tie = true;
                    break;
                }
            }
        }


        System.err.print(String.format("Tie_%s\t",tie));


        boolean isequal=compareTie(listTermTag1,listTermTag2);
        if(isequal){
            System.err.print(String.format("%s\t","NotChanged"));
            return tagsArr[1];
        }
        else{
            System.err.print(String.format("%s\t","Changed"));
            return tagsArr[0]; //NoStem
        }

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

        boolean tie=false;
        for(int i=1; i<listTermTag1.size(); i++){
            if(listTermTag1.get(i-1).getBinDF()== listTermTag1.get(i).getBinDF()){
                tie=true;
                break;
            }
        }

        if(!tie) {
            for (int i = 1; i < listTermTag2.size(); i++) {
                if (listTermTag2.get(i-1).getBinDF() == listTermTag2.get(i).getBinDF()) {
                    tie = true;
                    break;
                }
            }
        }


        System.err.print(String.format("Tie_%s\t",tie));


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

    private static String CTIOrder(Map<String, ArrayList<TermStats>> tagTermTermStats, String[] tagsArr) {
        ArrayList<TermTFDF> listTermTag1 = new ArrayList<TermTFDF>();
        ArrayList<TermTFDF> listTermTag2 = new ArrayList<TermTFDF>();

        ArrayList<TermStats> tsList = tagTermTermStats.get(tagsArr[0]);
        for (int i = 0; i < tsList.size(); i++) {
            TermTFDF termTFDF = new TermTFDF(i);
            termTFDF.setCti(tsList.get(i).cti());
            listTermTag1.add(termTFDF);
        }

        tsList = tagTermTermStats.get(tagsArr[1]);
        for (int i = 0; i < tsList.size(); i++) {
            TermTFDF termTFDF = new TermTFDF(i);
            termTFDF.setCti(tsList.get(i).cti());
            listTermTag2.add(termTFDF);
        }

        listTermTag1.sort((t1, t2) -> Double.compare(t1.getCti(), t2.getCti()));
        listTermTag2.sort((t1, t2) -> Double.compare(t1.getCti(), t2.getCti()));

        System.err.printf("%s\t",listTermTag1.toString());
        System.err.printf("%s\t",listTermTag2.toString());

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

        listTermTag1.sort((t1, t2) -> Integer.compare(t1.getBinDF(), t2.getBinDF()));
        listTermTag2.sort((t1, t2) -> Integer.compare(t1.getBinDF(), t2.getBinDF()));

        double[] obs1 = new double[listTermTag1.size()];
        int order=1;
        obs1[0]=order;
        for(int i=1;i<listTermTag1.size();i++) {
            if (listTermTag1.get(i).getBinDF() == listTermTag1.get(i - 1).getBinDF())
                obs1[i] = order;
            else
                obs1[i]=++order;
        }

        double[] obs2 = new double[listTermTag2.size()];
        order=1;
        obs2[0]=order;
        for(int i=1;i<listTermTag2.size();i++) {
            if (listTermTag2.get(i).getBinDF() == listTermTag2.get(i - 1).getBinDF())
                obs2[i] = order;
            else
                obs2[i]=++order;
        }


        double cosine_sim = Utils.cosineSim(obs1, obs2);

        System.err.print(Arrays.deepToString(ArrayUtils.toObject(obs1))); //print part1
        System.err.print(Arrays.deepToString(ArrayUtils.toObject(obs2))); //print part1


        if(cosine_sim<CorrThreshold){
            System.err.print(String.format("\t%s\t%f\t","NoStem: cosine_sim",cosine_sim)); //print part1
            return tagsArr[0]; //NoStem
        }
        else{
            System.err.print(String.format("\t%s\t%f\t","Stem: cosine_sim",cosine_sim)); //print part1
            return tagsArr[1]; //Stem
        }

    }

    /**
     * Chi Square Test Features
     * @param tagTermTermStats
     * @param tagsArr
     * @return
     */
    private static String Features(Map<String, ArrayList<TermStats>> tagTermTermStats, String[] tagsArr) {
        //String ChiSqureAggDFTF = ChiSqureAggDFTF(tagTermTermStats, tagsArr);
        //String ChiSqureDF = ChiSqureDF(tagTermTermStats, tagsArr);
        //String ChiSqureTF = ChiSqureTF(tagTermTermStats, tagsArr);

        ArrayList<TermTFDF> listTermTag1 = new ArrayList<TermTFDF>();
        ArrayList<TermTFDF> listTermTag2 = new ArrayList<TermTFDF>();

        ArrayList<TermStats> tsList = tagTermTermStats.get(tagsArr[0]);
        for (int i = 0; i < tsList.size(); i++) {
            TermTFDF termTFDF = new TermTFDF(i);
            termTFDF.setCti(tsList.get(i).cti());
            termTFDF.setDF(tsList.get(i).docFreq());
            termTFDF.setTF(tsList.get(i).totalTermFreq());
            termTFDF.setTerm(tsList.get(i).term().utf8ToString());
            listTermTag1.add(termTFDF);
        }

        tsList = tagTermTermStats.get(tagsArr[1]);
        for (int i = 0; i < tsList.size(); i++) {
            TermTFDF termTFDF = new TermTFDF(i);
            termTFDF.setCti(tsList.get(i).cti());
            termTFDF.setDF(tsList.get(i).docFreq());
            termTFDF.setTF(tsList.get(i).totalTermFreq());
            termTFDF.setTerm(tsList.get(i).term().utf8ToString());
            listTermTag2.add(termTFDF);
        }

        double[] idfN = listTermTag1.stream().mapToDouble(t -> Utils.idf(TermTFDF.maxDF, t.DF)).toArray();
        double[] idfS = listTermTag2.stream().mapToDouble(t -> Utils.idf(TermTFDF.maxDF, t.DF)).toArray();



        double ictfN=Utils.ictf(listTermTag1.stream().mapToLong(t -> t.TF).toArray(),TermTFDF.maxTF);
        double ictfS=Utils.ictf(listTermTag2.stream().mapToLong(t -> t.TF).toArray(),TermTFDF.maxTF);

        double[] scqN = listTermTag1.stream().mapToDouble(t -> Utils.scq(TermTFDF.maxDF, t.DF, t.TF)).toArray();
        double[] scqS = listTermTag2.stream().mapToDouble(t -> Utils.scq(TermTFDF.maxDF, t.DF, t.TF)).toArray();

        double[] ctiN = listTermTag1.stream().mapToDouble(t -> t.cti).toArray();
        double[] ctiS = listTermTag2.stream().mapToDouble(t -> t.cti).toArray();


        //Collections.frequency(terms, term) / terms.size()
        List<String> l1Terms=listTermTag1.stream().map(t->t.term).collect(Collectors.toList());
        List<String> l2Terms=listTermTag2.stream().map(t->t.term).collect(Collectors.toList());

        double[] qtfsN = listTermTag1.stream().mapToDouble(t ->(double) Collections.frequency(l1Terms,t.term)/l1Terms.size()).toArray();
        double[] qtfsS = listTermTag2.stream().mapToDouble(t ->(double) Collections.frequency(l2Terms,t.term)/l2Terms.size()).toArray();

        double[] ctfsN = listTermTag1.stream().mapToDouble(t ->(double) t.TF/TermTFDF.maxTF).toArray();
        double[] ctfsS = listTermTag2.stream().mapToDouble(t ->(double) t.TF/TermTFDF.maxTF).toArray();


        double scsN= Utils.scs(qtfsN,ctfsN);
        double scsS= Utils.scs(qtfsS,ctfsS);



//        String MSTTermFreq =MSTTermFreq(tagTermTermStats,tagsArr);
//        String MSTDocFreq =MSTDocFreq(tagTermTermStats, tagsArr);
//        String LSTDocFreq =LSTDocFreq(tagTermTermStats, tagsArr);
//        String LSTTermFreq =LSTTermFreq(tagTermTermStats, tagsArr);
//        String TFOrder =TFOrder(tagTermTermStats, tagsArr);
//        String DFOrder =DFOrder(tagTermTermStats, tagsArr);
//        String CTIOrder =CTIOrder(tagTermTermStats, tagsArr);



        //String ChiSqureTFIDF = ChiSqureTFIDF(tagTermTermStats, tagsArr);
        System.err.print(String.format("\t\t%f\t%f\t%f\t%f\t%f\t%f\t%f\t%f\t%f\t" +
                        "%f\t%f\t%f\t%f\t%f\t%f\t%f\t%f\t%f\t" +
                        "%f\t%f\t%f\t%f\t%f\t%f\t%f\t%f\t%f\t" +
                        "%f\t%f\t%f\t%f\t%f\t%f\t%f\t%f\t%f\t\t",

                new Aggregate.Variance().aggregate(idfN),
                new Aggregate.Variance().aggregate(idfS),
                new Aggregate.Gamma1().aggregate(idfN),
                new Aggregate.Gamma1().aggregate(idfS),
                new Aggregate.Gamma2().aggregate(idfN),
                new Aggregate.Gamma2().aggregate(idfS),
                new Aggregate.Maximum().aggregate(idfN),
                new Aggregate.Maximum().aggregate(idfS),
                new Aggregate.Minimum().aggregate(idfN),
                new Aggregate.Minimum().aggregate(idfS),
                new Aggregate.Average().aggregate(idfN),
                new Aggregate.Average().aggregate(idfS),
                new Aggregate.GeometricMean().aggregate(idfN),
                new Aggregate.GeometricMean().aggregate(idfS),
                new Aggregate.HarmonicMean().aggregate(idfN),
                new Aggregate.HarmonicMean().aggregate(idfS),

                new Aggregate.Variance().aggregate(ctiN),
                new Aggregate.Variance().aggregate(ctiS),
                new Aggregate.Gamma1().aggregate(ctiN),
                new Aggregate.Gamma1().aggregate(ctiS),
                new Aggregate.Gamma2().aggregate(ctiN),
                new Aggregate.Gamma2().aggregate(ctiS),
                new Aggregate.Maximum().aggregate(ctiN),
                new Aggregate.Maximum().aggregate(ctiS),
                new Aggregate.Minimum().aggregate(ctiN),
                new Aggregate.Minimum().aggregate(ctiS),
                new Aggregate.Average().aggregate(ctiN),
                new Aggregate.Average().aggregate(ctiS),
                new Aggregate.GeometricMean().aggregate(ctiN),
                new Aggregate.GeometricMean().aggregate(ctiS),
                new Aggregate.HarmonicMean().aggregate(ctiN),
                new Aggregate.HarmonicMean().aggregate(ctiS),

                ictfN/listTermTag1.size(),
                ictfS/listTermTag1.size(),

                scsN/listTermTag1.size(),
                scsS/listTermTag1.size()

                )
        );
        return tagsArr[0];
    }
/*
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

        if(opt==4){
            int order=1;
            v1[0]=order;
            for(int i=1;i<l1.size();i++) {
                if (l1.get(i).getBinDF() == l1.get(i - 1).getBinDF())
                    v1[i] = order;
                else
                    v1[i]=++order;
            }

            order=1;
            v2[0]=order;
            for(int i=1;i<l2.size();i++) {
                if (l2.get(i).getBinDF() == l2.get(i - 1).getBinDF())
                    v2[i] = order;
                else
                    v2[i]=++order;
            }
        }

        if(opt==5){
            int order=1;
            v1[0]=order;
            for(int i=1;i<l1.size();i++) {
                if (l1.get(i).getBinTF() == l1.get(i - 1).getBinTF())
                    v1[i] = order;
                else
                    v1[i]=++order;
            }

            order=1;
            v2[0]=order;
            for(int i=1;i<l2.size();i++) {
                if (l2.get(i).getBinTF() == l2.get(i - 1).getBinTF())
                    v2[i] = order;
                else
                    v2[i]=++order;
            }
        }

        if(Arrays.equals(v1, v2)) return 1;
        //if(Arrays.stream(v1).allMatch(d -> d == v1[0])) v1[0]=v1[0]-1;
        //if(Arrays.stream(v2).allMatch(d -> d==v2[0])) v2[0]=v2[0]-1;
        KendallsCorrelation corr = new KendallsCorrelation();
        double tau = corr.correlation(v1,v2);
        return tau;
    }
    static boolean  compareTie(ArrayList<SelectionMethods.TermTFDF> l1, ArrayList<SelectionMethods.TermTFDF> l2){

        for(int i=0;i<l1.size();i++){
            int p1=i,p2=i;
            int s1=1,s2=1;
            while (true){
                if(l1.get(p1).getIndexID() == l2.get(p2).getIndexID()){
                    p1++;
                    p2++;
                    break;
                }else {
                    if(p1==l1.size()-1 || p2==l2.size()-1 ) return false;

                    if (l1.get(p1).getBinDF() == l1.get(p1 + s1).getBinDF()) {
                        Collections.swap(l1, p1, p1 + s1);
                        s1++;
                    } else if (l2.get(p2).getBinDF() == l2.get(p2 + s2).getBinDF()) {
                        Collections.swap(l2, p2, p2 + s2);
                        s2++;
                    }
                    else return false;
                }
            }
            if(i==l1.size()-1) return true;
        }
        return false;
    }

    private static boolean combinatorics(ArrayList<SelectionMethods.TermTFDF> listTermTag1, ArrayList<SelectionMethods.TermTFDF> listTermTag2, String method){
        List<List<SelectionMethods.TermTFDF>>perm1= Generator.permutation(listTermTag1)
                .simple()
                .stream()
                .collect(toList());

        List<List<SelectionMethods.TermTFDF>>perm2=Generator.permutation(listTermTag2)
                .simple()
                .stream()
                .collect(toList());


        Iterator<List<SelectionMethods.TermTFDF>> it = perm1.iterator();

        while(it.hasNext()) {
            List<SelectionMethods.TermTFDF> l = it.next();
            if(!Ordering.from(Comparator.comparing(SelectionMethods.TermTFDF::getBinDF)).isOrdered(l))
                it.remove();
        }

        it = perm2.iterator();

        while(it.hasNext()) {
            List<SelectionMethods.TermTFDF> l = it.next();
            if(!Ordering.from(Comparator.comparing(SelectionMethods.TermTFDF::getBinDF)).isOrdered(l))
                it.remove();
        }

        List<List<List<SelectionMethods.TermTFDF>>> cartesianProduct = Generator.cartesianProduct(perm1,perm2).stream().collect(toList());

        int same=0,dif=0, m=cartesianProduct.size();
        for (List<List<SelectionMethods.TermTFDF>> matches: cartesianProduct) {
            boolean orderChanged = false;
            for(int i=0; i<matches.get(0).size(); i++){
                if(matches.get(0).get(i).getIndexID()!= matches.get(1).get(i).getIndexID()){
                    orderChanged = true;
                    break;
                }
            }
            if (orderChanged) dif++;
            else same++;


        }
        System.err.print("\tSame:\t"+same+"\tdiff:\t"+dif+"\tmatches:\t"+m+"\t");
        if("atLeastOneSame".equals(method)){
            if(same>0) return true;
            return false;
        }else if("majoritySame".equals(method)){
            if(same>=dif) return true;
            return false;
        }else if("atLeastOneDiff".equals(method)){
            if(dif>0) return true;
            return false;
        }
        throw new RuntimeException("invalid method name");
    }

}
