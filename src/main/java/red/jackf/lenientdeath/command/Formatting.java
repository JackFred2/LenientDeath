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

    private static final Style MOD_NAME_HOVER = Style.EMPTY
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("lenientdeath.title")));

    public static Style runCommand(Style base, String command) {
        return base.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                   .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, literal(command)));
    }



    private static final Component SUCCESS_PREFIX = literal("[+] ")
            .withStyle(MOD_NAME_HOVER.applyTo(SUCCESS));
    private static final Component INFO_PREFIX = literal("[•] ")
            .withStyle(MOD_NAME_HOVER.applyTo(INFO));
    private static final Component ERROR_PREFIX = literal("[-] ")
            .withStyle(MOD_NAME_HOVER.applyTo(ERROR));



    public static Component successLine(MutableComponent component) {
        return format(SUCCESS_PREFIX, SUCCESS, component);
    }

    public static Component infoLine(MutableComponent component) {
        return format(INFO_PREFIX, INFO, component);
    }

    public static Component errorLine(MutableComponent component) {
        return format(ERROR_PREFIX, ERROR, component);
    }

    private static Component format(Component prefix, Style style, MutableComponent content) {
        return Component.empty().append(prefix).append(content.withStyle(style));
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
        return variable(String.valueOf(value));
    }

    public static Component floating(float value) {
        return variable(String.valueOf(value));
    }

    public static Component string(String value) {
        return variable(value);
    }

    public static Component player(ServerPlayer player) {
        return variable(player.getDisplayName());
    }

    public static Component variable(String value) {
        return literal(value).withStyle(VARIABLE);
    }

    public static Component variable(Component value) {
        return value.copy().withStyle(VARIABLE);
    }
}
