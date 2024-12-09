package searchengine.repositories;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.config.Lemma;
import searchengine.config.Page;
import searchengine.config.PageLemma;

import java.util.Optional;

@Repository
public interface PageLemmaRepository extends JpaRepository<PageLemma, Integer> {
    Optional<PageLemma> findByPageAndLemma(Page page, Lemma lemma);
    boolean existsByPageAndLemma(Page page, Lemma lemma);
}
