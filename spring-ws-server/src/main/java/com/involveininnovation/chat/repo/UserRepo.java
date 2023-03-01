package com.involveininnovation.chat.repo;

import com.involveininnovation.chat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepo extends JpaRepository<User, Long> {
    User findByUsername(String username);
}
