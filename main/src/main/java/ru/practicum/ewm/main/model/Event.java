package ru.practicum.ewm.main.model;

import jakarta.persistence.*;
import lombok.*;
import ru.practicum.ewm.main.enums.EventsState;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "events")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Builder
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 120)
    private String title;
    @Column(nullable = false, length = 2000)
    private String annotation;
    @Column(nullable = false, length = 7000)
    private String description;

    @Column(name = "created_on", nullable = false, updatable = false)
    private LocalDateTime createdOn;
    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;
    @Column(name = "published_on")
    private LocalDateTime publishedOn;

    @ManyToOne
    @JoinColumn(name = "initiator_id", nullable = false, updatable = false)
    private User initiator;

    @Column(nullable = false)
    @Embedded
    private Location location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventsState state;

    @Column(nullable = false)
    private boolean paid;
    @Column(name = "request_moderation", nullable = false)
    private boolean requestModeration = true;
    @Column(name = "allow_comments", nullable = false)
    private boolean allowComments;

    @Column(name = "participant_limit", nullable = false)
    private long participantLimit;
    @Column(name = "confirmed_requests", nullable = false)
    private long confirmedRequests = 0;
    @Transient
    private long views;

    @OneToMany(mappedBy = "event", fetch = FetchType.EAGER)
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();
}
