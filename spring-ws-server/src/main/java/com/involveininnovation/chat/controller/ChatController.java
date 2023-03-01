package com.involveininnovation.chat.controller;

import com.involveininnovation.chat.entity.Comment;
import com.involveininnovation.chat.entity.Post;
import com.involveininnovation.chat.entity.User;
import com.involveininnovation.chat.model.ResponseDTO;
import com.involveininnovation.chat.repo.CommentRepo;
import com.involveininnovation.chat.repo.PostRepo;
import com.involveininnovation.chat.repo.UserRepo;
import com.involveininnovation.chat.utils.JwtUtils;
import com.involveininnovation.chat.utils.SecurityConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.transaction.Transactional;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@Transactional
public class ChatController {
    @Autowired
    private PostRepo postRepo;
    @Autowired
    private CommentRepo commentRepo;
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    @MessageMapping("/send")
    public void comment(@Payload Comment comment, SimpMessageHeaderAccessor accessor) {
        try {
            if (accessor.getUser() != null) {
                Authentication authentication = (Authentication) accessor.getUser();
                SecurityContextHolder.getContext().setAuthentication(authentication);
                simpMessagingTemplate.convertAndSend("/post/" + comment.getPost().getId() + "/comments", commentRepo.save(comment));
            }
        } catch (Exception exception) {
            throw new RuntimeException(exception.getMessage());
        }
    }

    @MessageExceptionHandler(RuntimeException.class)
    public void handleException(RuntimeException exception, SimpMessageHeaderAccessor accessor) {
        log.error(exception.getMessage());
        String username = ((User) (((Authentication) accessor.getUser())).getPrincipal()).getUsername();
        ResponseDTO response = ResponseDTO.builder().message(exception.getMessage()).build();
        simpMessagingTemplate.convertAndSendToUser(username, "/error", response);
    }

    @GetMapping("/api/post/{postId}/comments")
    public List<Comment> getCommentOfPost(@PathVariable long postId) {
        List<Comment> comments = commentRepo.findByPostId(postId);
        return comments;
    }

    @GetMapping("/api/post")
    public List<Post> getAllPost() {
        return postRepo.findAll();
    }

    @PostMapping("/api/post")
    public Post savePost(@RequestBody Post post) {
        return postRepo.save(post);
    }

    @PostMapping("/api/post/{postId}/comments")
    public Comment commentPost(@RequestBody Comment comment, @PathVariable long postId) {
        Post post = postRepo.findById(postId).get();
        comment.setPost(post);
        return commentRepo.save(comment);
    }

    @PostMapping("/api/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User userCreated = userRepo.save(user);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest().build().toUri();

        Map<String, String> claims = new HashMap<>();
        claims.put("type", "access_token");
        String accessToken = JwtUtils.generateToken(userCreated.getUsername(), SecurityConstants.ACCESS_TOKEN_LIFE_TIME, location.toString(), claims);

        Map<String, Object> data = new HashMap<>();
        data.put("access_token", accessToken);
        data.put("user", userCreated);

        ResponseDTO<?> response = ResponseDTO.builder().code(HttpStatus.CREATED.value()).data(data).build();

        return ResponseEntity.created(location).body(response);
    }
}
