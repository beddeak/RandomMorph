package kr.randommorph.fabric;
import java.util.*;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import kr.randommorph.fabric.morph.*;
import kr.randommorph.fabric.ability.Active;
public final class MorphCommands {
    private MorphCommands(){}
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        var root=Commands.literal("morph").requires(s->s.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER));
        for(String action:new String[]{"start","stop","next","reroll","pause","resume","status"})
            root.then(Commands.literal(action).executes(c->run(c,action,action.equals("start")?c.getSource().getServer().getPlayerList().getPlayers():List.of(c.getSource().getPlayerOrException())))
                .then(Commands.argument("targets",EntityArgument.players()).executes(c->run(c,action,EntityArgument.getPlayers(c,"targets")))));
        root.then(Commands.literal("set").then(Commands.argument("mob",StringArgumentType.word())
            .suggests((c,b)->SharedSuggestionProvider.suggest(MorphRegistry.profiles().stream().map(MorphProfile::id),b))
            .executes(c->set(c,List.of(c.getSource().getPlayerOrException())))
            .then(Commands.argument("targets",EntityArgument.players()).executes(c->set(c,EntityArgument.getPlayers(c,"targets"))))));
        var size=Commands.literal("size");
        for(int value:new int[]{1,2,4})size.then(Commands.literal(Integer.toString(value))
            .executes(c->size(c,value,List.of(c.getSource().getPlayerOrException())))
            .then(Commands.argument("targets",EntityArgument.players()).executes(c->size(c,value,EntityArgument.getPlayers(c,"targets")))));
        root.then(size);
        root.then(Commands.literal("interval").then(Commands.argument("seconds",IntegerArgumentType.integer(5,3600))
            .executes(c->{boolean ok=manager(c).interval(IntegerArgumentType.getInteger(c,"seconds"));reply(c,ok?"전체 주기와 기본 설정을 변경했습니다. 이미 지난 시간은 유지합니다.":"설정 저장 실패: 변경하지 않았습니다.");return ok?1:0;})
            .then(Commands.argument("targets",EntityArgument.players()).executes(c->{int n=0;for(var p:EntityArgument.getPlayers(c,"targets"))if(manager(c).interval(p,IntegerArgumentType.getInteger(c,"seconds")))n++;reply(c,"개별 주기 변경 "+n+"명 (현재 참가 세션)");return n;}))));
        root.then(Commands.literal("instincts").then(Commands.argument("enabled",BoolArgumentType.bool()).executes(c->{boolean enabled=BoolArgumentType.getBool(c,"enabled"),ok=manager(c).instincts(enabled);reply(c,ok?"플레이어 자동 추격/회피 본능: "+enabled:"설정 저장 실패");return ok?1:0;})));
        var cooldown=Commands.literal("cooldown").executes(c->{reply(c,"/morph cooldown <mob> [primary|secondary] <0..3600초|reset> | 몹 이름만 입력하면 조회 | /morph cooldown reset: 전체 기본값 복원");return 1;});
        cooldown.then(Commands.literal("reset").executes(c->{boolean ok=manager(c).resetCooldowns();reply(c,ok?"모든 능력 쿨다운을 기본값으로 복원하고 저장했습니다. 진행 중 대기는 경과 시간을 유지합니다.":"설정 저장 실패: 쿨다운을 변경하지 않았습니다.");return ok?1:0;}));
        var mob=Commands.argument("mob",StringArgumentType.word())
            .suggests((c,b)->SharedSuggestionProvider.suggest(MorphRegistry.profiles().stream().filter(p->p.primary()!=Active.NONE||p.secondary()!=Active.NONE).map(MorphProfile::id),b))
            .executes(c->showCooldown(c,null));
        mob.then(Commands.argument("seconds",IntegerArgumentType.integer(0,3600)).executes(c->setCooldown(c,false,false)));
        mob.then(Commands.literal("reset").executes(c->setCooldown(c,false,true)));
        for(boolean secondary:new boolean[]{false,true}){
            var slot=Commands.literal(secondary?"secondary":"primary").executes(c->showCooldown(c,secondary));
            slot.then(Commands.argument("seconds",IntegerArgumentType.integer(0,3600)).executes(c->setCooldown(c,secondary,false)));
            slot.then(Commands.literal("reset").executes(c->setCooldown(c,secondary,true)));mob.then(slot);
        }
        cooldown.then(mob);root.then(cooldown);
        root.then(Commands.literal("list").executes(c->{reply(c,"지원 "+MorphRegistry.profiles().size()+"종: "+String.join(", ",MorphRegistry.profiles().stream().map(MorphProfile::id).toList()));return 1;}));
        root.then(Commands.literal("reload").executes(c->{boolean ok=manager(c).reload();reply(c,ok?"설정을 다시 읽었습니다. 능력 쿨다운은 진행 중 대기에도 반영합니다. 변신 주기는 /morph interval <초>":"설정 오류: 기존 설정 유지. 로그를 확인하세요.");return ok?1:0;}));
        dispatcher.register(root);
    }
    private static MorphManager manager(CommandContext<CommandSourceStack> c){return RandomMorphMod.manager(c.getSource().getServer());}
    private static void reply(CommandContext<CommandSourceStack> c,String text){c.getSource().sendSuccess(()->Component.literal(text),false);}
    private static MorphProfile cooldownProfile(CommandContext<CommandSourceStack> c){
        var profile=MorphRegistry.find(StringArgumentType.getString(c,"mob"));
        if(profile==null)c.getSource().sendFailure(Component.literal("등록되지 않은 몹입니다. /morph list"));
        return profile;
    }
    private static String cooldownText(MorphConfig config,MorphProfile profile,boolean secondary){
        var action=secondary?profile.secondary():profile.primary();String key=secondary?"Shift+F":"F";
        if(action==Active.NONE)return key+": 능력 없음";
        boolean defaults=config.mobs.get(profile.id()).cooldown(secondary)==null;
        return key+" "+action.title()+": "+config.cooldownTicks(profile,secondary)/20+"초 ("+(defaults?"기본값":"설정값")+")";
    }
    private static int showCooldown(CommandContext<CommandSourceStack> c,Boolean secondary){
        var profile=cooldownProfile(c);if(profile==null)return 0;var config=manager(c).config();
        reply(c,profile.id()+" | "+(secondary==null?cooldownText(config,profile,false)+" | "+cooldownText(config,profile,true):cooldownText(config,profile,secondary)));return 1;
    }
    private static int setCooldown(CommandContext<CommandSourceStack> c,boolean secondary,boolean reset){
        var profile=cooldownProfile(c);if(profile==null)return 0;
        if((secondary?profile.secondary():profile.primary())==Active.NONE){c.getSource().sendFailure(Component.literal(profile.id()+": "+(secondary?"Shift+F":"F")+" 능력이 없습니다."));return 0;}
        var manager=manager(c);
        if(!manager.cooldown(profile,secondary,reset?null:IntegerArgumentType.getInteger(c,"seconds"))){c.getSource().sendFailure(Component.literal("설정 저장 실패: 쿨다운을 변경하지 않았습니다."));return 0;}
        reply(c,profile.id()+" | "+cooldownText(manager.config(),profile,secondary)+" | 서버 전체 적용·저장 완료. 진행 중 대기는 경과 시간을 유지합니다.");return 1;
    }
    private static int size(CommandContext<CommandSourceStack> c,int size,Collection<ServerPlayer> players){
        int success=0;for(var p:players)if(manager(c).size(p,size))success++;
        reply(c,"큐브 크기 "+size+" 적용 "+success+" / "+players.size()+"명 (남은 시간·체력 비율 유지)");return success;
    }
    private static int set(CommandContext<CommandSourceStack> c,Collection<ServerPlayer> players){
        var profile=MorphRegistry.find(StringArgumentType.getString(c,"mob"));if(profile==null){c.getSource().sendFailure(Component.literal("등록되지 않은 몹입니다. /morph list"));return 0;}
        int success=0;for(var p:players)if(manager(c).set(p,profile))success++;
        reply(c,"변신 적용 "+success+" / "+players.size()+"명");return success;
    }
    private static int run(CommandContext<CommandSourceStack> c,String action,Collection<ServerPlayer> players)throws CommandSyntaxException{
        var manager=manager(c);int success=0;
        if(action.equals("start")){boolean ok=manager.start(players);reply(c,ok?"3초 카운트다운 후 참가자 전원이 변신합니다.":"시작할 수 없습니다. 진행 중인 카운트다운이나 참가 조건을 확인하세요.");return ok?players.size():0;}
        for(var p:players){boolean ok=switch(action){
            case "next","reroll" -> manager.next(p);
            case "stop" -> manager.stop(p);
            case "pause","resume" -> {manager.pause(p,action.equals("pause"));yield manager.session(p)!=null;}
            case "status" -> {var s=manager.session(p);reply(c,p.getName().getString()+": "+(s==null?"참가 중 아님":s.form()+(MorphRegistry.find(s.form()).cube()?" (크기 "+s.cubeSize()+")":"")+" | "+s.remainingSeconds()+"초 | 주기 "+s.intervalSeconds()+"초 | paused="+s.paused()));yield true;}
            default -> false;
        };if(ok)success++;}
        if(!action.equals("status"))reply(c,action+" 적용 "+success+" / "+players.size()+"명");return success;
    }
}
