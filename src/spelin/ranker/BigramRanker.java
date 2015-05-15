package spelin.ranker;

import spelin.util.CandidateSuggestion;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Steven on 2015-03-28.
 * interface for
 */
public interface BigramRanker {

    public ArrayList<ArrayList<CandidateSuggestion>> rankBigramCandidates(ArrayList<List<CandidateSuggestion>> suggestions,
                                                                          final ArrayList<CandidateSuggestion> query,
                                                                          boolean runIterative);

    public double score(List<CandidateSuggestion> query, List<CandidateSuggestion> suggestionSequence);

}
