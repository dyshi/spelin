package spelin.language_models;

import org.apache.commons.lang3.StringUtils;
import spelin.bigram_dictionary.BigramDictionary;
import spelin.unigram_dictionary.UnigramDictionary;

import java.util.List;

/**
 * Created by Steven on 2015-02-22.
 */
public abstract class AbstractLanguageModel implements BigramLanguageModel {
    protected String constructBigram(List<String> combinationQuery, int index) {
        StringBuilder sb = new StringBuilder();
        sb.append(combinationQuery.get(index));
        sb.append(StringUtils.SPACE);
        sb.append(combinationQuery.get(index + 1));
        return sb.toString();
    }

    @Override
    public double score(List<String> combinationQuery, int index, UnigramDictionary unigramDictionary, BigramDictionary bigramDictionary) {
        return 0;
    }
}
