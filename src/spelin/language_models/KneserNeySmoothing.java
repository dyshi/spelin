package spelin.language_models;

import spelin.bigram_dictionary.BigramDictionary;
import spelin.unigram_dictionary.UnigramDictionary;

import java.util.List;

/**
 * Created by Steven on 2015-02-23.
 */
public class KneserNeySmoothing extends AbsoluteDiscountSmoothing {
    public KneserNeySmoothing(double discount) {
        super(discount);
    }

    @Override
    protected double lowerOrderProb (List<String> combinationQuery, int index, UnigramDictionary unigramDictionary, BigramDictionary bigramDictionary) {
        long uniqueBigramCount = bigramDictionary.getUniqueDocumentCount();
        double continuationProb = (double) bigramDictionary.documentsEndingIn(combinationQuery.get(index +1 )) /
                (double) uniqueBigramCount;
        return continuationProb;
    }
}
