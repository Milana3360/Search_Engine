package searchengine.controllers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.components.SiteCrawler;
import searchengine.config.Lemma;
import searchengine.config.Page;
import searchengine.config.PageRequestDTO;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;
import searchengine.services.PageService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ApiController {

    @Autowired
    private final IndexingService indexingService;

    private final StatisticsService statisticsService;

    @Autowired
    private SearchService searchService;
    private final SiteCrawler siteCrawler;

    public ApiController(IndexingService indexingService, StatisticsService statisticsService, SiteCrawler siteCrawler) {
        this.indexingService = indexingService;
        this.statisticsService = statisticsService;
        this.siteCrawler = siteCrawler;
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<String> startIndexing() {
        try {
            indexingService.startIndexing();
            return ResponseEntity.ok("Индексация начата.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка при запуске индексации.");
        }
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<String> stopIndexing() {
        try {
            siteCrawler.shutdown();
            return ResponseEntity.ok("Индексация остановлена.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка при остановке индексации.");
        }
    }

    @PostMapping("/indexPageLemma")
    public ResponseEntity<?> indexPage(@RequestBody PageRequestDTO request) {

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
        return ResponseEntity.ok(statistics);
    }
}
