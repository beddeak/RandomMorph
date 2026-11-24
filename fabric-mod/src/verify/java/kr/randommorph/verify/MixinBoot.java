package kr.randommorph.verify;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.impl.launch.knot.Knot;
/** Loader initialization only. Never invokes GameProvider.launch or Minecraft client/server Main. */
public final class MixinBoot {
    public static void main(String[] args)throws Exception{
        var side=EnvType.valueOf(args[0]);
        ClassLoader transformed=new Knot(side).init(new String[0]);
        Class.forName("kr.randommorph.verify.TransformedProbe",true,transformed).getMethod("run",String.class).invoke(null,args[0]);
    }
}
