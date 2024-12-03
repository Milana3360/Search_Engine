package searchengine.services;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.*;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final SiteRepository siteRepository;

    public StatisticsResponse getStatistics() {
        TotalStatistics total = new TotalStatistics();
        total.setSites((int) siteRepository.count());
        total.setPages((int) pageRepository.count());
        total.setLemmas((int) lemmaRepository.count());
        total.setIndexing(isIndexing());

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        for (Site site : siteRepository.findAll()) {
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setUrl(site.getUrl());
            item.setName(site.getName());

            item.setStatus(site.getStatus() != null ? site.getStatus().name() : "UNKNOWN");
            item.setStatusTime(site.getStatusTime() != null
                    ? site.getStatusTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    : 0L);
            item.setError(site.getLastError());

            List<Page> pages = pageRepository.findBySiteId(site.getId());
            List<Lemma> lemmas = lemmaRepository.findBySiteId(site.getId());

            item.setPages(pages.size());
            item.setLemmas(lemmas.size());
            detailed.add(item);
        }

        StatisticsResponse response = new StatisticsResponse();
        response.setResult(true);
        response.setTotal(total);
        response.setDetailed(detailed);

        return response;
    }

    private boolean isIndexing() {
        return siteRepository.existsByStatus(Status.INDEXING);
    }
}