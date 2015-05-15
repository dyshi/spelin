package spelin.bigram_dictionary;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import spelin.unigram_dictionary.UnigramDictionary;
import spelin.util.CSVDictionaryParser;
import spelin.util.FileParser;

import java.io.File;
import java.io.IOException;

/**
 * Created by Steven on 2015-02-04.
 */
public class BigramLuceneDictionary implements BigramDictionary {
    public static final String TOKEN_FIELD_NAME = "t";
    public static final String FREQUENCY_FIELD_NAME = "f";
    private static final StringField TOKEN_FIELD = new StringField(TOKEN_FIELD_NAME, "", Field.Store.YES);
    private static final LongField FREQUENCY_FIELD = new LongField(FREQUENCY_FIELD_NAME, 0, Field.Store.YES);
    private static final Document DOCUMENT_TO_ADD = makeDocument();

    private long frequencyCount;
    private long uniqueDocumentCount;
    private IndexWriter indexWriter;
    private IndexSearcher indexSearcher;
    private Directory indexDirectory;

    public BigramLuceneDictionary(String directoryPath, String inputFilePath) throws IOException {
        this.frequencyCount = 0;
        this.uniqueDocumentCount = 0;
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

    private void addDocument(FileParser.Token entry) throws IOException {
        TOKEN_FIELD.setStringValue(entry.token);
        FREQUENCY_FIELD.setLongValue(entry.frequency);

        this.frequencyCount += entry.frequency;
        this.uniqueDocumentCount ++;
        this.indexWriter.addDocument(DOCUMENT_TO_ADD);
    }

    @Override
    public void addTokens(File inputFile) throws IOException {
        CSVDictionaryParser parser = new CSVDictionaryParser(inputFile);
        FileParser.Token readToken = null;
        String[] pieces = parser.getStringToken();
        this.uniqueDocumentCount = Long.parseLong(pieces[0]);
        this.frequencyCount = Long.parseLong(pieces[1]);
        if (!tokensIndexed()) {
            while ((readToken = parser.getToken()) != null) {
                addDocument(readToken);
            }
        }
        this.indexWriter.close();
    }

    @Override
    public String normalizeToken(String token) {
        return null;
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
    public int getQueryFrequency(String bigramQuery) {
        Query q = new TermQuery(new Term(TOKEN_FIELD_NAME, bigramQuery));
        try {
            return this.indexSearcher.search(q, 1).totalHits;
        } catch (IOException e) {
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

    @Override
    public int documentsEndingIn(String secondToken) {
        Query q = new RegexpQuery(new Term(" "+ secondToken+ "$"));
        try {
            return this.indexSearcher.search(q, 1).totalHits;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public int documentsStartingIn(String firstToken) {
        Query q = new RegexpQuery(new Term("^" + firstToken + " "));
        try {
            return this.indexSearcher.search(q, 1).totalHits;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

}
