package br.com.weg.workshop.post.domain; import java.io.Serializable; import java.util.UUID; public record PostLikeId(UUID post,UUID user) implements Serializable { }
