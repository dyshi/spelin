package spelin.util;

import java.io.*;

/**
 * Created by Steven on 2015-02-01.
 */
public class CSVDictionaryParser implements FileParser {
    protected BufferedReader fileReader;

    private static final String TOKENIZER = ",";
    public CSVDictionaryParser(File inputFile) throws FileNotFoundException {
        this.fileReader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile)));
    }

    @Override
    public Token getToken() throws IOException {
        String line = this.fileReader.readLine();
        if (line == null) {
            return null;
        }
        Token token = new Token();

        String[] pieces = line.split(TOKENIZER);
        token.token = normalizeToken(pieces[0]);
        token.frequency = Long.parseLong(pieces[1]);
        return token;
    }

    @Override
    public String[] getStringToken() throws IOException {
        String line = this.fileReader.readLine();
        if (line == null) {
            return null;
        }

        return line.split(TOKENIZER);
    }

    //normalize the tokens
    public static String normalizeToken(String token) {
        return token.toLowerCase();
    }
}
