package spelin.spellchecker;

import spelin.util.CandidateSuggestion;

import java.util.ArrayList;
import java.util.List;

/**
 * spellchecker as proposed by Cucerzan and Brill, does correction 1 error at a time
 * each new candidate is exactly 1 edit distance away from before
 * Created by Steven on 2015-02-23.
 */
public class IterativeSpellChecker extends SpellChecker {

    private static final int MAX_ITER = 3;

    public ArrayList<ArrayList<CandidateSuggestion>> correct(ArrayList<CandidateSuggestion> tokenizedQuery) {
        //since we correct one error at a time, we decide when to stop
        boolean continueChecking = true;
        ArrayList<ArrayList<CandidateSuggestion>> rankedSuggestions = new ArrayList<ArrayList<CandidateSuggestion>>();
        int iterations = 0;
        while (continueChecking) {
            ArrayList<List<CandidateSuggestion>> combinationList = getRankedUnigramCandidates(tokenizedQuery);
            rankedSuggestions = bigramRanker.rankBigramCandidates(combinationList, tokenizedQuery, true);
            if (equivalent(rankedSuggestions.get(0), tokenizedQuery)) {
                continueChecking = false;
            } else {
                tokenizedQuery = rankedSuggestions.get(0);
            }
            if (iterations > MAX_ITER) {
                break;
            }
            iterations ++;
        }


        return rankedSuggestions;
    }

    private boolean equivalent(List<CandidateSuggestion> first, List<CandidateSuggestion> second) {
        boolean result = true;
        for (int i = 0; i < Math.min(first.size(), second.size()); i++) {
            if (!first.get(i).equals(second.get(i))) {
                result = false;
                break;
            }
        }
        return result && (first.size() == second.size());
    }
}