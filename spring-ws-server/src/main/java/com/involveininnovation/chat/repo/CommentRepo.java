package com.involveininnovation.chat.repo;

import com.involveininnovation.chat.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepo extends JpaRepository<Comment, Long> {
    List<Comment> findByPostId(Long postId);
}
