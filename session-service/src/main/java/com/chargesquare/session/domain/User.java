package com.chargesquare.session.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users", schema = "session")
public class User {

    @Id
    private Long id;

    protected User() {
    }

    public Long getId() {
        return id;
    }
}

