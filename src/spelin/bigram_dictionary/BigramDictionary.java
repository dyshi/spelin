package spelin.bigram_dictionary;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * The bigram dictionary is slightly different from the unigram dictionary
 * this is no longer concerned with generating candidates
 * it is more of a tool to look up how likely is a certain candidate combination
 * Created by Steven on 2015-02-22.
 */
public interface BigramDictionary {
    public void addTokens(File inputFile) throws IOException;

    public String normalizeToken(String token);

    public boolean tokensIndexed();

    public int getQueryFrequency(String bigramQuery);

    //for counting tokens
    public long getTotalFrequencyCount();

    //for counting types
    public long getUniqueDocumentCount();

    //return the number of unique documents that have the input as the second token in the bigram
    public int documentsEndingIn(String secondToken);

    //return the number of unique documents that have hte input as the first token in the bigram
    public int documentsStartingIn(String firstToken);
}
