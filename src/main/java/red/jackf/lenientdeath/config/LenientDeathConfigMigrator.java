package red.jackf.lenientdeath.config;

import red.jackf.jackfredlib.api.config.migration.MigratorBuilder;
import red.jackf.lenientdeath.LenientDeath;

public class LenientDeathConfigMigrator {
    public static MigratorBuilder<LenientDeathConfig> create() {
        //noinspection UnnecessaryLocalVariable
        MigratorBuilder<LenientDeathConfig> builder = MigratorBuilder.forMod(LenientDeath.MODID);

        // in future add migrations here

        return builder;
    }
}
