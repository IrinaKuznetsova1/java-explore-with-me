package ru.practicum.ewm.main.model;

import jakarta.persistence.*;
import lombok.*;
import ru.practicum.ewm.main.enums.RequestStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "requests")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Request {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false, updatable = false)
    private LocalDateTime created;

    @ManyToOne
    @JoinColumn(name = "event_id", nullable = false, updatable = false)
    private Event event;

    @ManyToOne
    @JoinColumn(name = "requester_id", nullable = false, updatable = false)
    private User requester;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestStatus status;
}
