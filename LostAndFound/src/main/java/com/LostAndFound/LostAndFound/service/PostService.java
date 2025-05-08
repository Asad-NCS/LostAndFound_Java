package com.LostAndFound.LostAndFound.service;

import com.LostAndFound.LostAndFound.dto.PostDTO;
import com.LostAndFound.LostAndFound.model.Post;
import com.LostAndFound.LostAndFound.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;

    public PostDTO createPost(PostDTO postDTO) {
        Post post = new Post();
        post.setTitle(postDTO.getTitle());
        post.setDescription(postDTO.getDescription());
        return toDTO(postRepository.save(post));
    }

    public PostDTO getPostById(Long id) {
        return postRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("Post not found"));
    }

    public List<PostDTO> getAllPosts() {
        return postRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    public PostDTO updatePost(Long id, PostDTO postDTO) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        post.setTitle(postDTO.getTitle());
        post.setDescription(postDTO.getDescription());
        return toDTO(postRepository.save(post));
    }

    public void deletePost(Long id) {
        postRepository.deleteById(id);
    }

    private PostDTO toDTO(Post post) {
        PostDTO dto = new PostDTO();
        dto.setId(post.getId());
        dto.setTitle(post.getTitle());
        dto.setDescription(post.getDescription());
        return dto;
    }

}
