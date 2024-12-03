package searchengine.config;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PageRequestDTO {

    private String url;
    private String title;
    private String content;

}