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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
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
    private final CrawlRepository crawlRepository;

    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    private volatile boolean isStopped = false;

    private final Set<String> visitedUrls = ConcurrentHashMap.newKeySet();

    public SiteCrawler(PageRepository pageRepository, SiteRepository siteRepository,
                       LemmaRepository lemmaRepository, PageLemmaRepository pageLemmaRepository,
                       CrawlRepository crawlRepository) {
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.lemmaRepository = lemmaRepository;
        this.pageLemmaRepository = pageLemmaRepository;
        this.crawlRepository = crawlRepository;
    }

    public void crawlSite(Site site) {
        try {

            visitedUrls.clear();
            isStopped = false;

            if (site.getStatus() == Status.INDEXING) {
                site.setStatus(Status.FAILED);
                site.setLastError("Индексация была прервана. Перезапуск.");
                site.setStatusTime(LocalDateTime.now());
                siteRepository.save(site);
            }

            site.setStatus(Status.INDEXED);
            site.setStatusTime(LocalDateTime.now());
            site.setLastError(null);
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
            System.out.println("Подключение к URL: " + normalizedUrl);
            Document document = Jsoup.connect(normalizedUrl).get();
            String content = document.html();
            String title = document.title();
            int statusCode = 200;

            Optional<Page> existingPageOptional = crawlRepository.findByUrl(normalizedUrl);

            Page page;
            if (existingPageOptional.isPresent()) {
                page = existingPageOptional.get();
                page.setContent(content);
                page.setTitle(title);
                page.setCode(statusCode);
                page.setPath(normalizedUrl);
            } else {
                page = new Page();
                page.setUrl(normalizedUrl);
                page.setSite(site);
                page.setContent(content);
                page.setTitle(title);
                page.setCode(statusCode);
                page.setPath(normalizedUrl);
            }

            pageRepository.save(page);
            indexPage(page);

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

            path = (path == null || path.equals("/")) ? "" : path;

            return protocol + "://" + host + path;
        } catch (MalformedURLException e) {
            return url;
        }
    }

    public void shutdown() {
        isStopped = true;

        if (SpringContext.isShuttingDown()) {
            System.out.println("Приложение завершает работу. Остановка индексации пропущена.");
            return;
        }

        System.out.println("Индексация остановлена.");
    }


    private boolean isStopped() {
        return isStopped;
    }
}
