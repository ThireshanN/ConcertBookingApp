package proj.concert.service.services;

import proj.concert.common.dto.ConcertSummaryDTO;
import proj.concert.service.domain.Concert;
import proj.concert.service.mapper.ConcertMapper;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;


@Path("/concert-service")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ConcertResource {

    private EntityManager em = PersistenceManager.instance().createEntityManager();

    @GET
    @Path("/concert-summaries")
    public Response getConcertSummaries() {
        try {
            List<ConcertSummaryDTO> summaries = em.createQuery("SELECT c FROM Concert c", Concert.class)
                    .setLockMode(LockModeType.PESSIMISTIC_READ)
                    .getResultList().stream()
                    .map(ConcertMapper::toSummaryDTO)
                    .collect(Collectors.toList());

            if (summaries.isEmpty()) {
                return Response.noContent().build();
            }

            GenericEntity<List<ConcertSummaryDTO>> entity = new GenericEntity<List<ConcertSummaryDTO>>(summaries) {};
            return Response.ok(entity).build();
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }

}
