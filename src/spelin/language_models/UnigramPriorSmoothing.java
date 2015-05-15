package spelin.language_models;

import spelin.bigram_dictionary.BigramDictionary;
import spelin.unigram_dictionary.UnigramDictionary;

import java.util.List;

/**
 * Created by Steven on 2015-02-22.
 */
public class UnigramPriorSmoothing extends AdditiveSmoothing {


    public UnigramPriorSmoothing(int value) {
        super(value);
    }

    @Override
    public double score(List<String> combinationQuery, int index, UnigramDictionary unigramDictionary, BigramDictionary bigramDictionary) {
        String bigram = this.constructBigram(combinationQuery, index);
        long bigramCount = bigramDictionary.getQueryFrequency(bigram);
        long unigramCount = unigramDictionary.getQueryFrequency(combinationQuery.get(index));
        long secondUnigramCount = unigramDictionary.getQueryFrequency(combinationQuery.get(index +1));

        return (double)( bigramCount + additiveValue * (secondUnigramCount / (double)unigramDictionary.getTotalFrequencyCount()) )
                / (double) (additiveValue * unigramDictionary.getTotalFrequencyCount() + unigramCount);
    }
}
