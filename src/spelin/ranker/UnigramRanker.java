package spelin.ranker;

import spelin.util.CandidateSuggestion;

import java.util.List;

/**
 * Created by Steven on 2015-02-01.
 * This interface must be implemented by any candidate rankers
 */
public interface UnigramRanker {

    public List<CandidateSuggestion> rankCandidates(List<CandidateSuggestion> candidates, final CandidateSuggestion query);

    public double score(CandidateSuggestion query, CandidateSuggestion candidate);

    public double scoreOriginal(String original);
}
