package spelin.model_components;

import org.apache.commons.lang3.StringUtils;

/**
 * Created by Steven on 2015-02-04.
 */
public class EditDistanceFeatures {
    public static double score(String query, String candidate) {
        return 1.0 - ((double) StringUtils.getLevenshteinDistance(query, candidate) / (double) query.length());
    }
}
