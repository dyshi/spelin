package spelin.language_models;

import spelin.bigram_dictionary.BigramDictionary;
import spelin.unigram_dictionary.UnigramDictionary;

import java.util.List;

/**
 * load the probability chart
 * Created by Steven on 2015-02-22.
 */
public class GoodTuringSmoothing implements BigramLanguageModel {
    @Override
    public double score(List<String> combinationQuery, int index, UnigramDictionary unigramDictionary, BigramDictionary bigramDictionary) {
        return 0;
    }
}
