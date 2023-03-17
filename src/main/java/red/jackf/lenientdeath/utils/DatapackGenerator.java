package red.jackf.lenientdeath.utils;

import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import red.jackf.lenientdeath.LenientDeath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static red.jackf.lenientdeath.LenientDeath.error;

public abstract class DatapackGenerator {

    // Returns success, and how many items were added to the file.
    public static Pair<Boolean, Integer> generateDatapack() {

        try {
            var dir = Files.createDirectories(Path.of("lenientdeath"));
            var tagFolder = Files.createDirectories(dir.resolve("data/lenientdeath/tags/items"));

            // Tag File
            var safeItems = new ArrayList<Identifier>();
            var vanillaItems = new ArrayList<Identifier>();
            var modItems = new ArrayList<Identifier>();

            Registries.ITEM.getIds().stream().sorted(Comparator.comparing(Identifier::getNamespace).thenComparing(Identifier::getPath)).forEach(id -> {
                if (id.getNamespace().equals("minecraft")) vanillaItems.add(id);
                else modItems.add(id);
            });

            Stream.concat(vanillaItems.stream(), modItems.stream()).forEach(id -> {
                var item = Registries.ITEM.get(id);
                if (LenientDeath.validSafeFoods(item)
                    || LenientDeath.validSafeArmor(item)
                    || LenientDeath.validSafeEquipment(item)) safeItems.add(id);
            });
            writeItemsToTagFile(tagFolder.resolve("safe.json"), safeItems);

            // pack.mcmeta
            copyFileFromResources("/assets/lenientdeath/icon.png", dir.resolve("pack.png"));
            copyFileFromResources("/assets/lenientdeath/pack.mcmeta", dir.resolve("pack.mcmeta"));

            return new Pair<>(true, safeItems.size());
        } catch (Exception ex) {
            error("Error generating tags", ex);
            return new Pair<>(false, 0);
        }
    }

    private static void copyFileFromResources(String resourceLocation, Path outputLocation) throws IOException {
        var output = outputLocation.toFile();
        var input = DatapackGenerator.class.getResourceAsStream(resourceLocation);
        if (!output.exists() && input != null) {
            Files.copy(input, output.getAbsoluteFile().toPath());
        }
    }

    private static void writeItemsToTagFile(Path file, List<Identifier> ids) throws IOException {
        var fileContents = new StringBuilder("""
            {
              "values": [
            """);
        ids.forEach(id -> {
            if (id.getNamespace().equals("minecraft"))
                fileContents.append("    \"")
                    .append(id)
                    .append("\",\n");
            else
                fileContents.append("    {\n      \"id\": \"") // modded items, mark not required
                    .append(id)
                    .append("\",\n      \"required\": false\n    },\n");
        });

        //trim last comma and newline
        fileContents.delete(fileContents.length() - 2, fileContents.length());
        fileContents.append("""
                
              ]
            }""");
        Files.write(file, fileContents.toString().lines().collect(Collectors.toList()));
    }
}
