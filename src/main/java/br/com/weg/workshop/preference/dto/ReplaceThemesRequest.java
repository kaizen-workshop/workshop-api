package br.com.weg.workshop.preference.dto; import jakarta.validation.constraints.NotNull; import java.util.*; public record ReplaceThemesRequest(@NotNull Set<UUID> themeIds){}
