package searchengine.controllers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import searchengine.services.SiteCrawlerService;

@RestController
public class SiteCrawlerController {

    @Autowired
    private SiteCrawlerService siteCrawlerService;

    @PostMapping("/api/indexPageCrawl")
    public String indexPage(@RequestBody String url) {
        String content = "Skillbox is a platform for learning and skill development.";
        siteCrawlerService.indexPage(url, content);
        return "Страница успешно проиндексирована!";
    }
}
