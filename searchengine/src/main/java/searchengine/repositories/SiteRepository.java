package searchengine.repositories;
import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.config.Site;
import searchengine.config.Status;

import java.util.Optional;

public interface SiteRepository extends JpaRepository<Site, Integer> {
    Optional<Site> findByUrl(String url);

    boolean existsByStatus(Status status);
}
