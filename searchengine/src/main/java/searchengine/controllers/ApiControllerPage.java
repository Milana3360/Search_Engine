package searchengine.controllers;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.config.Page;
import searchengine.config.UrlRequest;
import searchengine.services.IndexingService;
import searchengine.services.PageService;

@RestController
@RequestMapping("/api")
public class ApiControllerPage {

    private final PageService pageService;
    private final IndexingService indexingService;

    public ApiControllerPage(PageService pageService, IndexingService indexingService) {
        this.pageService = pageService;
        this.indexingService = indexingService;
    }

    @PostMapping("/indexPage")
    public ResponseEntity<String> indexPage(@RequestBody UrlRequest request) {
        try {
            String url = request.getUrl();
            Page page = pageService.addOrUpdatePage(url);
            indexingService.indexSinglePage(url);
            return ResponseEntity.ok("Страница добавлена или обновлена.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Некорректный URL: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Ошибка при добавлении/обновлении страницы.");
        }
    }


}
