package spelin.bigram_dictionary;

import spelin.util.CandidateSuggestion;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Steven on 2015-02-23.
 */
public class BigramCandidateGenerator {
    //method will recursively expand all possible combinations of unigram candidates to form a bunch of bigram sequences
    //will find the inorder combinations of the given suggestion lists
    public static  ArrayList<ArrayList<CandidateSuggestion>> generateCombinations(ArrayList<ArrayList<CandidateSuggestion>> combinationSoFar, ArrayList<List<CandidateSuggestion>> allSuggestions, int nextToken) {
        //check if there are no more tokens to consider, we can stop
        if (nextToken < allSuggestions.size()) {
            //new combinations this new token would spawn
            ArrayList<ArrayList<CandidateSuggestion>> newCombinations = new ArrayList<ArrayList<CandidateSuggestion>>();
            for (ArrayList<CandidateSuggestion> combination : combinationSoFar) {
                for (CandidateSuggestion suggestion : allSuggestions.get(nextToken)) {
                    ArrayList<CandidateSuggestion> newCombination = new ArrayList<CandidateSuggestion>(combination);
                    newCombination.add(suggestion);
                    newCombinations.add(newCombination);
                }
            }
            nextToken ++;
            return generateCombinations(newCombinations, allSuggestions, nextToken);
        } else {
            return combinationSoFar;
        }
    }
}
