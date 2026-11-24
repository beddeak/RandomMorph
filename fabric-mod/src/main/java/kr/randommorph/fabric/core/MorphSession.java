package kr.randommorph.fabric.core;

/** Per-player clock, independent of entity replacement on death/respawn. */
public final class MorphSession {
    private String form;
    private int intervalTicks;
    private int remainingTicks;
    private boolean paused;
    private int cubeSize=2;
    public MorphSession(String form, int seconds) {
        if (form == null || form.isBlank() || seconds < 5 || seconds > 3600) throw new IllegalArgumentException("Invalid session");
        this.form = form; this.intervalTicks = seconds * 20; remainingTicks = intervalTicks;
    }
    public boolean tick() {
        if (paused) return false;
        if (--remainingTicks > 0) return false;
        remainingTicks = intervalTicks;
        return true;
    }
    public void setForm(String form, boolean restart) {
        if (form == null || form.isBlank()) throw new IllegalArgumentException("Missing form");
        this.form = form; cubeSize=2;
        if (restart) remainingTicks = intervalTicks;
    }
    public int intervalSeconds(){return intervalTicks/20;}
    public void interval(int seconds){
        if(seconds<5||seconds>3600)throw new IllegalArgumentException("Interval: 5..3600 seconds");
        int elapsed=intervalTicks-remainingTicks;intervalTicks=seconds*20;remainingTicks=Math.max(1,intervalTicks-elapsed);
    }
    /** A blocked roll retries soon, without changing the configured normal interval. */
    public void retryAfter(int seconds){
        if(seconds<1||seconds>3600)throw new IllegalArgumentException("Retry: 1..3600 seconds");
        remainingTicks=Math.min(remainingTicks,seconds*20);
    }
    /** Current body variant survives avatar replacement, but resets on the next roll. */
    public int cubeSize(){return cubeSize;}
    public void cubeSize(int size){MorphFlags.withCubeSize(0,size);cubeSize=size;}
    public String form() { return form; }
    public int remainingTicks() { return remainingTicks; }
    public int remainingSeconds() { return (remainingTicks + 19) / 20; }
    public boolean paused() { return paused; }
    public void paused(boolean value) { paused = value; }
    public static float healthRatio(float health, float max) {
        if (!Float.isFinite(health) || !Float.isFinite(max) || max <= 0) return 0;
        return Math.clamp(health / max, 0, 1);
    }
}
