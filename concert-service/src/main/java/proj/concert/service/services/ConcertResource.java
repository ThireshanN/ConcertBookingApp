package proj.concert.service.services;

import org.apache.log4j.Priority;
import proj.concert.common.dto.*;
import proj.concert.service.domain.*;
import proj.concert.service.mapper.ConcertMapper;
import proj.concert.service.mapper.PerformerMapper;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

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
            String userName = creds.getUsername();
            String userPassword = creds.getPassword();
            TypedQuery<User> userQuery = em.createQuery("SELECT u FROM User u WHERE u.username = :username AND u.password = :password", User.class);
            userQuery.setParameter("username", userName);
            userQuery.setParameter("password", userPassword);
            List<User> users = userQuery.getResultList();

            userQuery = em.createQuery("SELECT u FROM User u", User.class);
            List<User> userList = userQuery.getResultList();

            for (User user : userList) {
                System.out.println(user.getId() + " " + user.getUsername() + " " + user.getPassword());
            }

            if (!users.isEmpty()) {
                String authValue = UUID.randomUUID().toString(); // Generate a random UUID as the auth value
                Response.ResponseBuilder builder = Response.ok().cookie(new NewCookie("auth", authValue));

                return builder.build();
            }

            Response.ResponseBuilder builder = Response.status(Response.Status.UNAUTHORIZED);
            System.out.println(builder);
            System.out.println(builder.build());
            return builder.build();
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
}
