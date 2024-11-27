package polytech.service_reservations.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import polytech.service_reservations.dto.ReservationEvent;
import polytech.service_reservations.kafka.KafkaProducer;
import polytech.service_reservations.model.Reservation;
import polytech.service_reservations.service.ReservationService;

@RestController
@RequestMapping("/")
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private KafkaProducer kafkaProducer;

    @GetMapping
    public List<Reservation> getAllReservations() {
        System.out.println("Fetching all reservations...");
        return reservationService.getAllReservations();
    }

    @GetMapping("/test")
        public String testEndpoint() {
    return "Test OK";
    }

    @GetMapping("/{id}")
    public Optional<Reservation> getReservationById(@PathVariable Long id) {
        return reservationService.getReservationById(id);
    }

    @GetMapping("/user/{id}")
    public List<Reservation> getReservationByIdUser(@PathVariable Long id) {
        return reservationService.getReservationByIdUser(id);
    }

    @PostMapping
    public Reservation createReservation(@RequestBody Reservation reservation) {
        Reservation savedReservation = reservationService.createReservation(reservation);

        if (savedReservation != null && savedReservation.getIdReservation() != null) {
            // Construire l'événement pour Kafka
            ReservationEvent event = new ReservationEvent(
                    savedReservation.getIdVoyageur(),
                    savedReservation.getIdEmplacement(),
                    savedReservation.getDateArrive(),
                    savedReservation.getDateDepart(),
                    false,
                    false
            );
            kafkaProducer.sendEvaluationEvent(event);
            System.out.println("Notification envoyée pour la réservation ID : " + savedReservation.getIdReservation());
        } else {
            ReservationEvent event = new ReservationEvent(
                    reservation.getIdVoyageur(),
                    reservation.getIdEmplacement(),
                    reservation.getDateArrive(),
                    reservation.getDateDepart(),
                    true,
                    false
            );
            kafkaProducer.sendEvaluationEvent(event);
            System.err.println("La réservation n'a pas pu être créée. Aucun événement envoyé.");
        }

        return savedReservation;
    }

    @GetMapping("/emplacement/{id}")
    public List<Reservation> getReservationByIdEmplacement(@PathVariable Long id) {
        return reservationService.getReservationByIdEmplacement(id);
    }

    @DeleteMapping("/{id}")
    public void deleteReservation(@PathVariable Long id) {
        Reservation reservation = reservationService.getReservationById(id).orElse(null);
        if (reservation == null) {
            System.err.println("La réservation n'existe pas.");
            return;
        }
        reservationService.deleteReservation(id);

        ReservationEvent event = new ReservationEvent(
                reservation.getIdVoyageur(),
                reservation.getIdEmplacement(),
                reservation.getDateArrive(),
                reservation.getDateDepart(),
                false,
                true
        );
        kafkaProducer.sendEvaluationEvent(event);
    }
}
