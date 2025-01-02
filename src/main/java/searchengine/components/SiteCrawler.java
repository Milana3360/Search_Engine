package searchengine.components;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import searchengine.config.Lemma;
import searchengine.config.Page;
import searchengine.config.PageLemma;
import searchengine.config.Site;
import searchengine.config.Status;
import searchengine.repositories.*;
import javax.transaction.Transactional;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.springframework.boot.logging.LoggingSystemProperties.LOG_FILE;

@Component
public class SiteCrawler {

    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final PageLemmaRepository pageLemmaRepository;
    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    private volatile boolean isStopped = false;

    private final Set<String> visitedUrls = ConcurrentHashMap.newKeySet();

    public SiteCrawler(PageRepository pageRepository, SiteRepository siteRepository,
                       LemmaRepository lemmaRepository, PageLemmaRepository pageLemmaRepository) {
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.lemmaRepository = lemmaRepository;
        this.pageLemmaRepository = pageLemmaRepository;
    }

    public void crawlSite(Site site) {
        try {
            visitedUrls.clear();
            isStopped = false;

            site.setStatus(Status.INDEXED);
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);

            crawlPage(site, site.getUrl());

            if (!isStopped) {
                site.setStatus(Status.INDEXED);
                site.setStatusTime(LocalDateTime.now());
                siteRepository.save(site);
                System.out.println("Индексация завершена для сайта: " + site.getUrl());
            }
        } catch (Exception e) {
            site.setStatus(Status.FAILED);
            site.setLastError("Ошибка индексации: " + e.getMessage());
            site.setStatusTime(LocalDateTime.now());
            siteRepository.save(site);
            System.err.println("Ошибка при индексации сайта: " + site.getUrl() + ". Причина: " + e.getMessage());
        }
    }

    public void crawlPage(Site site, String url) {
        String normalizedUrl = normalizeUrl(url);

        if (isStopped() || visitedUrls.contains(normalizedUrl)) {
            return;
        }
        visitedUrls.add(normalizedUrl);

        try {
            long startTime = System.nanoTime();

            System.out.println("Подключение к URL: " + normalizedUrl);
            Document document = Jsoup.connect(normalizedUrl).get();
            String content = document.html();

            String title = extractTitle(document);
            int statusCode = 200;

            Optional<Page> existingPageOptional = pageRepository.findByUrlAndSite(normalizedUrl, site);

            Page page;
            if (existingPageOptional.isPresent()) {
                page = existingPageOptional.get();
                page.setContent(content);
                page.setTitle(title);
                page.setCode(statusCode);
                page.setPath(getPathFromUrl(normalizedUrl));
            } else {
                page = new Page();
                page.setUrl(normalizedUrl);
                page.setSite(site);
                page.setContent(content);
                page.setTitle(title);
                page.setCode(statusCode);
                page.setPath(getPathFromUrl(normalizedUrl));
            }

            pageRepository.save(page);
            indexPageBatch(page);

            long endTime = System.nanoTime();
            long durationInNano = endTime - startTime;
            long durationInSeconds = durationInNano / 1_000_000_000;

            Path logDir = Paths.get("logs");
            if (Files.notExists(logDir)) {
                Files.createDirectories(logDir);
            }

            try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(LOG_FILE),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND)) {
                writer.write("Индексация страницы " + normalizedUrl + " заняла: " + durationInSeconds + " секунд.");
                writer.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (isStopped()) {
                return;
            }

            Elements links = document.select("a[href]");
            System.out.println("Извлекаем дочерние ссылки");
            for (Element link : links) {
                if (isStopped()) break;
                String childUrl = link.attr("abs:href");
                if (isValidUrl(childUrl) && childUrl.startsWith(site.getUrl())) {
                    crawlPage(site, childUrl);
                }
            }
        } catch (IOException e) {
            System.err.println("Ошибка обработки URL: " + url + ". Причина: " + e.getMessage());
            saveSiteError(site, "Ошибка обработки URL: " + e.getMessage());
        }
    }

    public void saveSiteError(Site site, String errorMessage) {
        if (errorMessage.length() > 255) {
            errorMessage = errorMessage.substring(0, 255);
        }
        site.setLastError(errorMessage);
        siteRepository.save(site);
    }


    @Transactional
    public void indexPageBatch(Page page) {
        Set<Lemma> lemmas = extractLemmasFromContent(page.getContent(), page.getSite());

        for (Lemma lemma : lemmas) {
            Optional<Lemma> existingLemmaOptional = lemmaRepository.findByLemmaAndSite(lemma.getLemma(), page.getSite());

            Lemma existingLemma;
            if (existingLemmaOptional.isPresent()) {
                existingLemma = existingLemmaOptional.get();
                existingLemma.setFrequency(existingLemma.getFrequency() + lemma.getFrequency());
            } else {
                existingLemma = new Lemma(lemma.getLemma(), page.getSite(), lemma.getFrequency());
                lemmaRepository.save(existingLemma);
            }

            PageLemma pageLemma = new PageLemma(page, existingLemma);
            pageLemmaRepository.save(pageLemma);
        }
    }

    private String extractTitle(Document document) {
        String title = document.title();
        if (title == null || title.isEmpty()) {
            Element h1 = document.selectFirst("h1");
            if (h1 != null) {
                title = h1.text();
            } else {
                title = "Без названия";
            }
        }
        return title.length() > 50 ? title.substring(0, 50) + "..." : title;
    }


    private Set<Lemma> extractLemmasFromContent(String content, Site site) {
        Set<Lemma> lemmas = new HashSet<>();
        String[] words = content.split("\\s+");
        for (String word : words) {
            String lemmaText = word.toLowerCase();
            if (lemmaText.length() > 255) {
                lemmaText = lemmaText.substring(0, 255);
            }
            lemmas.add(new Lemma(lemmaText, site, 1));
        }
        return lemmas;
    }

    private boolean isValidUrl(String url) {
        try {
            new URL(url);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }

    private String normalizeUrl(String url) {
        try {
            URL parsedUrl = new URL(url);
            String protocol = parsedUrl.getProtocol();
            String host = parsedUrl.getHost();
            String path = parsedUrl.getPath();
            return protocol + "://" + host + (path.isEmpty() ? "/" : path);
        } catch (MalformedURLException e) {
            return url;
        }
    }

    private String getPathFromUrl(String url) {
        try {
            URL parsedUrl = new URL(url);
            String path = parsedUrl.getPath();
            return (path == null || path.isEmpty()) ? "/" : path;
        } catch (MalformedURLException e) {
            return "/";
        }
    }

    public void shutdown() {
        isStopped = true;
        executor.shutdown();
        System.out.println("Индексация остановлена.");
    }

    private boolean isStopped() {
        return isStopped;
    }
}
