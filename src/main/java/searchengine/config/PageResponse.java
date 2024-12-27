package searchengine.config;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PageResponse {

    private String uri;
    private String title;
    private String snippet;
    private double relevance;
    private String site;
    private String siteName;

    public PageResponse() {
    }

}
