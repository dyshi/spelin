package spelin.unigram_dictionary;

import spelin.util.CandidateSuggestion;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by Steven on 2015-02-01.
 * This interface must be implemented by the candidate generator
 * This way we can easily plug and play for different dictionaries generated and used
 */
public interface UnigramDictionary {

    public void addTokens(File inputFile) throws IOException;

    public List<CandidateSuggestion> generateCandidates(CandidateSuggestion query);

    public String normalizeToken(String token);

    public boolean tokensIndexed();

    public long getQueryFrequency(String unigramQuery);

    public long getTotalFrequencyCount();

    public long getUniqueDocumentCount();

}
