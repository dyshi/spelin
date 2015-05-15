package spelin.ranker;

import spelin.util.CandidateSuggestion;
import spelin.model_components.DoubleMetaphoneFeatures;
import spelin.model_components.EditDistanceFeatures;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Simple ranker
 * Created by Steven on 2015-02-02.
 */
public class UnigramRankerImpl implements UnigramRanker {

    public static final double STRING_DIST_WEIGHT = 0.5;
    public static final double DM_DIST_WEIGHT = 0.5;

    @Override
    public List<CandidateSuggestion> rankCandidates(List<CandidateSuggestion> candidates, final CandidateSuggestion query) {
        Collections.sort(candidates, new Comparator<CandidateSuggestion>() {
            @Override
            public int compare(CandidateSuggestion o1, CandidateSuggestion o2) {
                //we want to sort descending
                return -1 * Double.compare(score(query, o1), score(query, o2));
            }
        });
        return candidates;
    }

    @Override
    public double score(CandidateSuggestion query, CandidateSuggestion candidate) {
        //sort using some error model/Language model
        //use the simple LD distance similarity
        candidate.score = STRING_DIST_WEIGHT * EditDistanceFeatures.score(query.candidate, candidate.candidate) +
                DM_DIST_WEIGHT * DoubleMetaphoneFeatures.scorePhoneticSimilarity(query.candidate, candidate.candidate);
        //System.out.print(candidate.candidate);
        //System.out.print(DoubleMetaphoneFeatures.scorePhoneticSimilarity(query.candidate, candidate.candidate));
        //System.out.println(EditDistanceFeatures.score(query.candidate, candidate.candidate));
        return candidate.score;
    }

    /**
     * Dummy implementation, very primitive unigram ranker
     * @param original  original query token
     * @return          score
     */
    @Override
    public double scoreOriginal(String original) {
        return 1.0;
    }
}
