package kr.randommorph.fabric.ability;
public enum Active {
    SHULKER_TELEPORT("부착 순간이동",4), SUMMON_VEX("벡스 소환",17), WITHER_BLUE_SKULL("파란 위더 해골",10),
    NONE("",0), GRAZE("풀 먹기",2), TELEPORT("순간이동",4), EXPLODE("폭발",12), BLAZE_FIRE("화염구 3연발",9),
    GHAST_FIRE("대형 화염구",4), WITHER_SKULL("위더 해골",2), DRAGON_FIRE("드래곤 화염구",5),
    SHULKER_BULLET("셜커 탄환",4), LASER("가디언 레이저",5), SONIC_BOOM("소닉 붐",8),
    FANGS("소환사 이빨",6), ARROW("전용 활/석궁 받기",2), POISON_ARROW("독 활 받기",3),
    SLOW_ARROW("감속 활 받기",2), WEAK_ARROW("나약함 활 받기",3), TRIDENT("삼지창",4),
    SNOWBALL("눈덩이",1), SPIT("침 뱉기",2), WIND_CHARGE("윈드 차지",3),
    SHELL("껍질 열기/닫기",1), PLAY_DEAD("죽은 척",15), CAMEL_DASH("낙타 돌진",3),
    NAUTILUS_DASH("수중 돌진",2), GOAT_RAM("염소 들이받기",8), RAVAGER_ROAR("포효",10),
    WITCH_HEAL("회복 물약 마시기",12), WITCH_POTION("무작위 물약 투척",4), PUFFER_INFLATE("복어 팽창",1);
    private final String title; private final int cooldown;
    Active(String title,int seconds){this.title=title;this.cooldown=seconds*20;}
    public String title(){return title;} public int cooldownTicks(){return cooldown;}
}
