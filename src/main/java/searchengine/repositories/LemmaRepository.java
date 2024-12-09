package searchengine.repositories;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.config.Lemma;
import searchengine.config.Site;
import java.util.List;
import java.util.Optional;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    List<Lemma> findByLemmaContaining(String lemma);

    Optional<Lemma> findByLemmaAndSite(String lemma, Site site);

    @Modifying
    @Query(value = "INSERT INTO lemma (lemma, frequency, site_id) " +
            "VALUES (:lemma, :frequency, :siteId) " +
            "ON DUPLICATE KEY UPDATE frequency = frequency + :frequency", nativeQuery = true)
    void saveOrUpdateLemma(@Param("lemma") String lemma,
                           @Param("frequency") int frequency,
                           @Param("siteId") int siteId);

    @Query("SELECT l FROM Lemma l WHERE l.lemma IN :lemmas")
    List<Lemma> findByLemmaIn(@Param("lemmas") List<String> lemmas);

    Optional<Lemma> findByLemma(String lemma);

    int countBySiteId(int siteId);

    List<Lemma> findBySiteId(int siteId);


}
