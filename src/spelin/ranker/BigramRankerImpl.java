package spelin.ranker;

import spelin.bigram_dictionary.BigramDictionary;
import spelin.language_models.BigramLanguageModel;
import spelin.util.CandidateSuggestion;
import spelin.unigram_dictionary.UnigramDictionary;
import spelin.bigram_dictionary.BigramCandidateGenerator;

import java.util.*;

/**
 * Created by Steven on 2015-02-22.
 */
public class BigramRankerImpl implements BigramRanker {

    private BigramDictionary bigramDictionary;
    private BigramLanguageModel languageModel;
    private UnigramDictionary unigramDictionary;

    public BigramRankerImpl(BigramDictionary bigramDictionary, BigramLanguageModel model, UnigramDictionary unigramDictionary) {
        this.bigramDictionary = bigramDictionary;
        this.unigramDictionary = unigramDictionary;
        this.languageModel = model;
    }

    public ArrayList<ArrayList<CandidateSuggestion>> generateCombination(ArrayList<List<CandidateSuggestion>> suggestions,
                                                                         final ArrayList<CandidateSuggestion> query, boolean runIterative) {
        //recursively generate the combinations
        //start with the empty string
        //We will perform the sort on this
        ArrayList<ArrayList<CandidateSuggestion>> allCombinations = new ArrayList<ArrayList<CandidateSuggestion>>();

        //Iterative mode, we only consider combinations with one error
        if (runIterative) {
            //for each piece of the query
            //we fix every single piece except that i
            for (int i = 0; i < query.size(); i++) {
                ArrayList<ArrayList<CandidateSuggestion>> emptyCombination = new ArrayList<ArrayList<CandidateSuggestion>>();
                emptyCombination.add(new ArrayList<CandidateSuggestion>());
                ArrayList<List<CandidateSuggestion>> suggestionsToUse = new ArrayList<List<CandidateSuggestion>>();
                for (int j = 0; j < i; j++) {
                    ArrayList<CandidateSuggestion> subbedOutPiece = new ArrayList<CandidateSuggestion>();
                    subbedOutPiece.add(query.get(j));
                    suggestionsToUse.add(subbedOutPiece);
                }
                suggestionsToUse.add(suggestions.get(i));

                for (int j = i +1;j < suggestions.size(); j ++) {
                    ArrayList<CandidateSuggestion> subbedOutPiece = new ArrayList<CandidateSuggestion>();
                    subbedOutPiece.add(query.get(j));
                    suggestionsToUse.add(subbedOutPiece);
                }
                allCombinations.addAll(BigramCandidateGenerator.generateCombinations(emptyCombination, suggestionsToUse, 0));
                emptyCombination.clear();
            }
        } else {
            ArrayList<ArrayList<CandidateSuggestion>> emptyCombination = new ArrayList<ArrayList<CandidateSuggestion>>();
            emptyCombination.add(new ArrayList<CandidateSuggestion>());
            allCombinations = BigramCandidateGenerator.generateCombinations(emptyCombination, suggestions, 0);

        }
        return allCombinations;
    }
    //this method is the entry point for the bigram ranker
    //essentially generate all candidates, and then rank them
    public ArrayList<ArrayList<CandidateSuggestion>> rankBigramCandidates(ArrayList<List<CandidateSuggestion>> suggestions,
                                                                          final ArrayList<CandidateSuggestion> query, boolean runIterative) {

        //to enforce max number of changes, we run generate combinations multiple times, each time with certain suggestion lists removed

        ArrayList<ArrayList<CandidateSuggestion>> allCombinations = generateCombination(suggestions, query, runIterative);
        Collections.sort(allCombinations, new Comparator<List<CandidateSuggestion>>() {
            @Override
            public int compare(List<CandidateSuggestion> o1, List<CandidateSuggestion> o2) {
                return -1 * Double.compare(score(query, o1), score(query, o2));
            }
        });
        return allCombinations;
    }

    //assume the markov property on the word and compute the bigram probability as the score
    public double score(List<CandidateSuggestion> query, List<CandidateSuggestion> suggestionSequence) {
        //we piece together bigram scores

        //special case if this is only 1 word
        //then use the unigram score because that is all we have
        if (suggestionSequence.size() == 1) {
            return suggestionSequence.get(0).score;
        }

        double chainedScore = 1.0;
        for (CandidateSuggestion suggestion : suggestionSequence) {
            chainedScore *= suggestion.score;
        }
        return chainedScore;
    }
}
