package proj.concert.service.mapper;

import proj.concert.common.dto.BookingDTO;
import proj.concert.common.dto.SeatDTO;
import proj.concert.service.domain.Reservation;

import java.util.ArrayList;
import java.util.List;

public class BookingMapper {
    private BookingMapper() {}

    // maps a Reservation entity to a BookingDTO
    public static BookingDTO toDTO(Reservation reservation) {
        // create a new list of SeatDTOs from the seats in the reservation
        List<SeatDTO> seats = new ArrayList<>();
        reservation.getSeats().forEach(seat -> seats.add(SeatMapper.toDTO(seat)));
        return new BookingDTO(reservation.getId(), reservation.getDate(), seats);
    }
}
