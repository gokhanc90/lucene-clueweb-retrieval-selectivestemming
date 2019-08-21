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
        double[] base = {0.3, 0.4893, 0.2473, 0.1209, 0.2798, 0.3298, 0.0928, 0.3043, 0.2738, 0.0765, 0.2028, 0.3983, 0.2616, 0.4883, 0.5045, 0.4587, 0.0671, 0.3922, 0.465, 0.3378, 0.0054, 0.7056, 0.2582, 0.2862, 0.0923, 0.2706, 0.6981, 0.4066, 0.0502, 0.575, 0.3469, 0.4806, 0.1063, 0.2456, 0.1672, 0.1206, 0.1479, 0.4292, 0.0281, 0.2959, 0.0207, 0.1833, 0.2596, 0.2815, 0.2805, 0.3937, 0.5535, 0.4365, 0.2578, 0.1389, 0.2201, 0.3659, 0.0324, 0.547, 0.0325, 0.4979, 0.1321, 0.317, 0.2054, 0.0339, 0.2448, 0.1407, 0.3598, 0.2939, 0.0671, 0.018, 0.488, 0.0404, 0.3921, 0.3669, 0.2925, 0.294, 0.2694, 0.2311, 0.2119, 0.3183, 0.2878, 0.305, 0.4136, 0.224, 0.3226, 0.4511, 0.2079, 0.4876, 0.1312, 0.332, 0.3169, 0.3742, 0.2484, 0.3362, 0.1969, 0.4611, 0.3258, 0.0816, 0.2961, 0.28, 0.2736, 0.1855, 0.3698, 0.5776, 0.2851
        };
        double[] run = {0.3419, 0.4936, 0.2475, 0.1435, 0.2489, 0.187, 0.1292, 0.2761, 0.1039, 0.0874, 0.4169, 0.2443, 0.262, 0.4935, 0.017, 0.4456, 0.0461, 0.3143, 0.4786, 0.2792, 0.0075, 0.1192, 0.3658, 0.2864, 0.2363, 0.2989, 0.6917, 0.3596, 0.0466, 0.5726, 0.3782, 0.4911, 0.0518, 0.2766, 0.1669, 0.3143, 0.1517, 0.4616, 0.4394, 0.2573, 0.0175, 0.0672, 0.5571, 0.3493, 0.2934, 0.428, 0.5533, 0.4428, 0.3029, 0.2869, 0.0113, 0.3177, 0.0192, 0.5824, 0.0313, 0.4632, 0.2194, 0.413, 0.2085, 0.0376, 0.2349, 0.1477, 0.3001, 0.3181, 0.0311, 0.0323, 0.4879, 0.0409, 0.4079, 0.3954, 0.4473, 0.3194, 0.2128, 0.235, 0.0071, 0.3019, 0.4119, 0.4578, 0.385, 0.2214, 0.3271, 0.4671, 0.2606, 0.4187, 0.1626, 0.3216, 0.3209, 0.3822, 0.2423, 0.3742, 0.2188, 0.4628, 0.3233, 0.1599, 0.4216, 0.2709, 0.0719, 0.3016, 0.4448, 0.4857, 0.2867
        };
        for(int i=0;i<11;i++)
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
