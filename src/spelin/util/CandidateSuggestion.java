package spelin.util;

/**
 * This class will carry relevant information regarding a candidate
 */
public class CandidateSuggestion {
    public String candidate;
    public long frequency;
    public double score;
    public long totalFrequency;

    public CandidateSuggestion(String candidate) {
        this.candidate = candidate;
        this.frequency = 1;
        this.score = 1.0;
        this.totalFrequency = 1;
    }

    public CandidateSuggestion(String candidate, long freq, long totalFrequency) {
        this.candidate = candidate;
        this.frequency = freq;
        this.totalFrequency = totalFrequency;
        this.score = 1;
    }

    public CandidateSuggestion(String candidate, long freq, double score) {
        this.candidate = candidate;
        this.frequency = freq;
        this.score = score;
        this.totalFrequency = frequency;
    }

    public boolean equals(CandidateSuggestion other) {
        return this.candidate.equalsIgnoreCase(other.candidate);
    }

    public CandidateSuggestion(CandidateSuggestion other) {
        this.candidate = other.candidate;
        this.score = other.score;
        this.frequency = other.frequency;
        this.totalFrequency = other.totalFrequency;
    }
    @Override
    public String toString() {
        return this.candidate;
    }
}
