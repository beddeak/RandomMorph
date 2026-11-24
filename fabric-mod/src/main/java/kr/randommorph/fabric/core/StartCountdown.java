package kr.randommorph.fabric.core;
import java.util.*;
/** One server-owned countdown; UUIDs avoid keeping stale Player instances. */
public final class StartCountdown {
    private final Set<UUID> players=new LinkedHashSet<>();
    private int ticks;
    public boolean begin(Collection<UUID> targets){
        if(active()||targets.isEmpty())return false;
        players.addAll(targets);ticks=60;return true;
    }
    public boolean active(){return !players.isEmpty()&&ticks>0;}
    public int seconds(){return (ticks+19)/20;}
    public Set<UUID> players(){return Set.copyOf(players);}
    public void cancel(UUID player){players.remove(player);if(players.isEmpty())ticks=0;}
    public void clear(){players.clear();ticks=0;}
    public List<UUID> tick(){
        if(!active())return List.of();
        if(--ticks>0)return List.of();
        var ready=List.copyOf(players);clear();return ready;
    }
}
