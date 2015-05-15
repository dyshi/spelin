package spelin.language_models;

import spelin.bigram_dictionary.BigramDictionary;
import spelin.unigram_dictionary.UnigramDictionary;

import java.util.List;

/**
 * Created by Steven on 2015-02-22.
 */
public interface BigramLanguageModel {

    //Assume index and index +1 both exists
    public double score(List<String> combinationQuery, int index, UnigramDictionary unigramDictionary, BigramDictionary bigramDictionary);
}
