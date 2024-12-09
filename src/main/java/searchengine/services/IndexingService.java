package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.components.MystemLemmatizer;
import searchengine.components.SiteCrawler;
import searchengine.config.*;
import searchengine.repositories.*;

import javax.transaction.Transactional;
import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class IndexingService {

    private final LemmaRepository lemmaRepository;
    private final PageLemmaRepository pageLemmaRepository;
    @Autowired
    private LemmaService lemmaService;

    @Autowired
    private SiteCrawlerService siteCrawlerService;

    private final SiteRepository siteRepository;
    private final SiteCrawler siteCrawler;
    private final PageService pageService;
    private final PageRepository pageRepository;
    private final CrawlRepository crawlRepository;

    public void startIndexing() {
        List<Site> sites = siteRepository.findAll();
        for (Site site : sites) {
            siteCrawler.crawlSite(site);
        }
    }

    public void indexSinglePage(String url) throws IOException {
        Page page = pageService.addOrUpdatePage(url);
        indexPage(page);
    }


    public void stopIndexing() {
        siteCrawler.shutdown();
    }

    @Transactional
    public void indexPage(Page page) throws IOException {
        Document document = Jsoup.connect(page.getUrl()).get();
        String content = document.html();
        page.setContent(content);

        if (content == null || content.isEmpty()) {
            throw new IllegalArgumentException("Контент страницы пустой.");
        }

        Set<Lemma> lemmas = siteCrawlerService.extractLemmasFromContent(content, page.getSite());

        System.out.println("Извлеченные леммы: " + lemmas);

        for (Lemma lemma : lemmas) {
            String lemmaText = lemma.getLemma();
            int frequency = lemma.getFrequency();

            Lemma existingLemma = lemmaRepository.findByLemmaAndSite(lemmaText, page.getSite())
                    .orElse(new Lemma(lemmaText, page.getSite(), 0));

            existingLemma.setFrequency(existingLemma.getFrequency() + frequency);
            lemmaRepository.save(existingLemma);

            System.out.println("Добавлена лемма: " + lemmaText + " для страницы: " + page.getUrl());

            if (!pageLemmaRepository.existsByPageAndLemma(page, existingLemma)) {
                PageLemma pageLemma = new PageLemma(page, existingLemma);
                pageLemmaRepository.save(pageLemma);
                System.out.println("Связь Page-Lemma сохранена: Page ID = " + page.getId() + ", Lemma ID = " + existingLemma.getId());
            }
        }

        System.out.println("Леммы успешно обработаны для страницы: " + page.getUrl());
    }

    public Set<Lemma> extractLemmasFromContent(String content, Site site) {
        Set<Lemma> lemmas = new HashSet<>();
        String lemmatizedText = MystemLemmatizer.lemmatize(content);
        String[] words = lemmatizedText.split("\\s+");

        for (String word : words) {
            if (!word.trim().isEmpty()) {
                Lemma lemma = new Lemma(word.toLowerCase(), site, 1);
                lemmas.add(lemma);
            }
        }
        return lemmas;
    }

    public void indexSite(Site site) throws IOException {
        Queue<String> urlsToIndex = new LinkedList<>();
        urlsToIndex.add(site.getUrl());

        while (!urlsToIndex.isEmpty()) {
            String currentUrl = urlsToIndex.poll();

            try {
                if (crawlRepository.findByUrl(currentUrl) != null) {
                    System.out.println("Страница уже существует в базе данных: " + currentUrl);
                    continue;
                }

                System.out.println("Подключение к URL: " + currentUrl);
                Document document = Jsoup.connect(currentUrl).get();
                String content = document.html();
                System.out.println("Контент страницы: " + document.html());

                Page page = new Page();
                page.setUrl(currentUrl);
                page.setSite(site);
                page.setContent(content);
                pageRepository.save(page);

                indexPage(page);

                Elements links = document.select("a[href]");
                links.forEach(link -> {
                    String childUrl = link.attr("abs:href");
                    System.out.println("Найденная дочерняя ссылка: " + childUrl);
                    if (childUrl.startsWith(site.getUrl())) {
                        urlsToIndex.add(childUrl);
                        System.out.println("Добавлена дочерняя ссылка для индексации: " + childUrl);
                    }
                });

            } catch (Exception e) {
                System.err.println("Ошибка при индексации страницы: " + currentUrl + ". Причина: " + e.getMessage());
            }
        }
    }
}



