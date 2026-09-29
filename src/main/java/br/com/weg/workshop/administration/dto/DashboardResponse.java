package br.com.weg.workshop.administration.dto;

public record DashboardResponse(
        long workshops,
        long publishedWorkshops,
        long registrations,
        long confirmedRegistrations,
        long waitingListRegistrations,
        long attendedRegistrations
) {
}
