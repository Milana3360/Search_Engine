package searchengine.config;
import javax.persistence.*;

@Entity
@Table(name = "page_lemma")
public class PageLemma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "page_id", nullable = false)
    private Page page;

    @ManyToOne
    @JoinColumn(name = "lemma_id", nullable = false)
    private Lemma lemma;

    public PageLemma(Page page, Lemma lemma) {
        this.page = page;
        this.lemma = lemma;
    }

    private int frequency;

    public PageLemma() {
    }

    public Page getPage() {
        return page;
    }

    public void setPage(Page page) {
        this.page = page;
    }

    public Lemma getLemma() {
        return lemma;
    }

    public void setLemma(Lemma lemma) {
        this.lemma = lemma;
    }
}
