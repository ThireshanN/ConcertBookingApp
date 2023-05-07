package proj.concert.service.services;

import org.apache.log4j.Priority;
import proj.concert.common.dto.*;
import proj.concert.common.dto.ConcertInfoSubscriptionDTO;
import proj.concert.service.domain.*;
import proj.concert.service.jaxrs.LocalDateTimeParam;
import proj.concert.service.mapper.BookingMapper;
import proj.concert.service.mapper.ConcertMapper;
import proj.concert.service.mapper.PerformerMapper;
import proj.concert.service.jaxrs.ConcertSubscription;
import proj.concert.common.dto.ConcertInfoNotificationDTO;
import proj.concert.common.dto.ConcertInfoSubscriptionDTO;
import proj.concert.service.util.TheatreLayout;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.net.URI;

import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.LinkedList;

import java.time.LocalDateTime;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import proj.concert.service.mapper.SeatMapper;

@Path("/concert-service")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ConcertResource {

    //static logger for logging events and messages
    private static final Logger LOGGER = LogManager.getLogger(ConcertResource.class);

    @GET
    @Path("/concerts")
    public Response getAllConcerts() {
        EntityManager em = PersistenceManager.instance().createEntityManager();
        try {
            //starting transaction for databese queries
            em.getTransaction().begin();
            List<Concert> concerts = em.createQuery("SELECT c FROM Concert c", Concert.class)
                    .getResultList();
            // if there are no concerts found then return a Response with a noContent status
            if (concerts.isEmpty()) {
                return Response.noContent().build();
            }

            List<ConcertDTO> concertDTOS = new ArrayList<>();
            for (Concert concert : concerts) {
                concertDTOS.add(ConcertMapper.toDTO(concert));
            }

            // Commit the transaction and create a GenericEntity with the concertDTOS list as its entity body
            em.getTransaction().commit();
            GenericEntity<List<ConcertDTO>> out = new GenericEntity<List<ConcertDTO>>(concertDTOS) {
            };
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
            //if no concert is found, return a Not_found response
            if (foundConcert == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            // Convert the Concert object to a ConcertDTO object and return it as the Response entity
            ConcertDTO concertDTO = ConcertMapper.toDTO(foundConcert);
            return Response.ok(concertDTO).build();
        } catch (Exception e) { //block to handle exceptions, rolling back the transaction in case of an error.
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            // Return a Response with an INTERNAL_SERVER_ERROR status and no entity body
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

            // Create a GenericEntity containing the list of ConcertSummaryDTOs and return a 200 OK response
            GenericEntity<List<ConcertSummaryDTO>> entity = new GenericEntity<List<ConcertSummaryDTO>>(summaries) {
            };

            em.getTransaction().commit(); // Commit the transaction
            return Response.ok(entity).build();
        } catch (Exception e) {
            // Log the exception and return an appropriate error response
            LOGGER.log(Priority.ERROR, "An error occurred while processing the request.", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("An error occurred while processing the request.").build();
        } finally {
            // Rollback the transaction if it is still active and close the EntityManager
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
            // Query the Performer entities and retrieve the results as a list
            List<Performer> performers = entityManager.createQuery("SELECT p FROM Performer p", Performer.class).getResultList();
            // Commit the transaction
            entityManager.getTransaction().commit();
            // If the list of performers is empty, return a No Content response
            if (performers.isEmpty()) {
                return Response.noContent().build();
            }

            // Convert the list of Performer entities to a list of PerformerDTOs
            List<PerformerDTO> performerDTOs = new ArrayList<>();

            for (Performer performer : performers) {
                performerDTOs.add(PerformerMapper.toDTO(performer));
            }
            GenericEntity<List<PerformerDTO>> entity = new GenericEntity<List<PerformerDTO>>(performerDTOs) {
            };

            return Response.ok(entity).build();
        } catch (Exception e) {
            if (entityManager.getTransaction().isActive()) {
                // If an exception occurs, roll back the transaction
                entityManager.getTransaction().rollback();
            }
            //return a Internal Server Error response
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

            // If the Performer was not found, return a NOT_FOUND response
            if (foundPerformer == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            // Map the Performer to a DTO and return it in the response body
            PerformerDTO performerDTO = PerformerMapper.toDTO(foundPerformer);
            return Response.ok(performerDTO).build();
        } catch (Exception e) {
            // If an error occurs, log the exception return an INTERNAL_SERVER_ERROR response
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            //return an INTERNAL_SERVER_ERROR response
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

            //Reservation made, check if remaining seats meet notification threshold
            int seatsRemaining = em.createQuery("SELECT COUNT(s) FROM Seat s WHERE s.date = :date AND s.isBooked = false", Long.class)
                    .setParameter("date", bookingDetails.getDate())
                    .getSingleResult()
                    .intValue();
            subscriptionNotifier(bookingDetails.getDate(), seatsRemaining);


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

            GenericEntity<Set<SeatDTO>> out = new GenericEntity<Set<SeatDTO>>(dtoSeats) {
            };
            return Response.ok(out).build();

        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().commit();
            }
            em.close();
        }
    }

    private static final ConcurrentHashMap<LocalDateTime, LinkedList<ConcertSubscription>> subscriptions = new ConcurrentHashMap<>();
    @POST
    @Path("/subscribe/concertInfo")
    public void subscribeToConcert(@Suspended AsyncResponse sub, @CookieParam("auth") Cookie authCookie, ConcertInfoSubscriptionDTO request) {
        EntityManager em = PersistenceManager.instance().createEntityManager();
        // Check if user is signed in
        try {
            User user = null;
            if (authCookie != null) {
                em.getTransaction().begin();
                try {
                    user = em.createQuery("SELECT u FROM User u where u.sessionId = :uuid", User.class)
                            .setParameter("uuid", UUID.fromString(authCookie.getValue()))
                            .setLockMode(LockModeType.OPTIMISTIC)
                            .getSingleResult();
                } catch (NoResultException e) {
                    sub.resume(Response.status(Status.UNAUTHORIZED).build());
                    return;
                }
            }
            if (user == null) {
                sub.resume(Response.status(Status.UNAUTHORIZED).build());
                return;
            }

            // Check if concert exists
            Concert concert = em.find(Concert.class, request.getConcertId(), LockModeType.PESSIMISTIC_READ);
            if (concert == null || !concert.getDates().contains(request.getDate())) {
                sub.resume(Response.status(Status.BAD_REQUEST).build());
                return;
            }
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().commit();
            }
            em.close();
        }
        // synchronized block to prevent race condition
        synchronized (subscriptions) {
            if (!subscriptions.contains(request.getDate())) {
                subscriptions.put(request.getDate(), new LinkedList<>());
            }
        }
        subscriptions.get(request.getDate()).add(new ConcertSubscription(sub, request.getPercentageBooked()));
    }

    private static final ExecutorService threadPool = Executors.newSingleThreadExecutor();
    private void subscriptionNotifier(LocalDateTime concertDateTime, int seatsRemaining) {
        // Execute the task in a separate thread
        threadPool.submit(() -> {
            // Calculate the percentage of booked seats
            double percentageBooked = 1.0 - (double) seatsRemaining / TheatreLayout.NUM_SEATS_IN_THEATRE;

            List<ConcertSubscription> concertSubscriptions = subscriptions.get(concertDateTime);
            for (Iterator<ConcertSubscription> iterator = concertSubscriptions.iterator(); iterator.hasNext(); ) {
                ConcertSubscription sub = iterator.next();

                // Check if the percentage of booked seats meets the notification threshold. Send notif if met.
                if (percentageBooked >= sub.percentageForNotif) {
                    iterator.remove();
                    sub.response.resume(Response.ok(new ConcertInfoNotificationDTO(seatsRemaining)).build());
                }
            }
        });
    }
}