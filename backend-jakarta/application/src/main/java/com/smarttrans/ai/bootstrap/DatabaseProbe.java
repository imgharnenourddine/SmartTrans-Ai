package com.smarttrans.ai.bootstrap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class DatabaseProbe {

    @PersistenceContext(unitName = "smarttransPU")
    private EntityManager entityManager;

    @Transactional
    public long countRoles() {
        return entityManager
                .createQuery("select count(role) from Role role", Long.class)
                .getSingleResult();
    }
}
