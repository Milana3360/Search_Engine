package searchengine.components;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MystemParser {
    public static Map<String, Integer> parseMystemResult(String mystemOutput) {
        Map<String, Integer> lemmaFrequency = new HashMap<>();

        Pattern pattern = Pattern.compile("\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(mystemOutput);

        while (matcher.find()) {
            String lemma = matcher.group(1);
            lemmaFrequency.put(lemma, lemmaFrequency.getOrDefault(lemma, 0) + 1);
        }
        return lemmaFrequency;
    }
}
