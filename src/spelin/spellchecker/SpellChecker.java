package spelin.spellchecker;

import spelin.bigram_dictionary.BigramDictionary;
import spelin.bigram_dictionary.BigramLuceneDictionary;
import spelin.language_models.BigramLanguageModel;
import spelin.language_models.KneserNeySmoothing;
import spelin.ranker.*;
import spelin.unigram_dictionary.UnigramDictionary;
import spelin.unigram_dictionary.UnigramLuceneDictionary;
import spelin.util.CandidateSuggestion;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Steven on 2015-02-01.
 * This class will be the main function
 *
 */
public class SpellChecker {

    protected String unigramInputDictionaryPath = "combinedNames.csv";
    protected String unigramOutputDirectoryPath = "unigramIndex";
    protected String bigramInputDictionaryPath = "bigram.txt";
    protected String bigramOutputDirectoryPath = "bigramIndex";

    protected UnigramDictionary unigramDictionary;
    protected BigramDictionary bigramDictionary;
    protected BigramLanguageModel languageModel;
    protected UnigramRanker ranker;
    protected BigramRanker bigramRanker;

    //NOTE different components can be substituted here
    public void setup() throws IOException {
        unigramDictionary = new UnigramLuceneDictionary(unigramOutputDirectoryPath, unigramInputDictionaryPath);
        bigramDictionary = new BigramLuceneDictionary(bigramOutputDirectoryPath, bigramInputDictionaryPath);
        languageModel = new KneserNeySmoothing(0.25);
        ranker = new BMUnigramRanker();
        bigramRanker = new BigramRankerImpl(bigramDictionary, languageModel, unigramDictionary);
    }

    public ArrayList<List<CandidateSuggestion>> getRankedUnigramCandidates(ArrayList<CandidateSuggestion> tokenizedQuery) {
        ArrayList<List<CandidateSuggestion>> combinationList = new ArrayList<List<CandidateSuggestion>>();
        for (CandidateSuggestion queryToken : tokenizedQuery) {
            combinationList.add(ranker.rankCandidates(unigramDictionary.generateCandidates(queryToken), queryToken));
        }
        return combinationList;
    }

    public ArrayList<ArrayList<CandidateSuggestion>> correct(ArrayList<CandidateSuggestion> tokenizedQuery) throws IOException {

        ArrayList<List<CandidateSuggestion>> combinationList = new ArrayList<List<CandidateSuggestion>>();
        for (List<CandidateSuggestion> tokenSuggestions : getRankedUnigramCandidates(tokenizedQuery)) {
            //it at least has itself as a suggestion
            if (tokenSuggestions.size() == 1) {
                ArrayList<CandidateSuggestion> splitTokens = considerSpaceSplitting(tokenSuggestions.get(0).candidate);
                ArrayList<List<CandidateSuggestion>> splitCandidates = getRankedUnigramCandidates(splitTokens);
                if (splitCandidates.get(0).size() * splitCandidates.get(1).size() > 1) {
                    combinationList.addAll(splitCandidates);
                } else {
                    combinationList.add(tokenSuggestions);
                }
            } else {
                combinationList.add(tokenSuggestions);
            }
        }

        ArrayList<ArrayList<CandidateSuggestion>> rankedSuggestions = bigramRanker.rankBigramCandidates(combinationList, tokenizedQuery, false);

        for (ArrayList<CandidateSuggestion> suggestion : rankedSuggestions) {
            for (CandidateSuggestion token : suggestion){
                System.out.print(token);
                System.out.print(" ");
                System.out.print(token.score);
            }
            System.out.println(" ");
        }
        return rankedSuggestions;
    }

    public ArrayList<CandidateSuggestion> tokenize(String query) {
        ArrayList<CandidateSuggestion> result = new ArrayList<CandidateSuggestion>();
        for (String token : query.replace("-"," ").split("\\s+")) {
            result.add(new CandidateSuggestion(token));
        }
        return result;
    }

    public ArrayList<CandidateSuggestion> considerSpaceSplitting(String token) {
        ArrayList<CandidateSuggestion> result = new ArrayList<CandidateSuggestion>();
        //we have a bunch of tokens, for each token, we want to tokenize accordingly
        //if this word is not in the dictionary, we should consider splitting as well
        //we are proceeding with the assumption that the dictionary is clean
        if (token.length() < 3) {
            return result;
        }
        double originalScore = this.ranker.scoreOriginal(token);
        double maxScore = originalScore;
        String firstToAdd = null;
        String secondToAdd = null;
        for (int i = 2; i < token.length() - 2; i++) {
            String firstPiece = new String(token.toCharArray(), 0, i);
            String secondPiece = new String(token.toCharArray(), i, token.length() - (i + 1));
            double score = this.ranker.scoreOriginal(firstPiece) * this.ranker.scoreOriginal(secondPiece);
            //if this word is not in the original corpus, AND it is likely to be split
            if (score > maxScore) {
                firstToAdd = firstPiece;
                secondToAdd = secondPiece;
                maxScore = score;
            }
        }
        if (originalScore < maxScore && this.unigramDictionary.getQueryFrequency(token) == 0) {
            result.add(new CandidateSuggestion(firstToAdd));
            result.add(new CandidateSuggestion(secondToAdd));
        }
        return result;
    }

    public void printResults(ArrayList<ArrayList<CandidateSuggestion>> rankedSuggestions) {
        for (ArrayList<CandidateSuggestion> suggestion : rankedSuggestions) {
            for (CandidateSuggestion token : suggestion){
                System.out.print(token.candidate);
                System.out.print(" ");
                System.out.print(String.valueOf(token.score));
            }
            System.out.println(" ");
        }
    }
}
