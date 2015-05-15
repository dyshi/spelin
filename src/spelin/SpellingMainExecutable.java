package spelin;

import spelin.spellchecker.IterativeMarkovProcessSpellChecker;
import spelin.spellchecker.SpellChecker;
import spelin.util.CandidateSuggestion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Created by Steven on 2015-02-23.
 */
public class SpellingMainExecutable {
    public static void main(String[] args) throws IOException {
        SpellChecker checker = new IterativeMarkovProcessSpellChecker();
        checker.setup();
        //read query
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Please enter query:");
        String query = reader.readLine();
        //tokenize the query and perform spelling suggestions
        ArrayList<CandidateSuggestion> tokenizedQuery = checker.tokenize(query);
        for (CandidateSuggestion suggestion : checker.correct(tokenizedQuery).get(0)) {
            System.out.print(suggestion.candidate);
            System.out.println(suggestion.score);
        }
    }
}
