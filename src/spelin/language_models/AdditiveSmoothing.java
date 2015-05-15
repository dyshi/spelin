package spelin.language_models;

import spelin.bigram_dictionary.BigramDictionary;
import spelin.unigram_dictionary.UnigramDictionary;

import java.util.List;

/**
 * Created by Steven on 2015-02-22.
 */
public class AdditiveSmoothing extends AbstractLanguageModel {

    protected int additiveValue;

    public AdditiveSmoothing(int value) {
        this.additiveValue = value;
    }

    @Override
    public double score(List<String> combinationQuery, int index, UnigramDictionary unigramDictionary, BigramDictionary bigramDictionary) {
        String bigram = this.constructBigram(combinationQuery, index);

        long bigramCount = bigramDictionary.getQueryFrequency(bigram);
        long unigramCount = unigramDictionary.getQueryFrequency(combinationQuery.get(index));

        return (double)( bigramCount + additiveValue) / (double) (additiveValue * unigramDictionary.getTotalFrequencyCount() + unigramCount);
    }
}
