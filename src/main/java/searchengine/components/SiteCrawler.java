package searchengine.components;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import searchengine.config.Lemma;
import searchengine.config.Page;
import searchengine.config.PageLemma;
import searchengine.config.Site;
import searchengine.repositories.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class SiteCrawler {

    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final PageLemmaRepository pageLemmaRepository;
    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    private final CrawlRepository crawlRepository;
    private volatile boolean isStopped = false;

    private final Set<String> visitedUrls = ConcurrentHashMap.newKeySet();

    public SiteCrawler(PageRepository pageRepository, SiteRepository siteRepository,LemmaRepository lemmaRepository,PageLemmaRepository pageLemmaRepository, CrawlRepository crawlRepository ) {
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.lemmaRepository = lemmaRepository;
        this.pageLemmaRepository = pageLemmaRepository;
        this.crawlRepository = crawlRepository;
    }

    public void crawlSite(Site site) {
        crawlPage(site, site.getUrl());  // Запуск индексации для главной страницы
    }

    @Async
    public void crawlPage(Site site, String url) {

        if (isStopped() || visitedUrls.contains(url)) {
            return;
        }
        visitedUrls.add(url);

        try {
            System.out.println("Подключение к URL: " + url);
            Document document = Jsoup.connect(url).get();
            String content = document.html();
            int statusCode = 200;

            Optional<Page> existingPageOptional = crawlRepository.findByUrl(url);

            Page page;
            if (existingPageOptional.isPresent()) {
                System.out.println("обновляем страницу тк она существует");
                page = existingPageOptional.get();
                page.setContent(content);
                page.setCode(statusCode);
                page.setTitle("Page Title");
                page.setPath(url);
            } else {
                System.out.println("создаем новую страницу тк её нет");
                page = new Page();
                page.setUrl(url);
                page.setSite(site);
                page.setContent(content);
                page.setCode(statusCode);
                page.setTitle("Page Title");
                page.setPath(url);
            }

            pageRepository.save(page);
            indexPage(page);

            if (isStopped()) {
                return;
            }

            Elements links = document.select("a[href]");
            System.out.println("извлекаем дочерние ссылки");
            for (Element link : links) {
                if (isStopped()) break;
                String childUrl = link.attr("abs:href");
                if (isValidUrl(childUrl) && childUrl.startsWith(site.getUrl())) {
                    crawlPage(site, childUrl);
                }
            }
        } catch (IOException e) {
            System.err.println("Ошибка обработки URL: " + url + ". Причина: " + e.getMessage());
        }
    }

    private boolean isValidUrl(String url) {
        try {
            new URL(url);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }

    private void indexPage(Page page) {
        Set<Lemma> lemmas = extractLemmasFromContent(page.getContent(), page.getSite());

        for (Lemma lemma : lemmas) {
            Lemma existingLemma = lemmaRepository.findByLemmaAndSite(lemma.getLemma(), page.getSite())
                    .orElse(new Lemma(lemma.getLemma(), page.getSite(), 0));

            existingLemma.setFrequency(existingLemma.getFrequency() + lemma.getFrequency());
            lemmaRepository.save(existingLemma);

            PageLemma pageLemma = new PageLemma(page, existingLemma);
            pageLemmaRepository.save(pageLemma);
        }
    }

    private Set<Lemma> extractLemmasFromContent(String content, Site site) {
        Set<Lemma> lemmas = new HashSet<>();

        String[] words = content.split("\\s+");

        for (String word : words) {
            String lemmaText = word.toLowerCase();

            if (lemmaText.length() > 255) {
                lemmaText = lemmaText.substring(0, 255);
            }

            Lemma lemma = new Lemma(lemmaText, site, 1);
            lemmas.add(lemma);
        }

        return lemmas;
    }

    public void shutdown() {
        isStopped = true;
        executor.shutdownNow();
    }

    private boolean isStopped() {
        return isStopped;
    }
}
