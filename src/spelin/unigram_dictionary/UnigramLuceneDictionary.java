package spelin.unigram_dictionary;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import spelin.util.CSVDictionaryParser;
import spelin.util.CandidateSuggestion;
import spelin.util.FileParser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Steven on 2015-02-01.
 */
public class UnigramLuceneDictionary implements UnigramDictionary {
    public  static final String TOKEN_FIELD_NAME = "t";
    public static final String FREQUENCY_FIELD_NAME ="f";
    private static final TextField TOKEN_FIELD = new TextField(TOKEN_FIELD_NAME, "", Field.Store.YES);
    private static final LongField FREQUENCY_FIELD = new LongField(FREQUENCY_FIELD_NAME,0, Field.Store.YES);
    private static final Document DOCUMENT_TO_ADD = makeDocument();

    private long frequencyCount;
    private long uniqueDocumentCount;
    private IndexWriter indexWriter;
    private IndexSearcher indexSearcher;
    private Directory indexDirectory;
    private static final int MAX_CANDIDATE_RESULTS = 1000;
    private static final int MAX_RETURN_NUM = 12;
    //Restricted to only 1 or 2, larger no point for our purposes and not supported.
    private static final int MAX_EDIT_CANDIDATE = 1;

    public UnigramLuceneDictionary(String directoryPath, String inputFilePath) throws IOException {
        this.indexDirectory = FSDirectory.open(new File(directoryPath));
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_4_10_3, new StandardAnalyzer());
        this.indexWriter = new IndexWriter(this.indexDirectory, config);
        this.addTokens(new File(inputFilePath));
        this.indexSearcher = new IndexSearcher(DirectoryReader.open(this.indexDirectory));
    }

    private static Document makeDocument() {
        Document doc = new Document();
        doc.add(TOKEN_FIELD);
        doc.add(FREQUENCY_FIELD);
        return doc;
    }

    private void addDocument(FileParser.Token token) throws IOException {
        TOKEN_FIELD.setStringValue(token.token);
        FREQUENCY_FIELD.setLongValue(token.frequency);

        this.indexWriter.addDocument(DOCUMENT_TO_ADD);
    }

    @Override
    public void addTokens(File inputFile) throws IOException {
        CSVDictionaryParser parser = new CSVDictionaryParser(inputFile);
        FileParser.Token readToken = null;
        String[] pieces = parser.getStringToken();
        //We should read the pieces and then parse the unique values from the first line
        this.uniqueDocumentCount = Long.parseLong(pieces[0]);
        this.frequencyCount = Long.parseLong(pieces[1]);
        if (!tokensIndexed()) {
            while ((readToken = parser.getToken()) != null) {
                addDocument(readToken);
            }
        }
        this.indexWriter.close();
    }

    public ScoreDoc[] searchIndex(String query) throws IOException {
        Query q = new FuzzyQuery(new Term(TOKEN_FIELD_NAME, query), MAX_EDIT_CANDIDATE);
        if (this.indexSearcher == null) this.indexSearcher = new IndexSearcher(DirectoryReader.open(this.indexDirectory));
        return this.indexSearcher.search(q, MAX_CANDIDATE_RESULTS).scoreDocs;
    }

    @Override
    public List<CandidateSuggestion> generateCandidates(CandidateSuggestion original) {
        String query = original.candidate;
        ArrayList<CandidateSuggestion> candidates = new ArrayList<CandidateSuggestion>();
        //denoting whether an original exists in the actual corpus
        boolean hasOriginal = false;
        candidates.add(original);
        try {
            for (ScoreDoc scoreDoc: this.searchIndex(query)) {
                Document doc = this.indexSearcher.doc(scoreDoc.doc);

                CandidateSuggestion suggestion = new CandidateSuggestion(doc.get(TOKEN_FIELD_NAME), Long.parseLong(doc.get(FREQUENCY_FIELD_NAME)), this.getTotalFrequencyCount());

                //If we have found the original, move on, and don't add the duplicate, but update the values in the original
                if (doc.get(TOKEN_FIELD_NAME).equals(query)) {
                    hasOriginal = true;
                    original.frequency = suggestion.frequency;
                    original.totalFrequency = this.getTotalFrequencyCount();
                }
                candidates.add(suggestion);
            }
            //we add the original
            if (!hasOriginal) {
                original.frequency = 1;
                original.totalFrequency = this.getTotalFrequencyCount();
                CandidateSuggestion originalCopy = new CandidateSuggestion(original);
                candidates.add(originalCopy);
            }
            return candidates.subList(0, Math.min(MAX_RETURN_NUM, candidates.size()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String normalizeToken(String token) {
        return CSVDictionaryParser.normalizeToken(token);
    }

    @Override
    public boolean tokensIndexed() {
        try {
            return DirectoryReader.indexExists(this.indexDirectory);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public long getQueryFrequency(String unigramQuery) {
        Query q = new TermQuery(new Term(TOKEN_FIELD_NAME, unigramQuery));
        try {
            if (this.indexSearcher == null) this.indexSearcher = new IndexSearcher(DirectoryReader.open(this.indexDirectory));
            ScoreDoc[] results = this.indexSearcher.search(q, 1).scoreDocs;
            if (results.length == 1) {
                return Long.parseLong(this.indexSearcher.doc(results[0].doc).get(FREQUENCY_FIELD_NAME));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public long getTotalFrequencyCount() {
        return this.frequencyCount;
    }

    @Override
    public long getUniqueDocumentCount() {
        return this.uniqueDocumentCount;
    }
}
