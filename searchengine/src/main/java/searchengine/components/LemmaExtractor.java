package searchengine.components;
import java.util.HashMap;
import java.util.Map;

public class LemmaExtractor {

    public static Map<String, Integer> extractLemmas(String content) {
        Map<String, Integer> lemmaFrequency = new HashMap<>();

        String lemmatizedText = MystemLemmatizer.lemmatize(content);
        String[] lemmas = lemmatizedText.split("\\s+");

        System.out.println("Извлечённые леммы: " + lemmaFrequency);
        System.out.println("Результат Mystem: " + lemmatizedText);
        String mystemOutput = MystemLemmatizer.lemmatize("Обновлённый контент страницы");
        Map<String, Integer> lemmass = MystemParser.parseMystemResult(mystemOutput);

        System.out.println("Извлечённые леммы: " + lemmass);

        for (String lemma : lemmas) {
            if (!lemma.isEmpty()) {
                lemmaFrequency.put(lemma, lemmaFrequency.getOrDefault(lemma, 0) + 1);
            }
        }
        return lemmaFrequency;
    }
}