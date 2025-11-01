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

public class BibtexDateToYear {

    public static void main(String[] args) throws Exception {
        // Change as needed
        String inputFile = "/Users/pkorpela/Downloads/manual_entries.bib";
        String outputFile = "/Users/pkorpela/Downloads/manual_entries2.bib";

        BibTeXDatabase db = readBibtex(inputFile);

        for (Map.Entry<Key, BibTeXEntry> entry : db.getEntries().entrySet()) {
            BibTeXEntry bibEntry = entry.getValue();
            String entryType = bibEntry.getType().toString().toLowerCase();

            boolean relevantType = entryType.equals("article") || entryType.equals("proceedings");

            // Only process @article and @proceedings
            if (relevantType) {
                Value dateVal = bibEntry.getField(new Key("date"));
                Value yearVal = bibEntry.getField(BibTeXEntry.KEY_YEAR);
                Value monthVal = bibEntry.getField(new Key("month"));

                // --- DATE -> YEAR ---
                if (dateVal != null && yearVal == null) {
                    String dateString = dateVal.toUserString();
                    String extractedYear = extractYearFromDate(dateString);

                    if (extractedYear != null) {
                        // Add year entry
                        bibEntry.addField(BibTeXEntry.KEY_YEAR, new StringValue(extractedYear, StringValue.Style.BRACED));
                        // Add a comment for the date field
                        bibEntry.removeField(new Key("date")); // Remove so not duplicated on write
                        addCommentField(bibEntry, "Original date = {" + dateString + "}");
                        System.out.println("Added year=" + extractedYear + " to " + bibEntry.getKey());
                    } else {
                        System.out.println("WARNING: Could not extract year from date in entry " + bibEntry.getKey() + ": " + dateString);
                        // Add a comment for the unparsed date
                        addCommentField(bibEntry, "Unparsed date = {" + dateString + "}");
                    }
                } else if (dateVal != null && yearVal != null) {
                    // If both present, just comment the date
                    bibEntry.removeField(new Key("date"));
                    addCommentField(bibEntry, "Original date = {" + dateVal.toUserString() + "}");
                }

                // --- MONTH FIELD ---
                if (monthVal != null) {
                    // Comment out month for articles and proceedings
                    bibEntry.removeField(new Key("month"));
                    addCommentField(bibEntry, "Original month = {" + monthVal.toUserString() + "}");
                }
            }
        }

        writeBibtex(db, outputFile);
        System.out.println("Done. Output written to " + outputFile);
    }

    // Try to extract a 4-digit year from a variety of date formats
    private static String extractYearFromDate(String dateStr) {
        // Patterns for YYYY or YYYY-MM or YYYY-MM-DD, etc.
        Pattern[] patterns = {
            Pattern.compile("^(\\d{4})$"),                       // "2023"
            Pattern.compile("^(\\d{4})[-/][01]?\\d$"),           // "2023-2", "2023/2"
            Pattern.compile("^(\\d{4})[-/][01]?\\d[-/][0-3]?\\d$"), // "2023-02-15"
            Pattern.compile(".*?(\\d{4}).*")                     // anything containing 4 digits
        };
        for (Pattern p : patterns) {
            Matcher m = p.matcher(dateStr);
            if (m.matches()) {
                return m.group(1);
            }
        }
        return null;
    }

    // Add a comment (as a pseudo-field) that is preserved on write
    private static void addCommentField(BibTeXEntry entry, String comment) {
        // Use a pseudo-field so that BibTeXFormatter will print it in the entry
        // This is a common workaround since BibTeX does not have native comments per field
        // We'll use a custom field "__comment" which you can post-process if needed
        int idx = 1;
        while (entry.getField(new Key("__comment" + idx)) != null) idx++;
        entry.addField(new Key("__comment" + idx), new StringValue(comment, StringValue.Style.QUOTED));
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

        // Optionally convert __comment fields to real BibTeX comments here if you wish

        // Convert leading tabs to two spaces (optional, like your original code)
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

