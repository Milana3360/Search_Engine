package searchengine.controllers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.components.SiteCrawler;
import searchengine.config.Lemma;
import searchengine.config.Page;
import searchengine.config.PageRequestDTO;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.services.*;
import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api")
public class ApiController {

    @Autowired
    private final IndexingService indexingService;

    private final StatisticsService statisticsService;

    private final SiteCrawlerService siteCrawlerService;

    @Autowired
    private SearchService searchService;
    private final SiteCrawler siteCrawler;
    private final PageRepository pageRepository;



    @Autowired
    private SiteRepository siteRepository;

    public ApiController(IndexingService indexingService, StatisticsService statisticsService, SiteCrawlerService siteCrawlerService, SearchService searchService, SiteCrawler siteCrawler, SiteRepository siteRepository, PageRepository pageRepository) {
        this.indexingService = indexingService;
        this.statisticsService = statisticsService;
        this.siteCrawlerService = siteCrawlerService;
        this.searchService = searchService;
        this.siteCrawler = siteCrawler;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<Map<String, Object>> startIndexing() {
        try {
            indexingService.startIndexing();
            Map<String, Object> response = new HashMap<>();
            response.put("result", true);
            response.put("error", null);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return null;
        }
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<Map<String, Object>> stopIndexing() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("result", true);
            response.put("error", null);
            siteCrawler.shutdown();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return null;
        }
    }

    @PostMapping("/indexPageLemma")
    public ResponseEntity<?> indexPage(@RequestBody PageRequestDTO request) throws IOException {

        Page page = new Page();
        page.setUrl(request.getUrl());
        page.setTitle(request.getTitle());
        page.setContent(request.getContent());
        List<Lemma> lemmas = extractLemmas(request.getContent());
        indexingService.indexPage(page);

       // searchService.savePageWithLemmas(page, lemmas);

        return ResponseEntity.ok("Page indexed successfully");
    }

    private List<Lemma> extractLemmas(String content) {
        List<Lemma> lemmas = new ArrayList<>();
        String[] words = content.split("\\s+");

        for (String word : words) {
            Lemma lemma = new Lemma();
            lemma.setLemma(word);
            lemmas.add(lemma);
        }
        return lemmas;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> getStatistics() {
            StatisticsResponse statistics = statisticsService.getStatistics();
            System.out.println("Statistics Response: " + statistics);
            return ResponseEntity.ok(statistics);
    }

}
