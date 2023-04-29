
package proj.concert.service.mapper;

import proj.concert.common.dto.ConcertDTO;
import proj.concert.common.dto.ConcertSummaryDTO;
import proj.concert.service.domain.Concert;


public class ConcertMapper {

    private ConcertMapper() {}

    public static ConcertSummaryDTO toSummaryDTO(Concert concert) {
        return new ConcertSummaryDTO(concert.getId(), concert.getTitle(), concert.getImageName());
    }

    public static ConcertDTO toDTO(Concert concert) {
        ConcertDTO dto = new ConcertDTO(concert.getId(), concert.getTitle(), concert.getImageName(), concert.getBlurb());
        concert.getPerformers().forEach(performer -> dto.getPerformers().add(PerformerMapper.toDTO(performer)));
        dto.getDates().addAll(concert.getDates());
        return dto;
    }

}

