package com.counselx.auth.repository;

import com.counselx.auth.entity.roles;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<roles, Long> {

    Optional<roles> findByName(String name);
}