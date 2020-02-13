package edu.anadolu.stats;

import org.apache.commons.math3.distribution.FDistribution;
import org.apache.commons.math3.stat.StatUtils;

import java.util.*;
import java.util.stream.Collectors;

public class LeveneTest {


    public final double df1;
    /**
     * the degree of freedoms
     */
    public final double df2;

    public final int k;
    /**
     * total number of observations
     */
    public final int N;
    /**
     * the test statistics
     */
    protected volatile double testStatistics;
    /**
     * p-value for the test statistics
     */
    protected volatile double pValue;

    public LeveneTest(List<List<Double>> samples) {
        k = samples.size();

        int tmp = 0;
        for (int i = 0; i < k; ++i) {
            tmp += samples.get(i) != null ? samples.get(i).size() : 0;
        }
        N = tmp;


        //compute the absolute deviations from the center
        final double[] centers = means(samples);
        List<List<Double>> z = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            final int index=i;
            List<Double> absDiffFromMean = samples.get(i).stream().map(j->Math.abs(j-centers[index])).collect(Collectors.toList());
            z.add(i,absDiffFromMean);
        }

        //compute the test statistics
        double totalSum=0.0;
        for(List<Double> l: z){
            totalSum += l.stream().reduce(0.0,Double::sum);
        }
        double zMean = totalSum / N;

        double term1 = 0;
        double term2 = 0;
        for (int i = 0; i < k; ++i) {
            double ziMean = z.get(i).stream().mapToDouble(Double::doubleValue).average().getAsDouble();
            term1 += samples.get(i).size() * Math.pow(ziMean - zMean, 2);

            //double ziVar = StatUtils.variance(z.get(i).stream().mapToDouble(d->d).toArray());
            term2+=z.get(i).stream().mapToDouble(d->Math.pow(d-ziMean,2)).sum();
          //  term2 += (samples.get(i).size() - 1) * ziVar;
        }

        df1 = k - 1;
        df2 = N - k;

        testStatistics = (term1 / term2) * (df2 / df1);

        //compute p-value



    }

    public double getPValue(){
        final FDistribution fdist = new FDistribution(null, df1, df2);
        return 1.0 - fdist.cumulativeProbability(testStatistics);
    }
    public double getLeveneStatistic(){
        return testStatistics;
    }
    public int getDF1(){
        return (int) df1;
    }
    public int getDF2(){
        return (int) df2;
    }





    private double[] means(List<List<Double>> samples) {
        double[] centers = new double[samples.size()];

        for (int i = 0; i < k; ++i)
            centers[i] = samples.get(i).stream().mapToDouble(Double::doubleValue).average().getAsDouble();

        return centers;
    }

    
}
