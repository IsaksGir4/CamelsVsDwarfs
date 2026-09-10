package com.spring.camelsvsdwarfs.repository.specification;

import com.spring.camelsvsdwarfs.entity.Team;
import com.spring.camelsvsdwarfs.entity.TeamStatus;
import org.springframework.data.jpa.domain.Specification;

public final class TeamSpecification {

    private TeamSpecification(){

    }

    public static Specification<Team> hasStatus(TeamStatus status){
        return (root,query, cb) ->
                status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Team> nameContains(String name) {
        return (root, query, cb) ->
                name ==null || name.isBlank() ? null :
                        cb.like(cb.lower(root.get("teamName")), "%" + name.toLowerCase() + "%");
    }
}
