package com.shahabhameed.democomplaints;

import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.TargetAggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

@SpringBootApplication
public class DemoComplaintsApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoComplaintsApplication.class, args);
	}

	@RestController
	public static class ComplaintAPI {

		private final ComplaintQueryObjectRepository repository;
		private final CommandGateway commandGateway;

		public ComplaintAPI(ComplaintQueryObjectRepository repository, CommandGateway commandGateway) {
			this.repository = repository;
			this.commandGateway = commandGateway;
		}

		@PostMapping
		public CompletableFuture<String> fileComplaint(@RequestBody Map<String, String> request) {
			String id = UUID.randomUUID().toString();
			return  commandGateway.send(new FileComplaintCommand(id, request.get("company"), request.get("description")));
		}

		@GetMapping
		public List<ComplaintQueryObject> findAll() {
			return repository.findAll();
		}

		@GetMapping("/{id}")
		public Optional<ComplaintQueryObject> find(@PathVariable String id) {
			return repository.findById(id);
		}
	}

	@Aggregate
	public static class Complaint {

		@TargetAggregateIdentifier
		private String complaintId;

		@CommandHandler
		public Complaint(FileComplaintCommand cmd) {
			Assert.hasLength(cmd.getCompany(), "Company cannot be null");
			apply(new ComplaintFiledEvent(cmd.getId(), cmd.getCompany(), cmd.getDescription()));
		}

		public Complaint() {
		}

		@EventSourcingHandler
		public void on(ComplaintFiledEvent event) {
			this.complaintId = event.getId();
		}
	}

	@Component
	public static class ComplaintQueryObjectUpdater {

		private final ComplaintQueryObjectRepository repository;

		public ComplaintQueryObjectUpdater(ComplaintQueryObjectRepository repository) {
			this.repository = repository;
		}

		@EventHandler
		public void on(ComplaintFiledEvent event) {
			repository.save(new ComplaintQueryObject(event.getId(), event.getCompany(), event.getDescription()));
		}
	}

	public static class FileComplaintCommand {

		@TargetAggregateIdentifier
		private final String id;

		public String getId() {
			return id;
		}

		public String getCompany() {
			return company;
		}

		public String getDescription() {
			return description;
		}

		private final String company;
		private final String description;

		public FileComplaintCommand(String id, String company, String description) {
			this.id = id;
			this.company = company;
			this.description = description;
		}
	}
}
