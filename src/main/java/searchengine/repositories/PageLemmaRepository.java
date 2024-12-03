package searchengine.repositories;
import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.config.Lemma;
import searchengine.config.Page;
import searchengine.config.PageLemma;

import java.util.Optional;

public interface PageLemmaRepository extends JpaRepository<PageLemma, Integer> {
    Optional<PageLemma> findByPageAndLemma(Page page, Lemma lemma);
}
