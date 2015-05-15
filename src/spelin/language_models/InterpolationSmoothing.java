package spelin.language_models;

import spelin.bigram_dictionary.BigramDictionary;
import spelin.unigram_dictionary.UnigramDictionary;

import java.util.List;

/**
 * Jelinek-Mercer smoothing
 * Created by Steven on 2015-02-23.
 */
public class InterpolationSmoothing extends AbstractLanguageModel {

    private double lambdaParam;

    public InterpolationSmoothing(double param) {
        this.lambdaParam = param;
    }

    @Override
    public double score(List<String> combinationQuery, int index, UnigramDictionary unigramDictionary, BigramDictionary bigramDictionary) {
        String bigram = constructBigram(combinationQuery, index);
        double bigramProb = bigramDictionary.getQueryFrequency(bigram) / (double) bigramDictionary.getTotalFrequencyCount();
        double unigramProb = unigramDictionary.getQueryFrequency(combinationQuery.get(index + 1))
                / (double) unigramDictionary.getTotalFrequencyCount();
        return this.lambdaParam * bigramProb + (1 - this.lambdaParam) * unigramProb;
    }

}
