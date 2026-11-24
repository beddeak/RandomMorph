package kr.randommorph.fabric.mixin;
import java.util.List;
import kr.randommorph.fabric.ai.SpeciesAI;
import kr.randommorph.fabric.morph.MorphAccess;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.ai.sensing.PlayerSensor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=PlayerSensor.class,remap=false)
public abstract class PlayerSensorMixin {
    @Redirect(method="doTick",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;players()Ljava/util/List;"))
    private List<ServerPlayer> randommorph$humanCandidates(ServerLevel level){return SpeciesAI.enabled(level)?level.players().stream().filter(p->((MorphAccess)p).randommorph$form()==null).toList():level.players();}
}
