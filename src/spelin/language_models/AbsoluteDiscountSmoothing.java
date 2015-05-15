package spelin.language_models;

import spelin.bigram_dictionary.BigramDictionary;
import spelin.unigram_dictionary.UnigramDictionary;

import java.util.List;

/**
 * Created by Steven on 2015-02-22.
 */
public class AbsoluteDiscountSmoothing extends AbstractLanguageModel {
    protected double discount;

    public AbsoluteDiscountSmoothing(double discount) {
        this.discount = discount;
    }

    @Override
    public double score(List<String> combinationQuery, int index, UnigramDictionary unigramDictionary, BigramDictionary bigramDictionary) {
        String bigram = constructBigram(combinationQuery, index);
        long bigramCount = bigramDictionary.getQueryFrequency(bigram);
        long unigramCount = unigramDictionary.getQueryFrequency(combinationQuery.get(index));

        double firstTerm = (((double) bigramCount - this.discount) / (double) unigramCount);
        double secondTerm = interpolationWeight(combinationQuery, index, unigramDictionary, bigramDictionary) *
                lowerOrderProb(combinationQuery, index, unigramDictionary, bigramDictionary);

        return firstTerm  + secondTerm;
    }

    //can be overwritten
    protected double lowerOrderProb(List<String> combinationQuery, int index, UnigramDictionary unigramDictionary, BigramDictionary bigramDictionary) {
        return  (unigramDictionary.getQueryFrequency(combinationQuery.get(index + 1)) / (double) unigramDictionary.getTotalFrequencyCount());
    }

    //can also be overwritten
    protected double interpolationWeight(List<String> combinationQuery, int index, UnigramDictionary unigramDictionary, BigramDictionary bigramDictionary) {
        //lambda is the normalizing constant
        double lambda = (this.discount / (double) unigramDictionary.getQueryFrequency(combinationQuery.get(index))) *
                bigramDictionary.documentsStartingIn(combinationQuery.get(index));

        return lambda;
    }
}
