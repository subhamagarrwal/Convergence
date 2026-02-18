package com.subham.convergence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.subham.convergence.model.User;
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);


    @Query("SELECT u FROM User u WHERE u.username = :username AND u.password = :password")
    Optional<User> findByUsernameAndPassword(@Param("username") String username, @Param("password") String password);


    @Query("SELECT u FROM User u WHERE u.isVerified = true")
    List<User> findAllVerifiedUsers();

    //find all users , using pagination
    Page <User> findAll(Pageable pageable);
    //find all registered users
    @Query("SELECT u FROM User u WHERE u.isVerified = true")
    Page <User> findByStatus(Pageable pageable);
    


}
