package searchengine.repositories;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.config.Site;
import searchengine.config.Status;

import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<Site, Integer> {

    @Query("SELECT s FROM Site s WHERE LOWER(s.url) = LOWER(:url)")
    Optional<Site> findByUrl(@Param("url") String url);

    boolean existsByStatus(Status status);
}
