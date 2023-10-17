package red.jackf.lenientdeath;

public interface PerPlayerDuck {
    String PER_PLAYER_TAG_KEY = "LenientDeathPerPlayer";

    boolean lenientdeath$isPerPlayerEnabled();

    void lenientdeath$setPerPlayerEnabled(boolean newValue);
}
