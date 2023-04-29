package proj.concert.service.domain;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.*;

import proj.concert.common.types.Genre;


@Entity
@Table(name = "performers")
public class Performer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Genre genre;

    @Column(columnDefinition = "TEXT")
    private String blurb;

    @Column(nullable = false)
    private String name;

    @Column(name = "ImgName")
    private String imageName;

    @ManyToMany(mappedBy = "performers")
    private Set<Concert> concerts = new HashSet<>();

    public Performer() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }



    public String getImageName() {
        return imageName;
    }

    public Genre getGenre() {
        return genre;
    }

    public String getBlurb() {
        return blurb;
    }



}