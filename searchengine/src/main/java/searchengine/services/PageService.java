package searchengine.services;
import org.springframework.stereotype.Service;
import searchengine.config.Page;
import searchengine.config.Site;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import javax.transaction.Transactional;
import java.net.MalformedURLException;
import java.net.URL;

@Service
public class PageService {

    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;

    public PageService(PageRepository pageRepository, SiteRepository siteRepository) {
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
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
                page.setContent("Пустой контент");
                page.setCode(200);
                Site site = siteRepository.findById(1)
                        .orElseThrow(() -> new IllegalArgumentException("Сайт не найден"));
                page.setSite(site);
            } else {
                page.setContent("Обновлённый контент страницы");
            }

            pageRepository.save(page);
            return page;

        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Некорректный URL: " + url);
        }
    }
}