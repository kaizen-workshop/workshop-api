package br.com.weg.workshop.post.dto; import jakarta.validation.constraints.*; public record CreateCommentRequest(@NotBlank @Size(max=4000) String content) { }
