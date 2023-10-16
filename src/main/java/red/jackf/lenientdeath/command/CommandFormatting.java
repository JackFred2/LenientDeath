package red.jackf.lenientdeath.command;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

class CommandFormatting {
    private static final ChatFormatting TEXT_COLOUR = ChatFormatting.WHITE;
    private static final ChatFormatting SUCCESS_COLOUR = ChatFormatting.GREEN;
    private static final ChatFormatting INFO_COLOUR = ChatFormatting.YELLOW;
    private static final ChatFormatting ERROR_COLOUR = ChatFormatting.RED;
    private static final ChatFormatting VARIABLE_COLOUR = ChatFormatting.AQUA;

    static Component success(Text... parts) {
        return build(TextType.SUCCESS, parts);
    }

    static Component success(Component component) {
        return success(new Text.Plain(component));
    }

    static Component info(Text... parts) {
        return build(TextType.INFO, parts);
    }

    static Component info(Component component) {
        return info(new Text.Plain(component));
    }

    static Component error(Text... parts) {
        return build(TextType.ERROR, parts);
    }

    static Component error(Component component) {
        return error(new Text.Plain(component));
    }

    static Text text(Component text) {
        return new Text.Plain(text);
    }

    static Text variable(String text) {
        return new Text.Plain(Component.literal(text).withStyle(VARIABLE_COLOUR));
    }

    static Text symbol(String symbol) {
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

    @SuppressWarnings("ClassEscapesDefinedScope")
    protected sealed interface Text {
        Component resolve(TextType type);

        record Plain(Component base) implements Text {
            @Override
            public Component resolve(TextType type) {
                return Component.literal("").withStyle(TEXT_COLOUR).append(base);
            }
        }

        record Symbol(String base) implements Text {
            @Override
            public Component resolve(TextType type) {
                return Component.literal(base).withStyle(type.colour);
            }
        }
    }

    protected enum TextType {
        SUCCESS(Component.literal("[+] ").withStyle(Style.EMPTY.withColor(SUCCESS_COLOUR)), SUCCESS_COLOUR),
        INFO(Component.literal("[•] ").withStyle(Style.EMPTY.withColor(INFO_COLOUR)), INFO_COLOUR),
        ERROR(Component.literal("[-] ").withStyle(Style.EMPTY.withColor(ERROR_COLOUR)), ERROR_COLOUR);
        private final Component prefix;
        private final ChatFormatting colour;

        TextType(Component prefix, ChatFormatting colour) {
            this.prefix = prefix;
            this.colour = colour;
        }
    }
}
