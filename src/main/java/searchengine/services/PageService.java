package searchengine.services;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.config.Page;
import searchengine.config.Site;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import javax.transaction.Transactional;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

@Service
public class PageService {

    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final SiteCrawlerService siteCrawlerService;

    public PageService(PageRepository pageRepository, SiteRepository siteRepository, SiteCrawlerService siteCrawlerService) {
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.siteCrawlerService = siteCrawlerService;
    }

    @Transactional
    public Page addOrUpdatePage(String url) {
        try {

            URL parsedUrl = new URL(url);
            String path = parsedUrl.getPath();

            Page page = pageRepository.findByUrl(url);
            if (page == null) {
                page = new Page();
                page.setUrl(url);
                page.setPath(path);
                Document document = Jsoup.connect(url).get();
                String content = document.html();
                page.setContent(content);
                siteCrawlerService.indexPage(url, content);

               // page.setContent("Обновлённый контент страницы");
                page.setCode(200);
                Site site = siteRepository.findById(1)
                        .orElseThrow(() -> new IllegalArgumentException("Сайт не найден"));
                page.setSite(site);
            } else {
                page.setContent(page.getContent());
            }

            pageRepository.save(page);
            System.out.println("Добавляем или обновляем страницу с URL: " + url);

            return page;

        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Некорректный URL: " + url);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Transactional
    public void indexPage(String url) {
          try {
            String content = getPageContent(url);
            System.out.println("Индексация страницы с URL: " + url);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getPageContent(String pageUrl) throws IOException {
        Document doc = Jsoup.connect(pageUrl).get();
        return doc.html();
    }
}
