package searchengine.config;
import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.util.List;

@Entity
@Getter
@Setter
public class Page {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String path;

    @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;

    @Column(nullable = false, unique = true)
    private String url;

    @Column(nullable = false)
    private int code;

    @ManyToMany
    @JoinTable(
            name = "page_lemma",
            joinColumns = @JoinColumn(name = "page_id"),
            inverseJoinColumns = @JoinColumn(name = "lemma_id")
    )
    private List<Lemma> lemmas;

    @OneToMany(mappedBy = "page", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PageLemma> pageLemmas;

    private String title;

    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(
            name = "page_lemma",
            joinColumns = @JoinColumn(name = "page_id"),
            inverseJoinColumns = @JoinColumn(name = "lemma_id")
    )
    private List<Lemma> lemmass;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

}
