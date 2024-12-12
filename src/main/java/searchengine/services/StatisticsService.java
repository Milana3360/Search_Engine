package searchengine.services;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.*;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import java.time.LocalDateTime;
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
            item.setUrl(cleanUrl(site.getUrl()));
            item.setName(site.getName());
            item.setStatus(site.getStatus() != null ? site.getStatus().name() : "UNKNOWN");
            item.setStatusTime(getStatusTime(site.getStatusTime()));
            item.setError(site.getLastError());
            item.setPages(pageRepository.findBySiteId(site.getId()).size());
            item.setLemmas(lemmaRepository.findBySiteId(site.getId()).size());
            detailed.add(item);
        }

        StatisticsData statisticsData = new StatisticsData();
        statisticsData.setTotal(total);
        statisticsData.setDetailed(detailed);

        StatisticsResponse response = new StatisticsResponse();
        response.setResult(true);
        response.setStatistics(statisticsData);
        return response;
    }

    private boolean isIndexing() {
        boolean result = siteRepository.existsByStatus(Status.INDEXING);
        System.out.println("Есть ли сайты в статусе INDEXING: " + result);
        return result;
    }

    private String cleanUrl(String url) {
        if (url != null) {
            url = url.replaceAll("[\\{\\}\\\"\\r\\n]", "").trim().replace("url: ", "").replace("www.", "");
        }
        return url != null ? url : "";
    }

    private Long getStatusTime(LocalDateTime statusTime) {
        if (statusTime == null) {
            return 74397403L;
        }
        return statusTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }


}
