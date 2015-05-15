package spelin.ranker;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import spelin.util.CSVDictionaryParser;
import spelin.util.CandidateSuggestion;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Ranker will index the transform probabilities,
 * and then when used to compute ranks, it will just return the most likely transformations
 */
public class BMUnigramRanker implements UnigramRanker {

    //Path for input dictionary and also output directory
    private static final String INPUT_FILE_PATH = "bmPairs.csv";
    private static final String INDEX_DIRECTORY_PATH = "BMIndex";

    public static final String TRANSFORM_FIELD_NAME1 = "p";
    public static final String TRANSFORM_FIELD_NAME2 = "r";
    public static final String SCORE_FIELD_NAME = "f";
    private static final StringField TRANSFORM_FIELD1 = new StringField(TRANSFORM_FIELD_NAME1, "", Field.Store.YES);
    private static final StringField TRANSFORM_FIELD2 = new StringField(TRANSFORM_FIELD_NAME2, "", Field.Store.YES);
    private static final DoubleField SCORE_FIELD = new DoubleField(SCORE_FIELD_NAME, 0, Field.Store.YES);
    private static final Document DOCUMENT_TO_ADD = makeDocument();

    private static double LANGUAGE_MODEL_WEIGHT = 0.3;

    private static final String PATTERN_SEPARATOR = " ";
    private static final String EMPTY_PATTERN = "\0";
    private static final int WINDOW_SIZE = 4;

    private IndexWriter indexWriter;
    private IndexSearcher indexSearcher;
    private Directory indexDirectory;
    private long uniqueEntryCount;
    private long frequencyCount;

    //For each group we are evaluating, we rely on this set to calculate the probability of the original
    //a multithreading chokepoint, but if we want to go that route, this can be modified
    private ArrayList<String> maximumTransformsCache;

    public static void setLANGUAGE_MODEL_WEIGHT(double weight) {
        LANGUAGE_MODEL_WEIGHT = weight;
    }

    public BMUnigramRanker() throws IOException {
        this.indexDirectory = FSDirectory.open(new File(INDEX_DIRECTORY_PATH));
        this.uniqueEntryCount = 0;
        this.frequencyCount = 0;
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_4_10_3, new StandardAnalyzer());
        this.indexWriter = new IndexWriter(indexDirectory, config);
        addPatterns(new File(INPUT_FILE_PATH));

        this.indexSearcher = new IndexSearcher(DirectoryReader.open(this.indexDirectory));
        this.maximumTransformsCache = new ArrayList<String>();
    }

    private void addDocument(String[] pieces) throws IOException {
        if (pieces.length < 3) {
            return;
        }
        TRANSFORM_FIELD1.setStringValue(pieces[0]);
        TRANSFORM_FIELD2.setStringValue(pieces[1]);
        double score = Double.parseDouble(pieces[2]);
        SCORE_FIELD.setDoubleValue(score);
        this.indexWriter.addDocument(DOCUMENT_TO_ADD);
    }

    private void addPatterns(File inputFile) throws IOException {
        CSVDictionaryParser parser = new CSVDictionaryParser(inputFile);
        String[] inputPieces;
        inputPieces = parser.getStringToken();
        this.uniqueEntryCount = Long.parseLong(inputPieces[0]);
        this.frequencyCount = Long.parseLong(inputPieces[1]);
        if (!tokensIndex()) {
            while ((inputPieces = parser.getStringToken()) != null) {
                addDocument(inputPieces);
            }
        }

        this.indexWriter.close();
    }

    private boolean tokensIndex() {
        try {
            return DirectoryReader.indexExists(this.indexDirectory);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static Document makeDocument(){
        Document doc = new Document();
        doc.add(TRANSFORM_FIELD1);
        doc.add(TRANSFORM_FIELD2);
        doc.add(SCORE_FIELD);
        return doc;
    }

    //Method to align words accordingly to the lowest edit distance
    private static String[] alignWords(String original, String candidate) throws Exception {
        int[][] cost = new int[candidate.length() +1][original.length() +1];

        //populate matrix for first row
        for (int i = 0; i < original.length() +1 ;i++) {
            cost[0][i] = i;
        }

        //populate matrix for first column
        for (int i = 0; i < candidate.length() + 1; i++) {
            cost[i][0] = i;
        }

        char[] candidateChars = candidate.toCharArray();
        char[] originalChars = original.toCharArray();
        //Run DP on this to first populate the matrix
        for (int i = 0; i < candidate.length(); i++) {
            for (int j = 0; j < original.length() ; j++) {
                if (candidateChars[i] == originalChars[j]) {
                    cost[i+1][j+1] = cost[i][j];
                } else if (i > 0 && j >0 && candidateChars[i-1] == originalChars[j]
                        && candidateChars[i] == originalChars[j-1]) {
                    //in case of transposition
                    cost[i+1][j+1] = cost[i-1][j-1] + 1;
                } else if (candidateChars[i] != originalChars[j]) {
                    //all other cases
                    cost[i+1][j+1] = Math.min(cost[i][j], Math.min(cost[i][j+1], cost[i+1][j])) + 1;
                }
            }
        }

        //System.out.println(cost[candidate.length()][original.length()]);

        int currentCost = cost[candidate.length()][original.length()];
        int candidateIndex = candidate.length();
        int originalIndex = original.length();

        StringBuilder originalSB = new StringBuilder();
        StringBuilder candidateSB = new StringBuilder();

        while (candidateIndex > 0 && originalIndex > 0) {
            if (originalIndex > 1 && candidateIndex > 1 && candidateChars[candidateIndex-1] != originalChars[originalIndex-1]
                    && candidateChars[candidateIndex-2] == originalChars[originalIndex-1]
                    && candidateChars[candidateIndex-1] == originalChars[originalIndex-2]) {
                //transposition
                candidateSB.append(candidateChars[candidateIndex - 1]);
                candidateSB.append(candidateChars[candidateIndex - 2]);
                originalSB.append(originalChars[originalIndex - 1]);
                originalSB.append(originalChars[originalIndex - 2]);

                originalIndex -= 2;
                candidateIndex -=2 ;
                currentCost --;
            } else if (originalChars[originalIndex-1] != candidateChars[candidateIndex-1]
                    && cost[candidateIndex-1][originalIndex] == currentCost -1) {
                //deletion
                candidateIndex--;
                currentCost--;
                originalSB.append('\0');
                candidateSB.append(candidateChars[candidateIndex]);
            } else if (originalChars[originalIndex-1] != candidateChars[candidateIndex-1]
                    && cost[candidateIndex][originalIndex-1] == currentCost-1) {
                //insertion
                originalIndex--;
                currentCost --;
                originalSB.append(originalChars[originalIndex]);
                candidateSB.append('\0');
            } else if (cost[candidateIndex-1][originalIndex-1] == currentCost
                    && originalChars[originalIndex-1] == candidateChars[candidateIndex-1]) {
                originalIndex--;
                candidateIndex--;
                originalSB.append(originalChars[originalIndex]);
                candidateSB.append(candidateChars[candidateIndex]);
            } else if (cost[candidateIndex-1][originalIndex-1] == currentCost-1
                    && originalChars[originalIndex -1] != candidateChars[candidateIndex-1]) {
                originalIndex--;
                candidateIndex--;
                currentCost--;
                originalSB.append(originalChars[originalIndex]);
                candidateSB.append(candidateChars[candidateIndex]);
            } else {
                throw new Exception("Error reconstructing aligned array");
            }
        }

        //there could be left overs
        while (candidateIndex > 0) {
            candidateSB.append(candidateChars[--candidateIndex]);
            originalSB.append("\0");
        }
        while (originalIndex > 0) {
            originalSB.append(originalChars[--originalIndex]);
            candidateSB.append("\0");
        }


        //We always return original first, and then candidate
        String[] result = new String[2];
        result[0] = originalSB.reverse().toString();
        result[1] = candidateSB.reverse().toString();
        return result;
    }

    //This method will return all BM transform pairings for lookup in the table
    private static HashSet<String> bmPairings(String originalString, String candidateString) throws Exception {
        String[] alignedWords = alignWords(originalString, candidateString);
        char[] original = alignedWords[0].toCharArray();
        char[] candidate = alignedWords[1].toCharArray();

        //Because we only consider candidates 1 distance apart, we know that the popularity of this one edit can
        //be used to score the candidate
        HashSet<String> transforms = new HashSet<String>();
        for (int i = 0; i < original.length; i++) {
            if (original[i] != candidate[i]) {
                int backIndex = i;
                int forwardIndex = i;
                while (backIndex > 0 && backIndex > i - WINDOW_SIZE) {
                    StringBuilder sb = new StringBuilder();

                    addContext(sb, backIndex, i + 1, original.length, new String(original, backIndex, i - backIndex + 1));
                    sb.append(PATTERN_SEPARATOR);
                    addContext(sb, backIndex, i + 1, original.length, new String(candidate, backIndex, i - backIndex + 1));
                    transforms.add(sb.toString().replace(EMPTY_PATTERN,""));
                    backIndex--;
                }

                while (forwardIndex < original.length && i + WINDOW_SIZE > forwardIndex) {
                    StringBuilder sb = new StringBuilder();
                    addContext(sb, i, forwardIndex+1, original.length, new String(original, i, forwardIndex - i + 1));
                    sb.append(PATTERN_SEPARATOR);
                    addContext(sb, i, forwardIndex+1, original.length, new String(candidate, i, forwardIndex - i + 1));
                    transforms.add(sb.toString().replace(EMPTY_PATTERN,""));
                    forwardIndex++;
                }
            }
        }
        return transforms;
    }

    private static void addContext(StringBuilder sb, int begin, int end, int length, String patternToAdd) {
        if (patternToAdd.equals(EMPTY_PATTERN)) {
            sb.append(patternToAdd);
            return;
        }

        if (begin == 0) {
            sb.append("^");
        } else {
            sb.append(".");
        }

        sb.append(patternToAdd);

        if (end == length) {
            sb.append("$");
        } else {
            sb.append(".");
        }
    }

    private static void runTestCase(String original, String candidate) throws Exception {
        HashSet<String> transforms = bmPairings(original, candidate);
        for (String transform : transforms) {
            System.out.println(transform.replaceAll(EMPTY_PATTERN, ""));
        }
    }

    private static void runAlignTestCase(String originalString, String candidateString) throws Exception {
        String[] result = alignWords(originalString, candidateString);
        char[] original = result[0].toCharArray();
        char[] candidate = result[1].toCharArray();
        for (char c : original) {
            System.out.print(c);
            System.out.print(" ");
        }

        System.out.println(" ");
        for(char c : candidate) {
            System.out.print(c);
            System.out.print(" ");
        }

        System.out.println(" ");
    }

    private Query transformFrequency(String[] pieces) throws IOException {

        TermQuery firstTermFirstCombo = new TermQuery(new Term(TRANSFORM_FIELD_NAME1, pieces[0]));
        TermQuery secondTermFirstCombo = new TermQuery(new Term(TRANSFORM_FIELD_NAME2, pieces.length > 1? pieces[1] : ""));
        BooleanQuery firstCombination = new BooleanQuery();
        firstCombination.add(firstTermFirstCombo, BooleanClause.Occur.MUST);
        firstCombination.add(secondTermFirstCombo, BooleanClause.Occur.MUST);

        BooleanQuery query = new BooleanQuery();
        query.add(firstCombination, BooleanClause.Occur.SHOULD);

        return query;
    }

    private ScoreDoc[] searchTransformDocs(HashSet<String> transforms) throws IOException {
        BooleanQuery q = new BooleanQuery();
        //remove invalid queries
        transforms.remove(PATTERN_SEPARATOR);

        //check if I have no candidates
        if (transforms.isEmpty()) return null;
        for (String transform : transforms) {
            q.add(transformFrequency(transform.split(PATTERN_SEPARATOR)), BooleanClause.Occur.SHOULD);
        }

        ScoreDoc[] docs = this.indexSearcher.search(q, transforms.size()).scoreDocs;
        return docs;
    }

    private double maxLikelyTransform(HashSet<String> transforms) throws IOException {
        //we know that this transform occurred at least once from observation
        double maxSoFar = 1.0 / this.frequencyCount;

        ScoreDoc[] docs = searchTransformDocs(transforms);
        String maxTransformTransform = "";
        for (ScoreDoc doc : docs) {
            double docScore = Double.parseDouble(this.indexSearcher.doc(doc.doc).get(SCORE_FIELD_NAME));
            if (maxSoFar < docScore) {
                maxSoFar = docScore;
                maxTransformTransform = this.indexSearcher.doc(doc.doc).get(TRANSFORM_FIELD_NAME2);
            }
            maxSoFar = Math.max(maxSoFar,
                    Double.parseDouble(this.indexSearcher.doc(doc.doc).get(SCORE_FIELD_NAME)));
        }

        this.maximumTransformsCache.add(maxTransformTransform + PATTERN_SEPARATOR + maxTransformTransform);

        return maxSoFar;
    }

    //NOTE THIS METHOD MUST BE CALLED AFTER ALL THE CANDIDATES HAVE BEEN SCORED
    //Implement P(w|w) = 1 - sum( P(c|w) for c != w)
    @Deprecated
    private double scoreOriginal(List<CandidateSuggestion> candidateSuggestions) throws IOException {
        //we have all the transforms that were applied, now we compute the transforms that weren't applied
        //ei if one candidate was from a -> '', we find a -> a now
        ScoreDoc[] docs = searchTransformDocs(new HashSet<String>(this.maximumTransformsCache));

        //if there are no other candidates, then doesn't matter what I score myself,
        if (docs == null) {
            return 1.0;
        }
        //at least since we know that there are n canidates from n transforms, then
        //there should exist at least n x-> x non transforms
        double minSum = (candidateSuggestions.size() -1) / (double) this.frequencyCount;
        double sum = 1.0;
        //we sum up the scores of everything that matched
        for (ScoreDoc doc : docs) {
            sum += Double.parseDouble(this.indexSearcher.doc(doc.doc).get(SCORE_FIELD_NAME));
        }

        return Math.min(minSum, sum);
    }


    @Override
    public List<CandidateSuggestion> rankCandidates(List<CandidateSuggestion> candidates, final CandidateSuggestion query) {
        //clear the cache before we start scoring
        //first we score all the candidates
        //We want to score the original query differently
        for (CandidateSuggestion candidate : candidates) {
            candidate.score = score(query, candidate);
        }

        //now we normalize everyone
        normalizeScores(candidates);
        Collections.sort(candidates, new Comparator<CandidateSuggestion>() {
            @Override
            public int compare(CandidateSuggestion o1, CandidateSuggestion o2) {
                //we want to sort descending
                return -1 * Double.compare(o1.score, o2.score);
            }
        });
        return candidates;
    }

    //Method will normalize all the scores
    public void normalizeScores(List<CandidateSuggestion> candidateSuggestions) {
        double normalizingConstant = 0.0;
        for (CandidateSuggestion candidate : candidateSuggestions) {
            normalizingConstant += candidate.score;
        }

        for (CandidateSuggestion candidateSuggestion : candidateSuggestions) {
            candidateSuggestion.score = candidateSuggestion.score / normalizingConstant;
        }
    }

    //method will find the most commonly occurring word pieces that makes up this word
    //the original should be heavily dependent on its presence in the corpus
    public double scoreOriginal(String original) {
        //we run a recursive algorithm
        char[] characters = original.toCharArray();
        double[][] costMatrix = new double[characters.length][characters.length];
        //we store the costs
        for (int i =0; i < characters.length; i++) {
            for (int j = i+1; j < characters.length+1; j++) {
                StringBuilder sb = new StringBuilder();
                addContext(sb, i, j, characters.length, new String(characters, i, j-i));
                String pattern = sb.toString();
                sb.append(PATTERN_SEPARATOR);
                sb.append(pattern);
                HashSet<String> testPattern = new HashSet<String>();
                testPattern.add(sb.toString());
                try {
                    costMatrix[i][j-1] = maxLikelyTransform(testPattern);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        //for each cost starting at i to j calculate the maximal score

        return maximalScoreOnSubString(costMatrix, 0, costMatrix.length);
    }

    //index from 0 to length of array
    private static double maximalScoreOnSubString(double[][] costMatrix, int start, int end) {
        //base case, we return the cost of a single character
        if (start == end) {
            return 1.0;
        }

        double maxSoFar = 0.0;
        for (int i = start; i < end; i++) {
            double cost = costMatrix[start][i] * maximalScoreOnSubString(costMatrix, i+1, end);
            maxSoFar = maxSoFar < cost ? cost : maxSoFar;
        }
        return maxSoFar;
    }

    @Override
    public double score(CandidateSuggestion query, CandidateSuggestion candidate) {
        //retrieve the count on
        try {
            HashSet<String> transforms = bmPairings(query.candidate, candidate.candidate);
            //if the transforms are empty, then the two terms are identical
            //NOTE because of the new method to score the original, MUST NOT calculate transforms randomly
            return transforms.isEmpty() ? scoreOriginal(query.candidate) * (candidate.frequency / (double) candidate.totalFrequency)
                    : maxLikelyTransform(transforms) * (1- LANGUAGE_MODEL_WEIGHT) + (candidate.frequency / (double) candidate.totalFrequency) * LANGUAGE_MODEL_WEIGHT;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }
}
