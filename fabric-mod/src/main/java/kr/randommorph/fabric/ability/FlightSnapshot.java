package kr.randommorph.fabric.ability;

/** Flight before the form took ownership; creative and spectator have different defaults. */
record FlightSnapshot(boolean mayfly, boolean flying, float speed, boolean creative, boolean spectator) {
    FlightSnapshot restored(boolean currentCreative, boolean currentSpectator) {
        if (creative == currentCreative && spectator == currentSpectator) return this;
        return new FlightSnapshot(currentCreative || currentSpectator, currentSpectator, .05f,
                currentCreative, currentSpectator);
    }
}
