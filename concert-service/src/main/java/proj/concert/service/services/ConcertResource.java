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

    private static final Logger LOGGER = LogManager.getLogger(ConcertResource.class);

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

            GenericEntity<List<ConcertSummaryDTO>> entity = new GenericEntity<List<ConcertSummaryDTO>>(summaries) {
            };

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

            for (Performer performer : performers) {
                performerDTOs.add(PerformerMapper.toDTO(performer));
            }
            GenericEntity<List<PerformerDTO>> entity = new GenericEntity<List<PerformerDTO>>(performerDTOs) {
            };

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

    // This method is a POST request handler to handle user login attempts.
    // It takes a UserDTO object as input which contains user credentials(username and password).
    @POST
    @Path("/login")
    public Response login(UserDTO creds) {

        // Create an EntityManager instance to interact with the persistence context
        EntityManager em = PersistenceManager.instance().createEntityManager();

        try {
            // Start a new transaction
            em.getTransaction().begin();

            // Create a query to find the user by username and password
            TypedQuery<User> queryForUser = em.createQuery("SELECT u FROM User u where u.username = :username AND u.password = :password", User.class)
                    .setParameter("username", creds.getUsername())
                    .setParameter("password", creds.getPassword())
                    .setLockMode(LockModeType.PESSIMISTIC_READ);

            // Execute the query to find the user, and catch any exceptions that may occur
            User user;
            try {
                user = queryForUser.getSingleResult();
            } catch (NoResultException e) {
                // Rollback the transaction and return an UNAUTHORIZED response if the user is not found
                em.getTransaction().rollback();
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }

            // Generate a session ID for the user and set it on the User object
            user.setSessionId(UUID.randomUUID());

            // Lock the User object for optimistic concurrency control
            em.lock(user, LockModeType.OPTIMISTIC_FORCE_INCREMENT);

            // Commit the transaction
            em.getTransaction().commit();

            // Return an OK response with a new cookie containing the session ID
            return Response.ok().cookie(new NewCookie("auth", user.getSessionId().toString())).build();

        } finally {
            // Close the EntityManager instance
            em.close();
        }
    }

    @POST
    @Path("/bookings")
    public Response createNewBooking(BookingRequestDTO bookingDetails, @CookieParam("auth") Cookie authCookie) {
        // Create an EntityManager for handling database operations
        EntityManager em = PersistenceManager.instance().createEntityManager();
        User currentUser;
        // Check if the authentication cookie is present
        if (authCookie == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        // Begin transaction for retrieving the current user
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

        // Begin transaction for booking
        em.getTransaction().begin();
        try {
            // Find the target concert and check if the requested date is valid
            Concert targetConcert = em.find(Concert.class, bookingDetails.getConcertId(), LockModeType.PESSIMISTIC_READ);
            if (targetConcert == null || !targetConcert.getDates().contains(bookingDetails.getDate())) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }

            // Find all available requested seats
            List<Seat> openRequestedSeats = em.createQuery("SELECT s FROM Seat s WHERE s.label IN :seatLabels AND s.date = :eventDate AND s.isBooked = false", Seat.class)
                    .setParameter("seatLabels", bookingDetails.getSeatLabels())
                    .setParameter("eventDate", bookingDetails.getDate())
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    .getResultList();

            // If not all requested seats are available, rollback transaction
            if (openRequestedSeats.size() != bookingDetails.getSeatLabels().size()) {
                em.getTransaction().rollback();
                return Response.status(Response.Status.FORBIDDEN).build();
            }

            // Create a new reservation and associate it with the open seats
            Reservation newReservation = new Reservation(bookingDetails.getConcertId(), bookingDetails.getDate());
            newReservation.getSeats().addAll(openRequestedSeats);
            openRequestedSeats.forEach(seat -> seat.setIsBooked(true));
            currentUser.addReservation(newReservation);
            em.persist(newReservation);

            // Count the remaining unbooked seats
            int seatsRemaining = em.createQuery("SELECT COUNT(s) FROM Seat s WHERE s.date = :date AND s.isBooked = false", Long.class)
                    .setParameter("date", bookingDetails.getDate())
                    .getSingleResult()
                    .intValue();

            // Call the subscription notifier if the remaining seats meet the notification threshold
            subscriptionNotifier(bookingDetails.getDate(), seatsRemaining);

            // Commit the transaction and return the reservation
            em.getTransaction().commit();
            return Response.created(URI.create("/concert-service/bookings/" + newReservation.getId())).build();

        } catch (Exception e) {
            // If any exception occurs, rollback the transaction and return an error response
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            return Response.serverError().build();
        } finally {
            // Close the EntityManager
            em.close();
        }
    }



    private User authenticateUser(String authCookie) {
        // Check if the authentication cookie is present
        if (authCookie == null) {
            return null;
        }

        try {
            // Convert the authentication cookie value into a UUID
            UUID sessionId = UUID.fromString(authCookie);
            // Retrieve the user associated with the given session ID
            return getUserBySessionId(sessionId);
        } catch (IllegalArgumentException e) {
            // If the authentication cookie value is not a valid UUID, return null
            return null;
        }
    }


    public User getUserBySessionId(UUID sessionId) {
        // Create a query to find the user with the given session ID
        TypedQuery<User> query = em.createQuery("SELECT u FROM User u WHERE u.sessionId = :sessionId", User.class);
        query.setParameter("sessionId", sessionId);
        try {
            // Attempt to get a single result from the query
            return query.getSingleResult();
        } catch (NoResultException e) {
            // If no result is found, return null
            return null;
        }
    }

    public List<Reservation> getBookingsByUser(User user) {
        // Create a query to find all reservations associated with a user
        TypedQuery<Reservation> query = em.createQuery("SELECT b FROM Reservation b WHERE b.user = :user", Reservation.class);
        query.setParameter("user", user);
        // Return the list of reservations associated with the user
        return query.getResultList();
    }


    @Path("/bookings")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<BookingDTO> getBookings(@CookieParam("auth") String authCookie) {
        // Check if the authentication cookie is present
        if (authCookie == null) {
            throw new NotAuthorizedException("User not logged in");
        }
        // Authenticate the user using the authentication cookie
        User user = authenticateUser(authCookie);
        // If the user is not authenticated, throw a NotAuthorizedException
        if (user == null) {
            throw new NotAuthorizedException("Invalid authentication token");
        }

        // Retrieve the bookings associated with the authenticated user
        List<Reservation> bookings = getBookingsByUser(user);
        // Create a list to store the BookingDTO objects
        List<BookingDTO> bookingDTOs = new ArrayList<>();
        // Iterate over the bookings and convert them to BookingDTOs
        for (Reservation booking : bookings) {
            bookingDTOs.add(BookingMapper.toDTO(booking));
        }
        // Return the list of BookingDTOs
        return bookingDTOs;
    }

    @GET
    @Path("/bookings/{id}")
    public Response getBooking(@PathParam("id") Long id, @CookieParam("auth") Cookie authCookie) {
        // Start a new transaction
        try {
            em.getTransaction().begin();
            User user = null;
            // Check if the authentication cookie is present
            if (authCookie != null) {
                try {
                    // Retrieve the user associated with the authentication cookie
                    user = em.createQuery("SELECT u FROM User u where u.sessionId = :uuid", User.class)
                            .setParameter("uuid", UUID.fromString(authCookie.getValue()))
                            .setLockMode(LockModeType.OPTIMISTIC)
                            .getSingleResult();
                } catch (NoResultException e) {
                    // If the user is not found, return an Unauthorized response
                    return Response.status(Response.Status.UNAUTHORIZED).build();
                }
            }
            // If the user is not authenticated, return an Unauthorized response
            if (user == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }
            // Find the reservation with the given id
            Reservation booking = em.find(Reservation.class, id, LockModeType.PESSIMISTIC_READ);
            // Check if the reservation exists and belongs to the authenticated user
            if (booking == null || !booking.getUser().equals(user)) {
                // If not, return a Forbidden response
                return Response.status(Response.Status.FORBIDDEN).build();
            }
            // Convert the reservation to a BookingDTO
            BookingDTO dtoBooking = BookingMapper.toDTO(booking);
            // Return the BookingDTO in the response
            return Response.ok(dtoBooking).build();
        } finally {
            // Commit the transaction if it is active
            if (em.getTransaction().isActive()) {
                em.getTransaction().commit();
            }
            // Close the EntityManager
            em.close();
        }
    }

    @GET
    @Path("/seats/{date}")
    public Response getSeatsByDateAndStatus(@PathParam("date") String dateString, @DefaultValue("Any") @QueryParam("status") String status) {
        // Convert the input date string to a LocalDateTime object
        LocalDateTime date = new LocalDateTimeParam(dateString).getLocalDateTime();
        // Obtain an EntityManager from the PersistenceManager
        EntityManager em = PersistenceManager.instance().createEntityManager();
        try {
            // Begin a new transaction
            em.getTransaction().begin();

            // Initialize a variable to hold the booking status
            Boolean isBooked = null;
            // Check if the query parameter status is "Booked" or "Unbooked" and set the corresponding boolean value
            if ("Booked".equalsIgnoreCase(status)) {
                isBooked = true;
            } else if ("Unbooked".equalsIgnoreCase(status)) {
                isBooked = false;
            }

            // Declare a list to hold the Seat entities
            List<Seat> seats;
            // If the status is not specified, retrieve all seats for the given date
            if (isBooked == null) {
                seats = em.createQuery("SELECT s FROM Seat s WHERE s.date = :date", Seat.class)
                        .setParameter("date", date)
                        .getResultList();
            } else {
                // If the status is specified, retrieve seats for the given date and booking status
                seats = em.createQuery("SELECT s FROM Seat s WHERE s.date = :date AND s.isBooked = :isBooked", Seat.class)
                        .setParameter("date", date)
                        .setParameter("isBooked", isBooked)
                        .getResultList();
            }

            // If no seats are found, return an empty list
            if (seats.isEmpty()) {
                em.getTransaction().commit();
                return Response.ok(Collections.emptyList()).build();
            }

            // Convert the Seat entities to SeatDTO objects
            Set<SeatDTO> dtoSeats = seats.stream().map(SeatMapper::toDTO).collect(Collectors.toSet());
            // Commit the transaction
            em.getTransaction().commit();

            // Wrap the set of SeatDTO objects in a GenericEntity to handle generic type information
            GenericEntity<Set<SeatDTO>> out = new GenericEntity<Set<SeatDTO>>(dtoSeats) {
            };
            // Return the set of SeatDTO objects as a JSON response
            return Response.ok(out).build();

        } finally {
            // If the transaction is active, commit it
            if (em.getTransaction().isActive()) {
                em.getTransaction().commit();
            }
            // Close the EntityManager
            em.close();
        }
    }


    // ConcurrentHashMap to store concert subscriptions by date
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
                    // Retrieve the user based on the sessionId stored in the authCookie
                    user = em.createQuery("SELECT u FROM User u where u.sessionId = :uuid", User.class)
                            .setParameter("uuid", UUID.fromString(authCookie.getValue()))
                            .setLockMode(LockModeType.OPTIMISTIC)
                            .getSingleResult();
                } catch (NoResultException e) {
                    // If user not found, return UNAUTHORIZED status
                    sub.resume(Response.status(Status.UNAUTHORIZED).build());
                    return;
                }
            }
            // If user is not logged in, return UNAUTHORIZED status
            if (user == null) {
                sub.resume(Response.status(Status.UNAUTHORIZED).build());
                return;
            }

            // Check if concert exists
            Concert concert = em.find(Concert.class, request.getConcertId(), LockModeType.PESSIMISTIC_READ);
            // If concert not found or the requested date is not available, return BAD_REQUEST status
            if (concert == null || !concert.getDates().contains(request.getDate())) {
                sub.resume(Response.status(Status.BAD_REQUEST).build());
                return;
            }
        } finally {
            // Commit transaction and close the EntityManager
            if (em.getTransaction().isActive()) {
                em.getTransaction().commit();
            }
            em.close();
        }
        // Synchronized block to prevent race condition
        synchronized (subscriptions) {
            // If the requested date is not in the subscriptions map, add it with an empty LinkedList
            if (!subscriptions.contains(request.getDate())) {
                subscriptions.put(request.getDate(), new LinkedList<>());
            }
        }
        // Add the new subscription to the list for the requested date
        subscriptions.get(request.getDate()).add(new ConcertSubscription(sub, request.getPercentageBooked()));
    }

    // ExecutorService to handle subscription notifications in a separate thread
    private static final ExecutorService threadPool = Executors.newSingleThreadExecutor();

    private void subscriptionNotifier(LocalDateTime concertDateTime, int seatsRemaining) {
        // Execute the task in a separate thread
        threadPool.submit(() -> {
            // Calculate the percentage of booked seats
            double percentageBooked = 1.0 - (double) seatsRemaining / TheatreLayout.NUM_SEATS_IN_THEATRE;

            // Retrieve the list of subscriptions for the concertDateTime
            List<ConcertSubscription> concertSubscriptions = subscriptions.get(concertDateTime);
            // Iterate over the subscriptions
            for (Iterator<ConcertSubscription> iterator = concertSubscriptions.iterator(); iterator.hasNext(); ) {
                ConcertSubscription sub = iterator.next();

                // Check if the percentage of booked seats meets the notification threshold. Send notif if met.
                if (percentageBooked >= sub.percentageForNotif) {
                    // Remove the subscription from the list
                    iterator.remove();
                    // Send the notification with the remaining seats count
                    sub.response.resume(Response.ok(new ConcertInfoNotificationDTO(seatsRemaining)).build());
                }
            }
        });
    }

}