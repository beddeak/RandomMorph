package kr.randommorph.fabric.core;

import java.util.function.Consumer;

/** One broken resource must not prevent the remaining resources from being released. */
public final class Cleanup {
    private final Consumer<Throwable> failure;
    public Cleanup(Consumer<Throwable> failure) { this.failure = failure; }
    public void attempt(Runnable action) {
        try { action.run(); }
        catch (RuntimeException | LinkageError error) { failure.accept(error); }
    }
}
