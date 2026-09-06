package com.spring.camelsvsdwarfs.service;

import com.spring.camelsvsdwarfs.entity.Player;
import com.spring.camelsvsdwarfs.entity.PlayerState;
import com.spring.camelsvsdwarfs.entity.PlayerType;
import org.springframework.data.jpa.domain.Specification;

public final class PlayerSpecification {

    private PlayerSpecification() {
        // Evita instanciacion: clase utilitaria de solo metodos estaticos
    }

    public static Specification<Player> hasType(PlayerType playerType){
        return (root, query, cb) -> playerType ==null?null:cb.equal(root.get("playerType"), playerType);
    }

    public static Specification<Player> hasState(PlayerState actualState){
        return (root, query, cb) -> actualState==null?null : cb.equal(root.get("actualState"), actualState);
    }

    public static Specification<Player> nameContains(String name) {
        return (root, query, cb) -> name==null || name.isBlank() ? null : cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }
}
