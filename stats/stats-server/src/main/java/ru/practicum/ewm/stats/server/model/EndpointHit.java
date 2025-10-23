package ru.practicum.ewm.stats.server.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "endpoint_hits")
@AllArgsConstructor
public class EndpointHit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(nullable = false, length = 32)
    private String app;
    @Column(nullable = false, length = 128)
    private String uri;
    @Column(nullable = false, length = 16)
    private String ip;
    @Column(nullable = false)
    private LocalDateTime timestamp;
}
