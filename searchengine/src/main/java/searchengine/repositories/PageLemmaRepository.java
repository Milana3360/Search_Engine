package searchengine.repositories;
import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.config.PageLemma;

public interface PageLemmaRepository extends JpaRepository<PageLemma, Integer> {
}
