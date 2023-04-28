package proj.concert.service.domain;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public class Seat{

	@Id
	private String label;
	private boolean isBooked;
	@Id
	private LocalDateTime date;
	private BigDecimal cost;


	public Seat(String label, boolean isBooked, LocalDateTime date, BigDecimal cost) {
		this.label = label;
		this.isBooked = isBooked;
		this.date = date;
		this.cost = cost;
	}	
	
	public Seat() {}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public Boolean getIsBooked() {
		return isBooked;
	}

	public void setIsBooked(Boolean isBooked) {
		this.isBooked = isBooked;
	}

	public LocalDateTime getDate() {
		return date;
	}

	public void setDate(LocalDateTime date) {
		this.date = date;
	}

	public BigDecimal getPrice() {
		return cost;
	}

	public void setPrice(BigDecimal price) {
		this.cost = price;
	}

}
