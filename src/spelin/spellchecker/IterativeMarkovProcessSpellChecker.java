package spelin.spellchecker;

import spelin.util.CandidateSuggestion;

import java.util.*;

/**
 * Created by Steven on 2015-03-28.
 */
public class IterativeMarkovProcessSpellChecker extends SpellChecker {

    //almost exactly like iterative correction
    //except we keep all the candidates generated until the end, and then we sum up

    private static final int ITERATION_MAX = 3;

    //first let's assume it only works with a single token
    public ArrayList<ArrayList<CandidateSuggestion>> correct(ArrayList<CandidateSuggestion> tokenizedQuery) {
        ArrayList<List<CandidateSuggestion>> result = new ArrayList<List<CandidateSuggestion>>();

        for (CandidateSuggestion token : tokenizedQuery) {
            ArrayList<CandidateSuggestion> correctionList = new ArrayList<CandidateSuggestion>();
            correctionList.add(token);
            ArrayList<CandidateSuggestion> resultList;
            ArrayList<CandidateSuggestion> allCandidates = recurseCorrect(correctionList, 0);
            if (allCandidates.size() == 1 && false) {
                ArrayList<CandidateSuggestion> splitTokens = considerSpaceSplitting(token.candidate);
                if (splitTokens.size() > 0) {
                    ArrayList<CandidateSuggestion> firstCorrectionList = new ArrayList<CandidateSuggestion>();
                    firstCorrectionList.add(splitTokens.get(0));
                    ArrayList<CandidateSuggestion> secondCorrectionList = new ArrayList<CandidateSuggestion>();
                    secondCorrectionList.add(splitTokens.get(1));
                    allCandidates = recurseCorrect(firstCorrectionList, 0);
                    result.add(sortedCondensedResults(allCandidates));
                    allCandidates = recurseCorrect(secondCorrectionList, 0);
                }
            }
            //now we want to sum up the scores of the same tokens, as there are multiple ways to get there
            resultList = sortedCondensedResults(allCandidates);
            result.add(resultList);
        }
        return this.bigramRanker.rankBigramCandidates(result, tokenizedQuery, false);
    }

    private ArrayList<CandidateSuggestion> sortedCondensedResults(ArrayList<CandidateSuggestion> allCandidates) {
        HashMap<String, Double> scoreMap = new HashMap<String, Double>();
        for (CandidateSuggestion candidate : allCandidates) {
            if (scoreMap.containsKey(candidate.candidate)) {
                scoreMap.put(candidate.candidate, scoreMap.get(candidate.candidate) + candidate.score);
            } else {
                scoreMap.put(candidate.candidate, candidate.score);
            }
        }

        //at the end, we want a list of the summed up candidates
        ArrayList<CandidateSuggestion> resultList = new ArrayList<CandidateSuggestion>();
        for (String key : scoreMap.keySet()) {
            //we lose the frequency count, but minor issue
            CandidateSuggestion summedCandidate = new CandidateSuggestion(key, 1, scoreMap.get(key));
            resultList.add(summedCandidate);
        }
        Collections.sort(resultList, new Comparator<CandidateSuggestion>() {
            @Override
            public int compare(CandidateSuggestion o1, CandidateSuggestion o2) {
                return -1 * Double.compare(o1.score, o2.score);
            }
        });

        return resultList;
    }

    private ArrayList<CandidateSuggestion> recurseCorrect(ArrayList<CandidateSuggestion> tokenizedQuery, int iteration) {
        ArrayList<CandidateSuggestion> result = new ArrayList<CandidateSuggestion>();

        //stop when we have exceed max iteration, or we have nothing to correct on
        if (iteration == ITERATION_MAX || tokenizedQuery.isEmpty()) {
            return result;
        }

        double[] sourceScores = new double[tokenizedQuery.size()];
        //we have the initial scores
        for (int i =0; i < tokenizedQuery.size(); i++) {
            sourceScores[i] = tokenizedQuery.get(i).score;
        }
        //initially, we have just 1 candidate, we should grow this to a list of candidates
        //
        //we run until convergence, but
        //we retrieve candidates that are already scored and normalized.
        //the only issue is that the original query has also been rescored here, which is ok
        //because we are weighting it by how much it came in with, and this represents the probability of
        //staying in this state
        ArrayList<List<CandidateSuggestion>> allCandidates = getRankedUnigramCandidates(tokenizedQuery);

        for (int i = 0; i < tokenizedQuery.size(); i++) {
            CandidateSuggestion query = tokenizedQuery.get(i);
            //the list of candidates we want to recurse on
            ArrayList<CandidateSuggestion> recurseCandidates = new ArrayList<CandidateSuggestion>();
            for (CandidateSuggestion candidate : allCandidates.get(i)) {
                if (!query.equals(candidate)) {
                    //Here we take into account previous transitions from the original query
                    candidate.score = candidate.score* sourceScores[i];
                    recurseCandidates.add(candidate);
                } else if (query.equals(candidate) && iteration == 0) {
                    //because we start with probability 1.0, we need to rescore the very first one
                    query = candidate;
                }
            }

            //so here we have a list of candidates to further expand
            ArrayList<CandidateSuggestion> recursiveresults = recurseCorrect(recurseCandidates, iteration+ 1);
            recursiveresults.add(query);
            result.addAll(recursiveresults);

        }
        //now we remove from the candidates the original, so that it is not recursed on
        return result;
    }
}
