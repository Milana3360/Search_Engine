package searchengine.services;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.Lemma;
import searchengine.config.Page;
import searchengine.config.PageLemma;
import searchengine.config.Site;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageLemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.config.Status;
import javax.transaction.Transactional;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
public class SiteCrawlerService {

    @Autowired
    private PageRepository pageRepository;

    @Autowired
    private LemmaRepository lemmaRepository;

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private PageLemmaRepository pageLemmaRepository;

    @Transactional
    public void indexPage(String url, String content) {

        Site site = siteRepository.findByUrl(url).orElseGet(() -> {
            Site newSite = new Site();
            newSite.setUrl(url);
            newSite.setName("Название сайта");
            newSite.setStatus(Status.INDEXING);
            siteRepository.save(newSite);
            return newSite;
        });

        Set<Lemma> lemmas = extractLemmasFromContent(content, site);

        lemmaRepository.saveAll(lemmas);

        Page page = new Page();
        page.setUrl(url);
        page.setContent(content);
        page.setCode(200);

        String path = "/";
        page.setPath(path);
        page.setSite(site);

        pageRepository.save(page);

        for (Lemma lemma : lemmas) {
            PageLemma pageLemma = new PageLemma(page, lemma);
            pageLemmaRepository.save(pageLemma);
        }
    }

    private Set<Lemma> extractLemmasFromContent(String content, Site site) {
        Set<Lemma> lemmas = new HashSet<>();

        String[] words = content.split("\\s+");

        for (String word : words) {
            if (word != null && !word.trim().isEmpty()) {
                int frequency = 1;
                Lemma lemma = new Lemma(word.toLowerCase(), site, frequency);
                lemmas.add(lemma);
            }
        }

        System.out.println("Извлечённые леммы: " + lemmas);
        return lemmas;
    }

    @Service
    @Transactional
    public class LemmaService {
        public void saveLemmas(Map<String, Integer> lemmaFrequencyMap, Site site) {
            for (Map.Entry<String, Integer> entry : lemmaFrequencyMap.entrySet()) {
                String lemmaText = entry.getKey();
                int frequency = entry.getValue();

                System.out.println("Сохраняем лемму: " + lemmaText + ", частота: " + frequency + ", сайт ID: " + site.getId());

                lemmaRepository.saveOrUpdateLemma(lemmaText, frequency, site.getId());
            }
        }
    }
}