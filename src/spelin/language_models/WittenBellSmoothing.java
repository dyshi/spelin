package spelin.language_models;

import spelin.bigram_dictionary.BigramDictionary;
import spelin.unigram_dictionary.UnigramDictionary;

import java.util.List;

/**
 * Created by Steven on 2015-02-23.
 */
public class WittenBellSmoothing extends AbstractLanguageModel {

    public double score(List<String> combinationQuery, int index, UnigramDictionary unigramDictionary, BigramDictionary bigramDictionary) {
        String bigram = constructBigram(combinationQuery, index);
        long bigramCount = bigramDictionary.getQueryFrequency(bigram);

        int secondTokenWordTypeCount = bigramDictionary.documentsStartingIn(combinationQuery.get(index));
        int unseenSecondTypeCount = unseenBigramGivenW(combinationQuery.get(index), unigramDictionary, bigramDictionary);

        if (bigramCount == 0) {
            return (secondTokenWordTypeCount) /
                    ((double) (secondTokenWordTypeCount + unigramDictionary.getTotalFrequencyCount()) * unseenSecondTypeCount);
        } else {
            return (double) bigramCount / (double) (secondTokenWordTypeCount + unigramDictionary.getTotalFrequencyCount());
        }
    }

    //this method estimates the number of unseen bigram possibilities for a starting w
    //we are assuming any unigram we observed can follow w, then the unseen ones are |unigramdict| - T(w)
    public int unseenBigramGivenW(String w, UnigramDictionary unigramDictionary, BigramDictionary bigramDictionary) {
        return (int) (unigramDictionary.getUniqueDocumentCount() - bigramDictionary.documentsStartingIn(w));
    }
}
