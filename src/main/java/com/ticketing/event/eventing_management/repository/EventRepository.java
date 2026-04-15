package com.ticketing.event.eventing_management.repository;

import com.ticketing.event.eventing_management.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    Optional<Event> findByWpId(String wpId);
    Optional<Event> findByWooProductId(Long wooProductId);
    Page<Event> findByActiveTrueAndEventDateAfter(LocalDateTime now, Pageable pageable);
    Page<Event> findByActiveTrueAndEventDateAfterAndCategory(LocalDateTime now, String category, Pageable pageable);
}
