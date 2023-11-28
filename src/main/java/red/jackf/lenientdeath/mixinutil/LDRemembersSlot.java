package red.jackf.lenientdeath.mixinutil;

public interface LDRemembersSlot {
    String LD_REMEMBERED_SLOT = "LenientDeathRememberedSlotId";

    void ld$setSlot(int slot);
}
