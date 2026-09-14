package com.colonyrank.mod.data;

public record ScoreBreakdown(
    double populationComponent,
    double happinessComponent,
    double buildingComponent,
    double levelComponent,
    double claimsComponent,
    double total,
    double finalScale
) {
}
