package searchengine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "indexing-settings")
public class IndexingSettings {

    private List<SiteInfo> sites;

    @Getter
    @Setter
    public static class SiteInfo {
        private String url;
        private String name;
    }
}