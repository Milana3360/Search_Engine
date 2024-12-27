package searchengine.controllers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import searchengine.config.PageResponse;
import searchengine.config.SearchResponse;
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
                                                          @RequestParam(required = false, defaultValue = "20") int limit) {

            Pageable pageable = PageRequest.of(offset / limit, limit);

            SearchResponse searchResponse = searchService.searchPagesWithLemmas(query, offset, limit);

            Map<String, Object> response = new HashMap<>();

            if (!searchResponse.isResult()) {
                response.put("result", false);
                response.put("error", "No results found for query: " + query);
            } else {
                response.put("result", true);
                response.put("count", searchResponse.getCount());
                response.put("data", searchResponse.getData());
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