package searchengine.services;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.components.MystemLemmatizer;
import searchengine.components.MystemParser;
import searchengine.repositories.LemmaRepository;
import java.util.Map;

@Service
public class LemmaService {

    @Autowired
    private LemmaRepository lemmaRepository;

    public void saveLemmas(String text, int siteId) {
        String mystemOutput = MystemLemmatizer.lemmatize(text);

        Map<String, Integer> lemmas = MystemParser.parseMystemResult(mystemOutput);

        for (Map.Entry<String, Integer> entry : lemmas.entrySet()) {
            String lemma = entry.getKey();
            int frequency = entry.getValue();

            lemmaRepository.saveOrUpdateLemma(lemma, frequency, siteId);
        }
    }
}

