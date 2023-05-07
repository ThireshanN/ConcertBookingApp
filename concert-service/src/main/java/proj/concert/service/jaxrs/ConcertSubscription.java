package proj.concert.service.jaxrs;
import javax.ws.rs.container.AsyncResponse;


public class ConcertSubscription {
    public final AsyncResponse response;
    public final double percentageForNotif;

    public ConcertSubscription(AsyncResponse response, int percentageBooked) {
        this.response = response;
        this.percentageForNotif = percentageBooked / 100.0;
    }
}
