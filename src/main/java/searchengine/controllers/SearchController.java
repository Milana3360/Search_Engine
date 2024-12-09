package searchengine.controllers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import searchengine.config.PageResponse;
import searchengine.services.SearchService;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

@RestController
@RequestMapping("/api")
public class SearchController {

    @Autowired
    private SearchService searchService;

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> search(@RequestParam String query,
                                                      @RequestParam int offset,
                                                      @RequestParam int limit) {
        List<PageResponse> searchResults = searchService.searchPages(query, offset, limit);

        Map<String, PageResponse> uniqueResultsMap = new LinkedHashMap<>();
        for (PageResponse response : searchResults) {
            String normalizedUrl = normalizeUrl(response.getUrl());
            uniqueResultsMap.put(normalizedUrl, response);
        }

        List<PageResponse> uniqueResults = new ArrayList<>(uniqueResultsMap.values());

        Map<String, Object> response = new HashMap<>();
        if (uniqueResults.isEmpty()) {
            response.put("result", false);
            response.put("error", "No results found for query: " + query);
        } else {
            response.put("result", true);
            response.put("count", uniqueResults.size());
            response.put("data", uniqueResults);
        }

        return ResponseEntity.ok(response);
    }

    private String normalizeUrl(String url) {
        try {
            URL parsedUrl = new URL(url);
            String protocol = parsedUrl.getProtocol();
            String host = parsedUrl.getHost();
            String path = parsedUrl.getPath();

            String query = parsedUrl.getQuery();
            if (query != null) {
                String[] params = query.split("&");
                Arrays.sort(params);
                query = String.join("&", params);
            }

            return protocol + "://" + host + path + (query != null ? "?" + query : "");
        } catch (MalformedURLException e) {
            return url;
        }
    }





}