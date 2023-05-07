package proj.concert.service.mapper;

import proj.concert.common.dto.PerformerDTO;
import proj.concert.service.domain.Performer;

public class PerformerMapper {

    // Private constructor to prevent instantiation of the class
    private PerformerMapper() {}
    public static PerformerDTO toDTO(Performer performer) {
        // Create and return a new PerformerDTO object with the Performer entity's properties
        return new PerformerDTO(performer.getId(), performer.getName(), performer.getImageName(), performer.getGenre(), performer.getBlurb());
    }

}