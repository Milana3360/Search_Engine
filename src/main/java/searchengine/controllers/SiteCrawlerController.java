package searchengine.controllers;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import searchengine.config.Page;
import searchengine.config.Site;
import searchengine.config.Status;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.SiteCrawlerService;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
public class SiteCrawlerController {

    @Autowired
    private SiteCrawlerService siteCrawlerService;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private SiteRepository siteRepository;

    @PostMapping("/api/indexPage")
    public ResponseEntity<Map<String, Object>> indexPage(@RequestParam String url) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (url == null || !url.startsWith("http://") && !url.startsWith("https://")) {
                response.put("result", false);
                response.put("error", "Некорректный URL: " + url);
                return ResponseEntity.badRequest().body(response);
            }

            Site site = siteRepository.findByUrl(url).orElseGet(() -> {
                Site newSite = new Site();
                newSite.setUrl(url);
                newSite.setName("Новое название сайта");
                newSite.setStatus(Status.INDEXING);
                newSite.setStatusTime(LocalDateTime.now());
                siteRepository.save(newSite);
                return newSite;
            });

            Page existingPage = pageRepository.searchByUrl(url).orElse(null);
            if (existingPage == null) {
                existingPage = new Page();
                existingPage.setUrl(url);
                existingPage.setSite(site);
                existingPage.setContent("");
                existingPage.setCode(200);
                existingPage.setPath(url);
                pageRepository.save(existingPage);
            }

            Document document = Jsoup.connect(url).get();
            String content = document.html();
            siteCrawlerService.indexPage(url, content);

            response.put("result", true);
            response.put("message", "Страница успешно проиндексирована!");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("result", false);
            response.put("error", "Ошибка при индексации страницы: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }



}
