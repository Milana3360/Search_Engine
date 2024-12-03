package searchengine.controllers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import searchengine.config.PageResponse;
import searchengine.services.SearchService;
import java.util.List;

@RestController
@RequestMapping("/api")
public class SearchController {

    @Autowired
    private SearchService searchService;

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam String query,
                                    @RequestParam int page,
                                    @RequestParam int size) {
        List<PageResponse> pages = searchService.searchPages(query, page, size);

        if (pages.isEmpty()) {
            return ResponseEntity.ok("No results found for query: " + query);
        }

        return ResponseEntity.ok(pages);
    }
}
