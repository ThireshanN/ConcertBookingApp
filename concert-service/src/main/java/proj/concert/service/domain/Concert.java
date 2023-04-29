package proj.concert.service.domain;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.*;


@Entity
@Table(name = "CONCERTS")
public class Concert{



    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;

    @Column(columnDefinition = "TEXT")
    private String blurb;

    @Column(name = "ImgName")
    private String imageName;

    @Column(nullable = false)
    private String concertName;

    private Set<Performer> performers;

    private Set<LocalDateTime> dates = new HashSet<>();


    public Concert() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return concertName;
    }

    public void setName(String title) {
        this.concertName = concertName;
    }

    public Set<LocalDateTime> getDates() {
        return dates;
    }

    public void setDates(Set<LocalDateTime> dates) {
        this.dates = dates;
    }

    public Set<Performer> getPerformers() {
        return performers;
    }

    public void setPerformers(Set<Performer> performers) {
        this.performers = performers;
    }


    public String getTitle() {
        return title;
    }

    public String getImageName() {
        return imageName;
    }

    public String getBlurb() {

        return blurb;
    }


}