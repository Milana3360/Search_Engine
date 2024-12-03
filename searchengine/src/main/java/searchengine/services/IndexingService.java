package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.components.LemmaExtractor;
import searchengine.components.SiteCrawler;
import searchengine.config.Lemma;
import searchengine.config.Page;
import searchengine.config.PageLemma;
import searchengine.config.Site;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageLemmaRepository;
import searchengine.repositories.SiteRepository;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class IndexingService {

    private final LemmaRepository lemmaRepository;
    private final PageLemmaRepository pageLemmaRepository;
    @Autowired
    private LemmaService lemmaService;

    private final SiteRepository siteRepository;
    private final SiteCrawler siteCrawler;
    private final PageService pageService;

    public void startIndexing() {
        List<Site> sites = siteRepository.findAll();
        for (Site site : sites) {
            siteCrawler.crawlSite(site);
        }
    }

    public void indexSinglePage(String url) {
        Page page = pageService.addOrUpdatePage(url);
        indexPage(page);
    }


    public void stopIndexing() {
        siteCrawler.shutdown();
    }

    public void indexPage(Page page) {
        String content = page.getContent();
        Map<String, Integer> lemmaFrequency = LemmaExtractor.extractLemmas(content);

        for (Map.Entry<String, Integer> entry : lemmaFrequency.entrySet()) {
            String lemmaText = entry.getKey();
            int frequency = entry.getValue();

            Lemma lemma = lemmaRepository.findByLemma(lemmaText)
                    .orElse(new Lemma(lemmaText, page.getSite(), 0));

            lemma.setFrequency(lemma.getFrequency() + frequency);
            lemmaRepository.save(lemma);

            PageLemma pageLemma = new PageLemma(page, lemma);
            pageLemmaRepository.save(pageLemma);

            int siteId = page.getSite().getId();
            lemmaService.saveLemmas(content, siteId);
        }
    }
}
