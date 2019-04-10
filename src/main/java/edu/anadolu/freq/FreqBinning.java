package edu.anadolu.freq;

public class FreqBinning {

    final int numOfBins;
    final long max;

    public FreqBinning(int numOfBins, long max) {
        this.numOfBins = numOfBins;
        this.max = max;
    }

    protected int bin(double freq) {

        return (int) Math.ceil(freq*numOfBins/max);
    }


    public int calculateBinValue(double freq) {
        final int result = bin(freq);

        return result;


    }

    public int numBins() {
        return numOfBins;
    }

}
