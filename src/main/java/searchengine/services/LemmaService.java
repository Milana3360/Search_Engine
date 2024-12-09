package searchengine.services;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.components.MystemLemmatizer;
import searchengine.components.MystemParser;
import searchengine.repositories.LemmaRepository;

import javax.transaction.Transactional;
import java.util.Map;

@Service
public class LemmaService {

    @Autowired
    private LemmaRepository lemmaRepository;

    @Transactional
    public void saveLemmas(String text, int siteId) {
        String mystemOutput = String.valueOf(MystemLemmatizer.lemmatize(text));

        Map<String, Integer> lemmas = MystemParser.parseMystemResult(mystemOutput);

        for (Map.Entry<String, Integer> entry : lemmas.entrySet()) {
            String lemma = entry.getKey();
            int frequency = entry.getValue();

            lemmaRepository.saveOrUpdateLemma(lemma, frequency, siteId);
        }
    }
}

