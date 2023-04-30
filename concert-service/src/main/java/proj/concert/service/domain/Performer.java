package proj.concert.service.domain;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.*;

import proj.concert.common.types.Genre;


@Entity
@Table(name = "PERFORMERS")
public class Performer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column( name = "ID")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column( name = "GENRE")
    private Genre genre;

    @Column( columnDefinition = "TEXT", name = "BLURB")
    private String blurb;

    @Column(nullable = false, name="NAME")
    private String name;

    @Column(name = "IMGNAME")
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

    public void setBlurb(String blurb) {
        this.blurb = blurb;
    }

}