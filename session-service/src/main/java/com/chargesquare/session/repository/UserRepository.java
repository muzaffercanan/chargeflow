package com.chargesquare.session.repository;

import com.chargesquare.session.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}

