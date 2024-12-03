package searchengine.config;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PageResponse {

    private String url;
    private String title;
    private String snippet;
    private double relevance;

}
