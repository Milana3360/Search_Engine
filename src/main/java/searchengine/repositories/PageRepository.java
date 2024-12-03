package searchengine.repositories;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import searchengine.config.Lemma;
import searchengine.config.Page;
import java.util.List;

public interface PageRepository extends JpaRepository<Page, Integer> {
    Page findByUrl(String url);

    org.springframework.data.domain.Page<Page> findPagesByLemmasIn(List<Lemma> lemmas, PageRequest pageRequest);

    @Query("SELECT DISTINCT p FROM Page p JOIN p.pageLemmas pl WHERE pl.lemma IN :lemmas")
    org.springframework.data.domain.Page<Page> findPagesByLemmasIn(@Param("lemmas") List<Lemma> lemmas, Pageable pageable);
    int countBySiteId(int siteId);
    List<Page> findBySiteId(int siteId);
}