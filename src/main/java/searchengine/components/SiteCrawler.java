package searchengine.components;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import searchengine.config.Page;
import searchengine.config.Site;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

@Component
@RequiredArgsConstructor
public class SiteCrawler {

    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final ForkJoinPool forkJoinPool = new ForkJoinPool();
    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    private final Set<String> visitedUrls = ConcurrentHashMap.newKeySet();

    public void crawlSite(Site site) {
        forkJoinPool.submit(() -> {
            new CrawlTask(site, site.getUrl(), visitedUrls).fork();
        });
    }

    private void crawlPage(Site site, String url) {
        if (visitedUrls.contains(url)) {
            return;
        }
        visitedUrls.add(url);

        try {
            Document document = Jsoup.connect(url).get();
            System.out.println("Страница успешно обработана " + url);
            String content = document.html();
            int statusCode = 200;

            Page page = new Page();
            page.setSite(site);
            page.setPath(url);
            page.setCode(statusCode);
            page.setContent(content);
            pageRepository.save(page);

            Elements links = document.select("a[href]");
            links.forEach(link -> {
                String href = link.attr("abs:href");
                crawlPage(site, href);
            });
        } catch (IOException e) {
            System.err.println("Ошибка обработки URL: " + url);
        }
    }

    public void shutdown() {
        forkJoinPool.shutdownNow();
    }

}