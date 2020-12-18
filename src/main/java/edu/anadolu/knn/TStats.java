package edu.anadolu.knn;

import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.exception.NoDataException;
import org.apache.commons.math3.exception.NullArgumentException;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.inference.TTest;
import org.apache.commons.math3.stat.ranking.NaNStrategy;
import org.apache.commons.math3.stat.ranking.NaturalRanking;
import org.apache.commons.math3.stat.ranking.TiesStrategy;
import org.apache.commons.math3.util.FastMath;

/**
 * Computes t test statistic for 1-sample t-test.
 */
public class TStats extends TTest {

    /**
     * Difference = sample1 - sample2 = our system - baseline
     *
     * @param sample2 baseline
     * @param sample1 our system
     * @return t statistics
     */
    public double tStats(final double[] sample2, final double[] sample1) {

        ensureDataConformance(sample2, sample1);

        //  z[i] = sample1[i] - sample2[i];
        // difference = our system - baseline

        double meanDifference = StatUtils.meanDifference(sample1, sample2);

        return t(meanDifference, 0,
                StatUtils.varianceDifference(sample1, sample2, meanDifference),
                sample1.length);

        // return FastMath.abs(t);

    }

    public static void main(String[] args) {
        double[] base = {0.437420000000000,0.0578400000000000,0.170620000000000,0.126180000000000,0.316160000000000,0.391270000000000,0.187340000000000,0.523290000000000,0.467530000000000,0.289410000000000,0.110070000000000,0.200910000000000,0.345900000000000,0.0340100000000000,0.411850000000000,0.459460000000000,0.573700000000000,0.334070000000000,0.784990000000000,0.581490000000000,0.866160000000000,0.428020000000000,0.172310000000000,0.551130000000000,0.612220000000000,0.270780000000000,0,0.700400000000000,0.392730000000000,0.151160000000000,0.196010000000000,0.730620000000000,0.292890000000000,0.0523500000000000,0.511680000000000,0.245070000000000,0.589310000000000,0.0859200000000000,0.146120000000000,0.289020000000000,0.408610000000000,0.391090000000000,0.0413400000000000,0.557120000000000,0.0532600000000000,0.380930000000000,0.198420000000000,0.431650000000000,0.535140000000000,0.525600000000000,0.407010000000000,0.672420000000000,0.498910000000000,0.561030000000000,0.225190000000000,0.00595000000000000,0.305380000000000,0.0945800000000000,0.405520000000000,0.611490000000000,0.425160000000000,0,0.0577600000000000,0.330180000000000,0.0773400000000000,0.944830000000000,0.344260000000000,0.0943800000000000,0.458220000000000,0.388960000000000,0.357090000000000,0.466400000000000,0.301410000000000,0.514740000000000,0.303030000000000,0.697030000000000,0.400670000000000,0.412370000000000,0.488560000000000,0.422180000000000,0.741650000000000,0.759650000000000,0.541060000000000,0.808240000000000,0.340120000000000,0.431110000000000,0.296500000000000,0.318840000000000,0.188450000000000,0.0861800000000000,0.662310000000000,0.411770000000000,0.135340000000000,0.577650000000000,0.176110000000000,0.251180000000000,0.291470000000000,0.803880000000000,0.505030000000000,0.346790000000000,0.0612700000000000,0.107270000000000,0.0794300000000000,0.249630000000000,0.312510000000000,0.0214800000000000,0.423680000000000,0.0385700000000000,0.110750000000000,0.296230000000000,0.237240000000000,0.218500000000000,0.00295000000000000,0.408390000000000,0,0.211320000000000,0.176480000000000,0.157140000000000,0.154220000000000,0.263350000000000,0.154880000000000,0.0300200000000000,0.310980000000000,0.0131300000000000,0.0559100000000000,0.297280000000000,0.203400000000000,0,0.0567600000000000,0.0352600000000000,0.134010000000000,0.296600000000000,0.195540000000000,0.0625100000000000,0.332820000000000,0.585190000000000,0.246780000000000,0.386230000000000,0.187780000000000,0.216900000000000,0.225960000000000,0.137640000000000,0.0249100000000000,0.152830000000000,0,0.233660000000000,0.100720000000000,0.106220000000000,0.169540000000000,0.308120000000000,0.306060000000000,0.0489400000000000,0.0582900000000000,0.102960000000000,0.163900000000000,0.0144300000000000,0.286770000000000,0,0.0982800000000000,0.0168600000000000,0.331850000000000,0.381000000000000,0.289340000000000,0.336940000000000,0.117480000000000,0.532510000000000,0.755990000000000,0,0.315080000000000,0.779760000000000,0.224380000000000,0.285820000000000,0,0.231230000000000,0.325620000000000,0.00859000000000000,0.0372900000000000,0.0344500000000000
        };

        double[] run = {0.437420000000000,0.0578400000000000,0.170620000000000,0.123910000000000,0.316160000000000,0.391270000000000,0.187340000000000,0.523290000000000,0.469510000000000,0.289410000000000,0.125830000000000,0.200910000000000,0.345900000000000,0.0181200000000000,0.324440000000000,0.459460000000000,0.613490000000000,0.201630000000000,0.784990000000000,0.581490000000000,0.578710000000000,0.413430000000000,0.172310000000000,0.500970000000000,0.612200000000000,0.270780000000000,0,0.700400000000000,0.446200000000000,0.584310000000000,0.194230000000000,0.730620000000000,0.263440000000000,0.0523500000000000,0.511680000000000,0.250850000000000,0.626720000000000,0.246980000000000,0.146120000000000,0.289020000000000,0.408610000000000,0.424910000000000,0.166680000000000,0.537400000000000,0.0532600000000000,0.380930000000000,0.258210000000000,0.454960000000000,0.564550000000000,0.525600000000000,0.407010000000000,0.515390000000000,0.674390000000000,0.581260000000000,0.0531100000000000,0.292830000000000,0.285330000000000,0.0945800000000000,0.383680000000000,0.652750000000000,0.425160000000000,0,0.0577600000000000,0.279410000000000,0.152240000000000,0.944830000000000,0.344260000000000,0.0943800000000000,0.458220000000000,0.356190000000000,0.357090000000000,0.466400000000000,0.283060000000000,0.514740000000000,0.411790000000000,0.697030000000000,0.321780000000000,0.412370000000000,0.607950000000000,0.410160000000000,0.742750000000000,0.795630000000000,0.656880000000000,0.808240000000000,0.340120000000000,0.439220000000000,0.296500000000000,0.341750000000000,0.246200000000000,0.124270000000000,0.662310000000000,0.411770000000000,0.103490000000000,0.657110000000000,0.169160000000000,0.238000000000000,0.291470000000000,0.803880000000000,0.526590000000000,0.346790000000000,0.0612700000000000,0.0766100000000000,0.0794300000000000,0.236610000000000,0.297750000000000,0.201280000000000,0.374200000000000,0.0385700000000000,0.110750000000000,0.337550000000000,0.264400000000000,0.218500000000000,0.0403500000000000,0.362230000000000,0,0.145170000000000,0.176480000000000,0.157140000000000,0.249330000000000,0.263350000000000,0.154880000000000,0.0300200000000000,0.310980000000000,0.0131300000000000,0.0559100000000000,0.297280000000000,0.203400000000000,0,0.0567600000000000,0.0352600000000000,0.134010000000000,0.296600000000000,0.224760000000000,0.0620500000000000,0.332820000000000,0.569400000000000,0.246780000000000,0.284280000000000,0.187780000000000,0.153140000000000,0.225960000000000,0.137640000000000,0.0917000000000000,0.152830000000000,0.0560500000000000,0.237630000000000,0.100720000000000,0.0786900000000000,0.169540000000000,0.465490000000000,0.324790000000000,0.0489400000000000,0.0637900000000000,0.102960000000000,0.299390000000000,0.0144300000000000,0.359850000000000,0,0.0982800000000000,0.0168600000000000,0.331850000000000,0.447160000000000,0.289340000000000,0.319190000000000,0.150740000000000,0.532510000000000,0.705240000000000,0,0.279550000000000,0.764260000000000,0.224380000000000,0.285820000000000,0,0.231230000000000,0.325620000000000,0.00859000000000000,0.0372900000000000,0.0344500000000000
        };

        for(int i=1;i<11;i++)
            System.out.println(tRisk(base,run,i));
    }

    /**
     * TRisk implementation from
     * <p>
     * Hypothesis testing for the risk-sensitive evaluation of retrieval systems.
     * B. Taner Dincer, Craig Macdonald, and Iadh Ounis. 2014.
     * DOI: https://doi.org/10.1145/2600428.2609625
     *
     * @param base  baseline
     * @param run   run
     * @param alpha risk aversion parameter. Usually 1, 5, or 10.
     * @return TRisk score. Values less than -2 means significant risk, values greater than +2 means significant gain.
     */

    public static double tRisk(final double[] base, final double[] run, double alpha) {

        if (run.length != base.length)
            throw new RuntimeException("array lengths are not equal!");

        int n = base.length;
        double meanDifference = 0d;

        final double[] deltas = new double[base.length];

        for (int i = 0; i < n; i++) {

            double sdiff = run[i] - base[i];

            if (sdiff >= 0)
                deltas[i] = sdiff;
            else
                deltas[i] = (1d + alpha) * sdiff;

            meanDifference += deltas[i];

        }

        // [h,p,~,stats] = ttest(deltas, 0, 0.05, 'both');
        // return stats.tstats;

        meanDifference /= n;

        double sum1 = 0d;
        double sum2 = 0d;

        for (int i = 0; i < n; i++) {
            double diff = deltas[i];
            sum1 += (diff - meanDifference) * (diff - meanDifference);
            sum2 += diff - meanDifference;
        }
        double varianceDifference = (sum1 - (sum2 * sum2 / n)) / (n - 1);

        return meanDifference / Math.sqrt(varianceDifference / n);

    }

    /**
     * URisk implementation from
     * <p>
     * Reducing the risk of query expansion via robust constrained optimization.
     * Kevyn Collins-Thompson. 2009.
     * DOI: https://doi.org/10.1145/1645953.1646059
     *
     * @param base baseline
     * @param run  run
     * @return URisk score.
     */

    public static double URisk(double[] base, double[] run, double alpha) {

        if (run.length != base.length)
            throw new RuntimeException("array lengths are not equal!");

        final double[] win = new double[base.length];
        final double[] loss = new double[base.length];


        for (int i = 0; i < run.length; i++) {
            win[i] = Math.max(0, run[i] - base[i]);
            loss[i] = Math.max(0, base[i] - run[i]);
        }

        double reward = StatUtils.mean(win);
        double risk = StatUtils.mean(loss);

        return reward - (1 + alpha) * risk;


    }

    static double[] calculateDifferences(final double[] x, final double[] y) {

        final double[] z = new double[x.length];

        for (int i = 0; i < x.length; ++i) {
            z[i] = y[i] - x[i];
        }

        return z;
    }

    static double[] calculateAbsoluteDifferences(final double[] z)
            throws NullArgumentException, NoDataException {

        if (z == null) {
            throw new NullArgumentException();
        }

        if (z.length == 0) {
            throw new NoDataException();
        }

        final double[] zAbs = new double[z.length];

        for (int i = 0; i < z.length; ++i) {
            zAbs[i] = FastMath.abs(z[i]);
        }

        return zAbs;
    }

    private final NaturalRanking naturalRanking = new NaturalRanking(NaNStrategy.FIXED, TiesStrategy.AVERAGE);

    double z(final double[] x, final double[] y) {

        ensureDataConformance(x, y);

        // throws IllegalArgumentException if x and y are not correctly specified
        //  z[i] = y[i] - x[i];
        // difference = our system - baseline
        final double[] z = calculateDifferences(x, y);
        final double[] zAbs = calculateAbsoluteDifferences(z);

        final double[] ranks = naturalRanking.rank(zAbs);

        double Wplus = 0;

        for (int i = 0; i < z.length; ++i) {
            if (z[i] > 0) {
                Wplus += ranks[i];
            }
        }

        final int N = x.length;
        // final double Wminus = (((double) (N * (N + 1))) / 2.0) - Wplus;


        final double ES = (double) (N * (N + 1)) / 4.0;

        /* Same as (but saves computations):
         * final double VarW = ((double) (N * (N + 1) * (2*N + 1))) / 24;
         */
        final double VarS = ES * ((double) (2 * N + 1) / 6.0);

        // - 0.5 is a continuity correction
        return (Wplus - ES - 0.5) / FastMath.sqrt(VarS);


    }

    private void ensureDataConformance(final double[] x, final double[] y)
            throws NullArgumentException, NoDataException, DimensionMismatchException {

        if (x == null ||
                y == null) {
            throw new NullArgumentException();
        }
        if (x.length == 0 ||
                y.length == 0) {
            throw new NoDataException();
        }
        if (y.length != x.length) {
            throw new DimensionMismatchException(y.length, x.length);
        }
    }

}
