package red.jackf.lenientdeath.command;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;

import static net.minecraft.network.chat.Component.literal;

public class Formatting {
    public static final Style SUCCESS = Style.EMPTY.withColor(ChatFormatting.GREEN);
    public static final Style INFO = Style.EMPTY.withColor(ChatFormatting.YELLOW);
    public static final Style ERROR = Style.EMPTY.withColor(ChatFormatting.RED);

    public static final Style VARIABLE = Style.EMPTY.withColor(ChatFormatting.AQUA);
    public static final Style NUMBER = Style.EMPTY.withColor(ChatFormatting.GOLD);
    public static final Style STRING = Style.EMPTY.withColor(ChatFormatting.GREEN);
    public static final Style PLAYER = Style.EMPTY.withColor(ChatFormatting.WHITE);


    public static Style runCommand(Style base, String command) {
        return base.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                   .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, literal(command)));
    }

    private static MutableComponent colour(String text, int colour, boolean bold) {
        Style base = Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("lenientdeath.title")))
                .withBold(bold);
        return literal(text).withStyle(base.withColor(colour));
    }

    private static final Component PREFIX = Component.empty()
            .append(colour("[", 0xFFFFFF, false))
            .append(colour("L", 0xDDDDDD, true))
            .append(colour("D", 0xBBBBBB, true))
            .append(colour("]", 0x999999, false))
            .append(CommonComponents.SPACE);


    public static Component successLine(MutableComponent component) {
        return format(SUCCESS, component);
    }

    public static Component infoLine(MutableComponent component) {
        return format(INFO, component);
    }

    public static Component errorLine(MutableComponent component) {
        return format(ERROR, component);
    }

    private static Component format(Style style, MutableComponent content) {
        return Component.empty().append(PREFIX).append(content.withStyle(style));
    }

    public static Component commandButton(Style style, Component label, String command) {
        return ComponentUtils.wrapInSquareBrackets(label).withStyle(runCommand(style, command));
    }

    // object formatters

    public static MutableComponent listItem(Component item) {
        return literal(" • ").append(item);
    }

    public static Component bool(boolean value) {
        return value ? literal("true").withStyle(SUCCESS) : literal("false").withStyle(ERROR);
    }

    public static Component integer(int value) {
        return literal(String.valueOf(value)).withStyle(NUMBER);
    }

    public static Component floating(float value) {
        return literal(String.valueOf(value)).withStyle(NUMBER);
    }

    public static Component string(String value) {
        return literal(value).withStyle(STRING);
    }

    public static Component player(ServerPlayer player) {
        return player.getDisplayName().copy().withStyle(PLAYER);
    }

    public static Component variable(String value) {
        return literal(value).withStyle(VARIABLE);
    }

    public static Component variable(Component value) {
        return value.copy().withStyle(VARIABLE);
    }
}
