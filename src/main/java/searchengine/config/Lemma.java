package searchengine.config;
import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.util.List;

@Entity
@Getter
@Setter
public class Lemma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, unique = true)
    private String lemma;

    @ManyToOne
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(nullable = false)
    private int frequency;

    @OneToMany(mappedBy = "lemma", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PageLemma> pageLemmas;

    public Lemma() {}

    public Lemma(String lemma, Site site, int frequency) {
        this.lemma = lemma.toLowerCase();
        this.site = site;
        this.frequency = frequency;
    }

    @Override
    public String toString() {
        return "Lemma{" +
                "lemma='" + lemma + '\'' +
                ", frequency=" + frequency + '}';
    }
}
