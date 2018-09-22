package edu.anadolu.eval;

/**
 * To hold system-score pairs.
 * Useful for sorting systems(tags) descending by their effectiveness
 */
final public class SystemScore implements Comparable<SystemScore> {

    final public String system;
    final public double score;

    public SystemScore(String system, double score) {
        this.system = system;
        this.score = score;
    }

    public double score() {
        return score;
    }
    public String system() {
        return system;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SystemScore that = (SystemScore) o;

        if (Double.compare(that.score, score) != 0) return false;
        return system != null ? system.equals(that.system) : that.system == null;

    }

    @Override
    public int compareTo(SystemScore o) {
        if (equals(o)) return 0;

        final int doubleCompare = Double.compare(o.score, score);

        if (doubleCompare == 0)
            return system.compareTo(o.system);
        else
            return doubleCompare;
    }

    @Override
    public String toString() {
        return system + "(" + score + ")";
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        hashCode = 31 * hashCode + (system == null ? 0 : system.hashCode());
        hashCode = 31 * hashCode + Double.hashCode(score);
        return hashCode;
    }

}
