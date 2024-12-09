package searchengine.controllers;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.config.Page;
import searchengine.config.UrlRequest;
import searchengine.services.IndexingService;
import searchengine.services.PageService;
import searchengine.services.SiteCrawlerService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiControllerPage {

    @Autowired
    private SiteCrawlerService siteCrawlerService;

    private final PageService pageService;
    private final IndexingService indexingService;

    public ApiControllerPage(PageService pageService, IndexingService indexingService) {
        this.pageService = pageService;
        this.indexingService = indexingService;
    }

    @PostMapping(value = "/indexPageCrawl")
    public ResponseEntity<Map<String, Object>> indexPage(@RequestParam("url") String url) {
        Map<String, Object> response = new HashMap<>();
        try {
            pageService.addOrUpdatePage(url);
            indexingService.indexSinglePage(url);
            response.put("result", true);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("result", false);
            response.put("error", "Некорректный URL: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("result", false);
            response.put("error", "Ошибка при добавлении/обновлении страницы: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }


}
