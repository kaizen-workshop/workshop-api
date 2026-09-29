package br.com.weg.workshop.notification.dto;

import br.com.weg.workshop.notification.domain.DevicePlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterDeviceRequest(@NotBlank @Size(max = 500) String token,
                                    @NotNull DevicePlatform platform) { }
