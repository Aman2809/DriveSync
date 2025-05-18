package com.project.cloudsync.repositories;


import com.project.cloudsync.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {
}
