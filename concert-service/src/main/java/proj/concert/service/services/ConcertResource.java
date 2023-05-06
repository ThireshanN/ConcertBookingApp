package proj.concert.service.services;

import org.apache.log4j.Priority;
import proj.concert.common.dto.*;
import proj.concert.service.domain.*;
import proj.concert.service.jaxrs.LocalDateTimeParam;
import proj.concert.service.mapper.BookingMapper;
import proj.concert.service.mapper.ConcertMapper;
import proj.concert.service.mapper.PerformerMapper;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import proj.concert.service.mapper.SeatMapper;

@Path("/concert-service")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ConcertResource {

    private static final Logger LOGGER = Logger.getLogger(ConcertResource.class);
    @GET
    @Path("/concerts")
    public Response getAllConcerts() {
        EntityManager em = PersistenceManager.instance().createEntityManager();
        try {
            em.getTransaction().begin();
            List<Concert> concerts = em.createQuery("SELECT c FROM Concert c", Concert.class)
                    .getResultList();

            if (concerts.isEmpty()) {
                return Response.noContent().build();
            }

            List<ConcertDTO> concertDTOS = new ArrayList<>();
            for (Concert concert : concerts) {
                concertDTOS.add(ConcertMapper.toDTO(concert));
            }
            em.getTransaction().commit();
            GenericEntity<List<ConcertDTO>> out = new GenericEntity<List<ConcertDTO>>(concertDTOS) {};
            return Response.ok(out).build();
        } catch (Exception e) { //block to handle exceptions, rolling back the transaction in case of an error.
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
//            e.printStackTrace(); //FOR DEBUGGING
            return Response.serverError().entity("Error retrieving concerts: " + e.getMessage()).build();
        } finally {
            em.close();
        }
    }

    @GET
    @Path("/concerts/{id}")
    public Response getConcert(@PathParam("id") long id) {
        EntityManager entityManager = PersistenceManager.instance().createEntityManager();

        try {
            entityManager.getTransaction().begin();
            Concert foundConcert = entityManager.find(Concert.class, id);
            entityManager.getTransaction().commit();

            if (foundConcert == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            ConcertDTO concertDTO = ConcertMapper.toDTO(foundConcert);
            return Response.ok(concertDTO).build();
        } catch (Exception e) { //block to handle exceptions, rolling back the transaction in case of an error.
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            entityManager.close();
        }
    }

    private EntityManager em = PersistenceManager.instance().createEntityManager();

    @GET
    @Path("/concerts/summaries")
    public Response getConcertSummaries() {
        EntityManager em = PersistenceManager.instance().createEntityManager();
        try {
            em.getTransaction().begin(); // Start a transaction
            List<ConcertSummaryDTO> summaries = em.createQuery("SELECT c FROM Concert c", Concert.class)
                    .setLockMode(LockModeType.PESSIMISTIC_READ)
                    .getResultList().stream()
                    .map(ConcertMapper::toSummaryDTO)
                    .collect(Collectors.toList());

            if (summaries.isEmpty()) {
                LOGGER.log(Priority.WARN, "No concert summaries found.");
                return Response.noContent().build();
            }

            GenericEntity<List<ConcertSummaryDTO>> entity = new GenericEntity<List<ConcertSummaryDTO>>(summaries) {};

            em.getTransaction().commit(); // Commit the transaction
            return Response.ok(entity).build();
        } catch (Exception e) {
            // Log the exception and return an appropriate error response
            LOGGER.log(Priority.ERROR, "An error occurred while processing the request.", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("An error occurred while processing the request.").build();
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

    @GET
    @Path("/performers")
    public Response getAllPerformers() {
        EntityManager entityManager = PersistenceManager.instance().createEntityManager();

        try {
            entityManager.getTransaction().begin();
            List<Performer> performers = entityManager.createQuery("SELECT p FROM Performer p", Performer.class).getResultList();
            entityManager.getTransaction().commit();

            if (performers.isEmpty()) {
                return Response.noContent().build();
            }

            List<PerformerDTO> performerDTOs = new ArrayList<>();

            for(Performer performer:performers){
                performerDTOs.add(PerformerMapper.toDTO(performer));
            }
            GenericEntity<List<PerformerDTO>> entity = new GenericEntity<List<PerformerDTO>>(performerDTOs) {};

            return Response.ok(entity).build();
        } catch (Exception e) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            entityManager.close();
        }
    }

    @GET
    @Path("/performers/{id}")
    public Response getPerformer(@PathParam("id") long id) {
        EntityManager entityManager = PersistenceManager.instance().createEntityManager();

        try {
            entityManager.getTransaction().begin();
            Performer foundPerformer = entityManager.find(Performer.class, id);
            entityManager.getTransaction().commit();

            if (foundPerformer == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            PerformerDTO performerDTO = PerformerMapper.toDTO(foundPerformer);
            return Response.ok(performerDTO).build();
        } catch (Exception e) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            entityManager.close();
        }
    }

    @POST
    @Path("/login")
    public Response login(UserDTO creds) {
        EntityManager em = PersistenceManager.instance().createEntityManager();
        try {
            em.getTransaction().begin();
            TypedQuery<User> queryForUser = em.createQuery("SELECT u FROM User u where u.username = :username AND u.password = :password", User.class)
                    .setParameter("username", creds.getUsername())
                    .setParameter("password", creds.getPassword())
                    .setLockMode(LockModeType.PESSIMISTIC_READ);
            User user;
            try {
                user = queryForUser.getSingleResult();
            } catch (NoResultException e) {
                em.getTransaction().rollback();
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }
            user.setSessionId(UUID.randomUUID());
            em.lock(user, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
            em.getTransaction().commit();
            return Response.ok().cookie(new NewCookie("auth", user.getSessionId().toString())).build();
        } finally {
            em.close();
        }
    }

    @POST
    @Path("/bookings")
    public Response createNewBooking(BookingRequestDTO bookingDetails, @CookieParam("auth") Cookie authCookie) {
        EntityManager em = PersistenceManager.instance().createEntityManager();
        User currentUser;
        if (authCookie == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        try {
            em.getTransaction().begin();
            currentUser = em.createQuery("SELECT u FROM User u where u.sessionId = :sessionId", User.class)
                    .setParameter("sessionId", UUID.fromString(authCookie.getValue()))
                    .setLockMode(LockModeType.OPTIMISTIC)
                    .getSingleResult();
            em.getTransaction().commit();
        } catch (NoResultException e) {
            em.getTransaction().rollback();
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        em.getTransaction().begin();
        try {
            Concert targetConcert = em.find(Concert.class, bookingDetails.getConcertId(), LockModeType.PESSIMISTIC_READ);
            if (targetConcert == null || !targetConcert.getDates().contains(bookingDetails.getDate())) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }

            List<Seat> openRequestedSeats = em.createQuery("SELECT s FROM Seat s WHERE s.label IN :seatLabels AND s.date = :eventDate AND s.isBooked = false", Seat.class)
                    .setParameter("seatLabels", bookingDetails.getSeatLabels())
                    .setParameter("eventDate", bookingDetails.getDate())
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    .getResultList();

            if (openRequestedSeats.size() != bookingDetails.getSeatLabels().size()) {
                em.getTransaction().rollback();
                return Response.status(Response.Status.FORBIDDEN).build();
            }

            Reservation newReservation = new Reservation(bookingDetails.getConcertId(), bookingDetails.getDate());
            newReservation.getSeats().addAll(openRequestedSeats);
            openRequestedSeats.forEach(seat -> seat.setIsBooked(true));
            currentUser.addReservation(newReservation);
            em.persist(newReservation);
            em.getTransaction().commit();
            return Response.created(URI.create("/concert-service/bookings/" + newReservation.getId())).build();

        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            return Response.serverError().build();
        } finally {
            em.close();
        }
    }


    private User authenticateUser(String authCookie) {
        if (authCookie == null) {
            return null;
        }

        try {
            UUID sessionId = UUID.fromString(authCookie);
            return getUserBySessionId(sessionId);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public User getUserBySessionId(UUID sessionId) {
        TypedQuery<User> query = em.createQuery("SELECT u FROM User u WHERE u.sessionId = :sessionId", User.class);
        query.setParameter("sessionId", sessionId);
        try {
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public List<Reservation> getBookingsByUser(User user) {
        TypedQuery<Reservation> query = em.createQuery("SELECT b FROM Reservation b WHERE b.user = :user", Reservation.class);
        query.setParameter("user", user);
        return query.getResultList();
    }

    @Path("/bookings")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<BookingDTO> getBookings(@CookieParam("auth") String authCookie) {
        if (authCookie == null) {
            throw new NotAuthorizedException("User not logged in");
        }
        User user = authenticateUser(authCookie);
        if (user == null) {
            throw new NotAuthorizedException("Invalid authentication token");
        }

        List<Reservation> bookings = getBookingsByUser(user);
        List<BookingDTO> bookingDTOs = new ArrayList<>();
        for (Reservation booking : bookings) {
            bookingDTOs.add(BookingMapper.toDTO(booking));
        }
        return bookingDTOs;
    }
    @GET
    @Path("/bookings/{id}")
    public Response getBooking(@PathParam("id") Long id, @CookieParam("auth") Cookie authCookie) {

        try {
            em.getTransaction().begin();
            User user = null;
            if (authCookie != null) {
                try {
                    user = em.createQuery("SELECT u FROM User u where u.sessionId = :uuid", User.class)
                            .setParameter("uuid", UUID.fromString(authCookie.getValue()))
                            .setLockMode(LockModeType.OPTIMISTIC)
                            .getSingleResult();
                } catch (NoResultException e) {
                    return Response.status(Response.Status.UNAUTHORIZED).build();
                }
            }
            if (user == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }
            Reservation booking = em.find(Reservation.class, id, LockModeType.PESSIMISTIC_READ);
            if (booking == null || !booking.getUser().equals(user)) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }
            BookingDTO dtoBooking = BookingMapper.toDTO(booking);
            return Response.ok(dtoBooking).build();
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().commit();
            }
            em.close();
        }
    }
    @GET
    @Path("/seats/{date}")
    public Response getSeatsByDateAndStatus(@PathParam("date") String dateString, @DefaultValue("Any") @QueryParam("status") String status) {
        LocalDateTime date = new LocalDateTimeParam(dateString).getLocalDateTime();
        EntityManager em = PersistenceManager.instance().createEntityManager();
        try {
            em.getTransaction().begin();

            Boolean isBooked = null;
            if ("Booked".equalsIgnoreCase(status)) {
                isBooked = true;
            } else if ("Unbooked".equalsIgnoreCase(status)) {
                isBooked = false;
            }

            List<Seat> seats;
            if (isBooked == null) {
                seats = em.createQuery("SELECT s FROM Seat s WHERE s.date = :date", Seat.class)
                        .setParameter("date", date)
                        .getResultList();
            } else {
                seats = em.createQuery("SELECT s FROM Seat s WHERE s.date = :date AND s.isBooked = :isBooked", Seat.class)
                        .setParameter("date", date)
                        .setParameter("isBooked", isBooked)
                        .getResultList();
            }

            if (seats.isEmpty()) {
                em.getTransaction().commit();
                return Response.ok(Collections.emptyList()).build();
            }

            Set<SeatDTO> dtoSeats = seats.stream().map(SeatMapper::toDTO).collect(Collectors.toSet());
            em.getTransaction().commit();

            GenericEntity<Set<SeatDTO>> out = new GenericEntity<Set<SeatDTO>>(dtoSeats) {};
            return Response.ok(out).build();

        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().commit();
            }
            em.close();
        }
    }
}
