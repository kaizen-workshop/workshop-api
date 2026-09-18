package br.com.weg.workshop.user.dto; import jakarta.validation.constraints.NotBlank; public record UpdateProfileRequest(@NotBlank String name,String phone,String profileImage) {}
