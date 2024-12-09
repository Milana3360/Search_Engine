package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.config.Page;

import java.util.Optional;

@Repository
public interface CrawlRepository extends JpaRepository<Page, Integer> {
    Optional<Page> findByUrl(String url);
}

