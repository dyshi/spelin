package spelin.ranker;

import spelin.bigram_dictionary.BigramDictionary;
import spelin.language_models.BigramLanguageModel;
import spelin.unigram_dictionary.UnigramDictionary;
import spelin.util.CandidateSuggestion;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Implementation of Bigram Ranker using the Bing API
 * Created by Steven on 2015-03-23.
 */
public class BingBigramRanker extends BigramRankerImpl {

    private static final String BASE_URL = "http://weblm.research.microsoft.com/rest.svc/bing-body/2013-12/3/cp?u=INSERT_TOKEN&format=text&p=";
    private static final String ENCODED_SPACE = "%20";

    private static final String GET_REQUEST = "GET";
    private static final String UTF8 = "UTF-8";

    public BingBigramRanker(BigramDictionary bigramDictionary, BigramLanguageModel model, UnigramDictionary unigramDictionary) {
        super(bigramDictionary, model, unigramDictionary);
    }

    private static String formUrl(List<CandidateSuggestion> correctionSequence) throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();
        sb.append(BASE_URL);
        for (CandidateSuggestion token : correctionSequence) {
            sb.append(URLEncoder.encode(token.candidate, UTF8));
            sb.append(ENCODED_SPACE);
        }

        sb.deleteCharAt(sb.length()-1);
        sb.deleteCharAt(sb.length()-1);
        sb.deleteCharAt(sb.length()-1);
        return sb.toString();
    }

    @Override
    public double score (List<CandidateSuggestion> query, List<CandidateSuggestion> correctionSequence) {
        String url = null;
        try {
            url = formUrl(correctionSequence);

            URL urlToGet = new URL(url);
            HttpURLConnection con = (HttpURLConnection) urlToGet.openConnection();

            con.setRequestMethod(GET_REQUEST);

            BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));

            String line = reader.readLine();
            double score = Double.parseDouble(line);
            reader.close();

            return Math.pow(2, score);
        } catch (Exception e) {
            return -1.0;
        }
    }

    @Override
    public ArrayList<ArrayList<CandidateSuggestion>> rankBigramCandidates(ArrayList<List<CandidateSuggestion>> suggestions, final ArrayList<CandidateSuggestion> query, boolean runIterative) {
        ArrayList<ArrayList<CandidateSuggestion>> allCombinations = generateCombination(suggestions, query, runIterative);
        Collections.sort(allCombinations, new Comparator<ArrayList<CandidateSuggestion>>() {
            @Override
            public int compare(ArrayList<CandidateSuggestion> o1, ArrayList<CandidateSuggestion> o2) {
                return -1* Double.compare(score(query, o1), score(query, o2));
            }
        });

        return allCombinations;
    }
}
