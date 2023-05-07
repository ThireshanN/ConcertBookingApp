package proj.concert.service.mapper;

import proj.concert.common.dto.SeatDTO;
import proj.concert.service.domain.Seat;

public class SeatMapper {

    private SeatMapper() {}
    // Converts a Seat object to a SeatDTO object.
    public static SeatDTO toDTO(Seat s) {
        return new SeatDTO(s.getLabel(), s.getPrice());
    }
}