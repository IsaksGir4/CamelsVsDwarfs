package com.spring.camelsvsdwarfs.repository.specification;

import com.spring.camelsvsdwarfs.entity.Race;
import com.spring.camelsvsdwarfs.entity.RaceStatus;
import com.spring.camelsvsdwarfs.entity.RaceType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public final class RaceSpecification {

    private RaceSpecification() {
    }

    public static Specification<Race> hasStatus(RaceStatus status) {
        return (root, query, cb) ->
                status == null ? null : cb.equal(root.get("raceStatus"), status);
    }

    public static Specification<Race> hasType(RaceType type) {
        return (root, query, cb) ->
                type == null ? null : cb.equal(root.get("raceType"), type);
    }

    public static Specification<Race> nameContains(String name) {
        return (root, query, cb) ->
                name == null || name.isBlank() ? null :
                        cb.like(cb.lower(root.get("raceName")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Race> scheduledAfter(LocalDate date) {
        return (root, query, cb) ->
                date == null ? null : cb.greaterThanOrEqualTo(root.get("programationDate"), date);
    }
}