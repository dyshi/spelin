package spelin.language_models;

import spelin.bigram_dictionary.BigramDictionary;
import spelin.unigram_dictionary.UnigramDictionary;

import java.util.List;

/**
 * Created by Steven on 2015-02-22.
 */
public class BackoffSmoothing extends AbstractLanguageModel{

    private double alpha;

    public BackoffSmoothing(double alpha) {
        this.alpha = alpha;
    }
    @Override
    public double score(List<String> combinationQuery, int index, UnigramDictionary unigramDictionary, BigramDictionary bigramDictionary) {
        String bigram = this.constructBigram(combinationQuery, index);

        long bigramCount = bigramDictionary.getQueryFrequency(bigram);
        if (bigramCount > 0) {
            return (double) bigramCount / (double) bigramDictionary.getTotalFrequencyCount();
        } else {
            long unigramCount = unigramDictionary.getQueryFrequency(combinationQuery.get(index +1));
            return (alpha * (double) unigramCount)/ (double) unigramDictionary.getTotalFrequencyCount();
        }
    }
}
