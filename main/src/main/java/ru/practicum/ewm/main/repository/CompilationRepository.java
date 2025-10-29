package ru.practicum.ewm.main.repository;

import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.ewm.main.model.Compilation;

import java.util.List;

public interface CompilationRepository extends JpaRepository<Compilation, Long> {
    @Query("""
            SELECT COUNT(c) > 0
            FROM Compilation c
            WHERE LOWER(TRIM(c.title)) = LOWER(TRIM(:title))
            """)
    boolean existsCompilationByTitle(String title);

    @EntityGraph(attributePaths = {"events", "events.category", "events.initiator"})
    List<Compilation> findByPinned(boolean pinned, Pageable pageable);

    @Override
    @NonNull
    @EntityGraph(attributePaths = {"events", "events.category", "events.initiator"})
    Page<Compilation> findAll(@NonNull Pageable pageable);
}
