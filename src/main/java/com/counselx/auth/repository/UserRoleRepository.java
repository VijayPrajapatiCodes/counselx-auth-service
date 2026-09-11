package com.counselx.auth.repository;

import com.counselx.auth.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    Optional<UserRole> findByUser_Id(Long userId);

    List<UserRole> findAllByUser_Id(Long userId);
}
