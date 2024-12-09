package searchengine.dto.statistics;
import lombok.Data;
import java.util.List;

@Data
public class StatisticsResponse {
    private boolean result;
    private StatisticsData statistics;
    //private TotalStatistics total;
    //private List<DetailedStatisticsItem> detailed;

}
