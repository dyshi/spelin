package spelin.util;

import spelin.unigram_dictionary.UnigramDictionary;

import java.io.IOException;

/**
 * Created by Steven on 2015-02-01.
 * Interface to be implemented by the file parser, this way our dictionary data can be easily substituted based what
 * is available
 */
public interface FileParser {

    public static class Token {
        public String token;
        public long frequency;
    }

    public Token getToken() throws IOException;

    public String[] getStringToken() throws IOException;

}
