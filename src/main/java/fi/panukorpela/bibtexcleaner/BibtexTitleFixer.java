package fi.panukorpela.bibtexcleaner;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jbibtex.BibTeXDatabase;
import org.jbibtex.BibTeXEntry;
import org.jbibtex.BibTeXFormatter;
import org.jbibtex.BibTeXParser;
import org.jbibtex.Key;
import org.jbibtex.StringValue;
import org.jbibtex.Value;

public class BibtexTitleFixer {

    public static void main(String[] args) throws Exception {
        // Change these as needed:
        String inputFile = "/Users/pkorpela/Downloads/manual_entries.bib";
        String outputFile = "/Users/pkorpela/Downloads/manual_entries2.bib";

        BibTeXDatabase db = readBibtex(inputFile);

        for (Map.Entry<Key, BibTeXEntry> entry : db.getEntries().entrySet()) {
            BibTeXEntry bibEntry = entry.getValue();
            String entryType = bibEntry.getType().toString().toLowerCase();
            Value titleVal = bibEntry.getField(BibTeXEntry.KEY_TITLE);

            if (titleVal != null && !entryType.equals("book")) {
                String origTitle = titleVal.toUserString();
                String fixedTitle = smartCapitalizeTitle(origTitle);
                bibEntry.addField(BibTeXEntry.KEY_TITLE, new StringValue(fixedTitle, StringValue.Style.BRACED));
            }
        }

        writeBibtex(db, outputFile);
        System.out.println("Done. Output written to " + outputFile);
    }

    // Smart capitalization as per your rules
    private static String smartCapitalizeTitle(String title) {
        // Split into sentences (after dots)
        Pattern splitPattern = Pattern.compile("(\\.\\s+)");
        String[] tokens = splitPattern.split(title);
        Matcher matcher = splitPattern.matcher(title);

        StringBuilder result = new StringBuilder();
        for (String segment : tokens) {
            String[] words = segment.split(" ");
            for (int j = 0; j < words.length; j++) {
                String word = words[j];
                // If more than one uppercase letter, don't touch
                int uppercaseCount = 0;
                for (char c : word.toCharArray()) {
                    if (Character.isUpperCase(c)) uppercaseCount++;
                }
                if (uppercaseCount > 1) {
                    result.append(word);
                } else if (j == 0) {
                    // First word of segment
                    if (word.length() > 0)
                        result.append(Character.toUpperCase(word.charAt(0)))
                              .append(word.substring(1).toLowerCase());
                } else {
                    result.append(word.toLowerCase());
                }
                if (j < words.length - 1) result.append(" ");
            }
            // Restore the separator if present
            if (matcher.find()) result.append(matcher.group(1));
        }
        return result.toString();
    }

    private static BibTeXDatabase readBibtex(String filename) throws Exception {
        Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
        BibTeXParser parser = new BibTeXParser();
        return parser.parse(reader);
    }

    private static void writeBibtex(BibTeXDatabase db, String filename) throws Exception {
        StringWriter sw = new StringWriter();
        BibTeXFormatter formatter = new BibTeXFormatter();
        formatter.format(db, sw);
        String formatted = sw.toString();

        // Convert leading tabs to two spaces
        String formattedWithSpaces = replaceLeadingTabsWithSpaces(formatted, 2);

        try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), "UTF-8"))) {
            writer.write(formattedWithSpaces);
        }
    }

    
    private static String replaceLeadingTabsWithSpaces(String text, int spacesPerTab) {
        StringBuilder sb = new StringBuilder();
        Pattern pattern = Pattern.compile("^(\\t+)", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(text);
        int lastEnd = 0;
        while (matcher.find()) {
            sb.append(text, lastEnd, matcher.start());
            int tabCount = matcher.group(1).length();
            sb.append(" ".repeat(tabCount * spacesPerTab));
            lastEnd = matcher.end();
        }
        sb.append(text.substring(lastEnd));
        return sb.toString();
    }
}
