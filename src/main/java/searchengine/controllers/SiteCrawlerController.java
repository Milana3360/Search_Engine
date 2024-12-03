package searchengine.controllers;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import searchengine.services.SiteCrawlerService;

import java.io.IOException;
import java.util.Map;

@RestController
public class SiteCrawlerController {

    @Autowired
    private SiteCrawlerService siteCrawlerService;

    @PostMapping("/api/indexPageCrawl")
    public String indexPage(@RequestBody Map<String, String> requestBody) throws IOException {
        String url = requestBody.get("url");
        if (url == null || !url.startsWith("http://") && !url.startsWith("https://")) {
            throw new IllegalArgumentException("Некорректный URL: " + url);
        }
        Document document = Jsoup.connect(url).get();
        String content = document.html();
        siteCrawlerService.indexPage(url, content);
        return "Страница успешно проиндексирована!";
    }
}
