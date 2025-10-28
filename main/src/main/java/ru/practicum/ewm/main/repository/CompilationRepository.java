package ru.practicum.ewm.main.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.ewm.main.model.Compilation;

public interface CompilationRepository extends JpaRepository<Compilation, Long> {

    @Query("""
            SELECT COUNT(c) > 0
            FROM Compilation c
            WHERE LOWER(TRIM(c.title)) = LOWER(TRIM(:title))
            """)
    boolean existsCompilationByTitle(String title);
}
