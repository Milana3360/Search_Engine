package searchengine.config;
import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Setter
@Getter
public class Site {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private String name;

    private LocalDateTime statusTime;

    private String lastError;
}
