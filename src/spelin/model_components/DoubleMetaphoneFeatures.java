package spelin.model_components;

import org.apache.commons.codec.language.DoubleMetaphone;
import org.apache.commons.lang3.StringUtils;

/**
 * Created by Steven on 2015-02-04.
 * This class provides a measure of phonetic similarity
 */
public class DoubleMetaphoneFeatures {
    private static final DoubleMetaphone dm = new DoubleMetaphone();

    public static String encode(String token) {
        return dm.encode(token);
    }

    public static double scorePhoneticSimilarity(String query, String candidate) {
        String queryEncoding = encode(query);
        String candidateEncoding = encode(candidate);
        return 1.0 - ((double) StringUtils.getLevenshteinDistance(queryEncoding, candidateEncoding) / (double) queryEncoding.length());
    }
}
