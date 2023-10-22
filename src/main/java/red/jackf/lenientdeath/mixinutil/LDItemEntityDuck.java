package red.jackf.lenientdeath.mixinutil;

/**
 * Used to mark an entity as a 'death drop', in case we want to add certain immunities to it.
 */
public interface LDItemEntityDuck {
    String IS_DEATH_DROP_ITEM = "LenientDeathIsDeathDrop";

    void lenientdeath$markDeathDropItem();

    boolean lenientdeath$isDeathDropItem();
}
