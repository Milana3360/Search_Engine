package searchengine.components;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import searchengine.config.Site;
import java.io.IOException;
import java.util.concurrent.RecursiveTask;
import java.util.Set;

public class CrawlTask extends RecursiveTask<Void> {
    private final Site site;
    private final String url;
    private final Set<String> visitedUrls;
    public CrawlTask(Site site, String url, Set<String> visitedUrls) {
        this.site = site;
        this.url = url;
        this.visitedUrls = visitedUrls;
    }

    @Override
    protected Void compute() {
        if (visitedUrls.contains(url)) {
            return null;
        }
        visitedUrls.add(url);

        try {
            Document document = Jsoup.connect(url).get();
            System.out.println("Обработана страница: " + url);
            Elements links = document.select("a[href]");
            for (var link : links) {
                String href = link.attr("abs:href");
                new CrawlTask(site, href, visitedUrls).fork();
            }
        } catch (IOException e) {
            System.err.println("Ошибка обработки URL: " + url);
        }
        return null;
    }

}

