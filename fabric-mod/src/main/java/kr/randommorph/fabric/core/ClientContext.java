package kr.randommorph.fabric.core;

/** Entity IDs and transient input belong to a particular world and player instance. */
public final class ClientContext {
    private Object level, player;
    public boolean update(Object nextLevel, Object nextPlayer) {
        boolean changed = level != nextLevel || player != nextPlayer;
        level = nextLevel;
        player = nextPlayer;
        return changed;
    }
    public void reset() { level = null; player = null; }
}
