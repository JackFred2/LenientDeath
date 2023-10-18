package red.jackf.lenientdeath.command;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Player;

import static net.minecraft.network.chat.Component.literal;

public class CommandFormatting {
    private static final ChatFormatting TEXT_COLOUR = ChatFormatting.WHITE;
    public static final ChatFormatting SUCCESS_COLOUR = ChatFormatting.GREEN;
    private static final ChatFormatting INFO_COLOUR = ChatFormatting.YELLOW;
    public static final ChatFormatting ERROR_COLOUR = ChatFormatting.RED;
    private static final ChatFormatting VARIABLE_COLOUR = ChatFormatting.AQUA;

    private static final Style LENIENT_DEATH_HOVER = Style.EMPTY
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("lenientdeath.title")));

    public static Component success(Text... parts) {
        return build(TextType.SUCCESS, parts);
    }

    public static Component success(Component component) {
        return success(new Text.Plain(component));
    }

    public static Component info(Text... parts) {
        return build(TextType.INFO, parts);
    }

    public static Component info(Component component) {
        return info(new Text.Plain(component));
    }

    public static Component error(Text... parts) {
        return build(TextType.ERROR, parts);
    }

    public static Component error(Component component) {
        return error(new Text.Plain(component));
    }


    public static Style suggests(Style base, String command) {
        return base.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, literal(command)));
    }


    public static Text text(Component text) {
        return new Text.Plain(text);
    }

    public static Text variable(String text) {
        return new Text.Plain(literal(text).withStyle(VARIABLE_COLOUR));
    }

    public static Text player(Player player) {
        return variable(player.getDisplayName().getString());
    }

    public static Text bool(boolean value) {
        return new Text.Plain(literal(value ? "true" : "false").withStyle(value ? SUCCESS_COLOUR : ERROR_COLOUR));
    }

    public static Text symbol(String symbol) {
        return new Text.Symbol(symbol);
    }

    private static Component build(TextType textType, Text[] parts) {
        var base = Component.empty();
        base.append(textType.prefix);
        for (Text part : parts) {
            base.append(part.resolve(textType));
        }
        return base;
    }

    public sealed interface Text {
        Component resolve(TextType type);

        record Plain(Component base) implements Text {
            @Override
            public Component resolve(TextType type) {
                return literal("").withStyle(TEXT_COLOUR).append(base);
            }
        }

        record Symbol(String base) implements Text {
            @Override
            public Component resolve(TextType type) {
                return literal(base).withStyle(type.colour);
            }
        }
    }

    public enum TextType {
        SUCCESS(literal("[+] ").withStyle(LENIENT_DEATH_HOVER.withColor(SUCCESS_COLOUR)), SUCCESS_COLOUR),
        INFO(literal("[•] ").withStyle(LENIENT_DEATH_HOVER.withColor(INFO_COLOUR)), INFO_COLOUR),
        ERROR(literal("[-] ").withStyle(LENIENT_DEATH_HOVER.withColor(ERROR_COLOUR)), ERROR_COLOUR),
        BLANK(literal("[?] ").withStyle(LENIENT_DEATH_HOVER), ChatFormatting.RESET);
        private final Component prefix;
        private final ChatFormatting colour;

        TextType(Component prefix, ChatFormatting colour) {
            this.prefix = prefix;
            this.colour = colour;
        }
    }
}
