package br.com.weg.workshop.administration.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record BulkAttendanceRequest(
        @NotEmpty @Size(max = 500) List<@Valid AttendanceUpdateRequest> updates
) {
}
