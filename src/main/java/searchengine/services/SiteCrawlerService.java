package searchengine.services;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import searchengine.config.Lemma;
import searchengine.config.Page;
import searchengine.config.Site;
import searchengine.repositories.*;
import searchengine.config.Status;
import javax.transaction.Transactional;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

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

    @Autowired
    private CrawlRepository crawlRepository;



    @Transactional
    public void indexPage(String url, String content) {
        Site site = siteRepository.findByUrl(url).orElseGet(() -> {
            Site newSite = new Site();
            newSite.setUrl(url);
            newSite.setName("Название сайта");
            newSite.setStatus(Status.INDEXING);
            newSite.setStatusTime(LocalDateTime.now());
            siteRepository.save(newSite);
            return newSite;
        });

        checkIndexingTimeout(site);

        try {
            Page existingPage = crawlRepository.findByUrl(url)
                    .orElseThrow(() -> new IllegalArgumentException("Страница с указанным URL не найдена: " + url));

            if (existingPage != null) {
                existingPage.setContent(content);
                existingPage.setCode(200);
                pageRepository.save(existingPage);
            } else {
                Page page = new Page();
                page.setUrl(url);
                page.setContent(content);
                page.setCode(200);
                page.setPath("/");
                page.setSite(site);
                pageRepository.save(page);
            }

            Set<Lemma> lemmas = extractLemmasFromContent(content, site);
            for (Lemma lemma : lemmas) {
                Lemma existingLemma = lemmaRepository.findByLemmaAndSite(lemma.getLemma(), site).orElse(null);
                if (existingLemma != null) {
                    existingLemma.setFrequency(existingLemma.getFrequency() + lemma.getFrequency());
                    lemmaRepository.save(existingLemma);
                } else {
                    lemmaRepository.save(lemma);
                }
            }

            site.setStatus(Status.INDEXED);
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);
            siteRepository.flush();
            System.out.println("устанавливается значение" + site.getStatus());
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);

        } catch (Exception e) {

            site.setStatus(Status.FAILED);
            System.out.println("устанавливается значение" + site.getStatus());
            site.setStatusTime(LocalDateTime.now());
            site.setLastError(e.getMessage());
            siteRepository.save(site);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            throw e;
        }
    }

    private void checkIndexingTimeout(Site site) {
        Duration duration = Duration.between(site.getStatusTime(), LocalDateTime.now());
        if (site.getStatus() == Status.INDEXING && duration.toMinutes() > 30) {
            site.setStatus(Status.FAILED);
            System.out.println("устанавливается значение" + site.getStatus());
            site.setLastError("Индексация превысила допустимое время.");
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);
        }
    }


    public Set<Lemma> extractLemmasFromContent(String content, Site site) {
        Set<Lemma> lemmas = new HashSet<>();

        String[] words = content.split("\\s+");

        for (String word : words) {
            if (word != null && !word.trim().isEmpty()) {
                String lemmaText = word.toLowerCase();

                if (lemmaText.length() > 255) {
                    lemmaText = lemmaText.substring(0, 255);
                }

                int frequency = 1;
                Lemma lemma = new Lemma(lemmaText, site, frequency);
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