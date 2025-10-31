package com.thereallemon.llmconversations.util;

import com.thereallemon.llmconversations.LLMConversationsMod;

public class DebugLogger {
    public static void log(String message) {
        LLMConversationsMod.LOGGER.info("[LLMConversations] " + message);
    }

    public static void debug(String message) {
        LLMConversationsMod.LOGGER.debug("[LLMConversations] " + message);
    }

    public static void debug(String format, Object... args) {
        LLMConversationsMod.LOGGER.debug("[LLMConversations] " + formatMessage(format, args));
    }

    public static void debugSection(String title) {
        LLMConversationsMod.LOGGER.debug("[LLMConversations] ========== " + title + " ==========");
    }

    public static void debugSeparator() {
        LLMConversationsMod.LOGGER.debug("[LLMConversations] " + "=".repeat(50));
    }

    public static void error(String message) {
        LLMConversationsMod.LOGGER.error("[LLMConversations] " + message);
    }

    public static void error(String message, Throwable throwable) {
        LLMConversationsMod.LOGGER.error("[LLMConversations] " + message, throwable);
    }

    public static void error(String format, Object... args) {
        LLMConversationsMod.LOGGER.error("[LLMConversations] " + formatMessage(format, args));
    }

    private static String formatMessage(String format, Object... args) {
        String result = format;
        for (Object arg : args) {
            result = result.replaceFirst("\\{\\}", String.valueOf(arg));
        }
        return result;
    }
}

