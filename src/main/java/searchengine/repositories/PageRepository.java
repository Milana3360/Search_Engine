package searchengine.repositories;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchengine.config.Lemma;
import searchengine.config.Page;
import searchengine.config.Site;

import java.util.List;
import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {
    Page findByUrl(String url);

    org.springframework.data.domain.Page<Page> findPagesByLemmasIn(List<Lemma> lemmas, PageRequest pageRequest);

    @Query("SELECT DISTINCT p FROM Page p JOIN p.pageLemmas pl WHERE pl.lemma IN :lemmas")
    org.springframework.data.domain.Page<Page> findPagesByLemmasIn(@Param("lemmas") List<Lemma> lemmas, Pageable pageable);
    int countBySiteId(int siteId);
    List<Page> findBySiteId(int siteId);


    @Query("SELECT p FROM Page p JOIN p.lemmas l WHERE l IN :lemmas AND p.site.name LIKE %:siteName%")
    List<Page> findPagesByLemmasInAndSiteName(@Param("lemmas") List<Lemma> lemmas,
                                              @Param("siteName") String siteName,
                                              Pageable pageable);

     List<Page> findBySite(Site site);

    Optional<Page> searchByUrl(String url);


    boolean existsByUrlAndSite(String url, Site site);










}