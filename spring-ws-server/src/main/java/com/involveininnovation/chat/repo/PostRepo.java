package com.involveininnovation.chat.repo;

import com.involveininnovation.chat.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepo extends JpaRepository<Post, Long> {
}
