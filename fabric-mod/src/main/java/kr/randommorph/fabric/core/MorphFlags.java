package kr.randommorph.fabric.core;
import net.minecraft.core.Direction;
/** form_v7: persistent sheep dye bits and an independent three-size cube code. */
public final class MorphFlags {
    public static final int STUNG=32,SHEARED=64,GRAZE_SHIFT=7,FACE_SHIFT=13,COLOR_SHIFT=16,CUBE_SHIFT=20;
    private static final int COLOR_MASK=15<<COLOR_SHIFT;
    private static final int CUBE_MASK=3<<CUBE_SHIFT;
    private static final int GRAZE_MASK=63<<GRAZE_SHIFT,FACE_MASK=7<<FACE_SHIFT;
    private MorphFlags(){}
    public static int grazing(int flags){return (flags&GRAZE_MASK)>>>GRAZE_SHIFT;}
    public static int withGrazing(int flags,int ticks){return (flags&~GRAZE_MASK)|(Math.clamp(ticks,0,40)<<GRAZE_SHIFT);}
    public static Direction face(int flags){int face=(flags&FACE_MASK)>>>FACE_SHIFT;return face<6?Direction.values()[face]:Direction.DOWN;}
    public static int withFace(int flags,Direction face){return (flags&~FACE_MASK)|(face.ordinal()<<FACE_SHIFT);}
    public static int color(int flags){return (flags&COLOR_MASK)>>>COLOR_SHIFT;}
    public static int withColor(int flags,int color){return (flags&~COLOR_MASK)|(Math.clamp(color,0,15)<<COLOR_SHIFT);}
    public static int cubeSize(int flags){return switch((flags&CUBE_MASK)>>>CUBE_SHIFT){case 0->2;case 1->1;case 2->4;default->throw new IllegalArgumentException("Invalid cube size code");};}
    public static int withCubeSize(int flags,int size){
        int code=switch(size){case 2->0;case 1->1;case 4->2;default->throw new IllegalArgumentException("Cube size: 1, 2 or 4");};
        return (flags&~CUBE_MASK)|(code<<CUBE_SHIFT);
    }
    public static boolean valid(int flags){return flags>=0&&(flags&~(127|GRAZE_MASK|FACE_MASK|COLOR_MASK|CUBE_MASK))==0&&grazing(flags)<=40&&((flags&FACE_MASK)>>>FACE_SHIFT)<6&&((flags&CUBE_MASK)>>>CUBE_SHIFT)<3;}
}
