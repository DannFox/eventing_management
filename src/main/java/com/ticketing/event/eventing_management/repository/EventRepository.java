package com.ticketing.event.eventing_management.repository;

import com.ticketing.event.eventing_management.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {
    Optional<Event> findByWpId(String wpId);
    Optional<Event> findByWooProductId(Long wooProductId);
}
