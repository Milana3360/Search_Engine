package searchengine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import searchengine.config.IndexingSettings;
import searchengine.config.Lemma;
import searchengine.repositories.LemmaRepository;

import java.util.Collections;
import java.util.List;


@SpringBootApplication
@EnableConfigurationProperties(IndexingSettings.class)
public class Application {


    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
