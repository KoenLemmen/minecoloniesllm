package com.thereallemon.llmconversations.llm;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.thereallemon.llmconversations.config.LLMConfig;
import com.thereallemon.llmconversations.util.DebugLogger;
import okhttp3.*;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.List;

/**
 * Client for making async requests to OpenRouter API
 */
public class OpenRouterClient {
    private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    private final OkHttpClient client;
    private final Gson gson;
    
    public OpenRouterClient() {
        this.client = new OkHttpClient();
        this.gson = new Gson();
    }
    
    /**
     * Send a chat request asynchronously with conversation history (no separate user message)
     * @param apiKey OpenRouter API key
     * @param model Model identifier
     * @param systemPrompt System prompt for the conversation
     * @param conversationHistory Messages including the latest user message
     * @return CompletableFuture with the assistant's response
     */
    public CompletableFuture<String> sendChatRequest(
            String apiKey,
            String model,
            String systemPrompt,
            List<ChatMessage> conversationHistory) {

        return sendChatRequestWithRetry(apiKey, model, systemPrompt, conversationHistory, null, 0);
    }

    /**
     * Send a chat request asynchronously (legacy method with separate user message)
     * @param apiKey OpenRouter API key
     * @param model Model identifier
     * @param systemPrompt System prompt for the conversation
     * @param conversationHistory Previous messages in the conversation
     * @param userMessage Current user message
     * @return CompletableFuture with the assistant's response
     */
    public CompletableFuture<String> sendChatRequest(
            String apiKey,
            String model,
            String systemPrompt,
            List<ChatMessage> conversationHistory,
            String userMessage) {
        
        return sendChatRequestWithRetry(apiKey, model, systemPrompt, conversationHistory, userMessage, 0);
    }

    /**
     * Internal method with retry logic (max 3 retries total)
     */
    private CompletableFuture<String> sendChatRequestWithRetry(
            String apiKey,
            String model,
            String systemPrompt,
            List<ChatMessage> conversationHistory,
            String userMessage,
            int attemptNumber) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                if (attemptNumber > 0) {
                    DebugLogger.log("Retry attempt " + attemptNumber + " of 3");
                }

                DebugLogger.debugSection("LLM API Request");
                DebugLogger.debug("Model: {}", model);
                DebugLogger.debug("System Prompt: {}", systemPrompt);
                if (userMessage != null) {
                    DebugLogger.debug("User Message: {}", userMessage);
                }
                DebugLogger.debug("Conversation History Length: {}", conversationHistory.size());
                
                JsonObject requestBody = buildRequestBody(
                    model, systemPrompt, conversationHistory, userMessage
                );
                
                DebugLogger.debug("Request Body: {}", gson.toJson(requestBody));
                
                Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("HTTP-Referer", "https://minecraft-minecolonies-llm")
                    .addHeader("X-Title", "Minecolonies LLM Conversations")
                    .post(RequestBody.create(gson.toJson(requestBody), JSON))
                    .build();
                
                DebugLogger.debug("Sending request to OpenRouter...");
                
                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "No error details";
                        DebugLogger.error("API Error Response: " + errorBody);
                        throw new IOException("API Error: " + response.code() + 
                                             " - " + response.message() + " | " + errorBody);
                    }
                    
                    String responseBody = response.body().string();
                    DebugLogger.debug("Raw API Response: {}", responseBody);
                    
                    String parsedResponse = parseResponse(responseBody);

                    // Check if response is empty - retry if so
                    if (parsedResponse == null || parsedResponse.trim().isEmpty()) {
                        DebugLogger.error("Received empty response from LLM");
                        throw new IOException("Empty response from LLM");
                    }

                    DebugLogger.debug("Parsed Response: {}", parsedResponse);
                    DebugLogger.debugSeparator();
                    
                    return parsedResponse;
                }
            } catch (Exception e) {
                // Retry logic: max 3 attempts total
                if (attemptNumber < 3) {
                    DebugLogger.error("Request failed (attempt " + (attemptNumber + 1) + "): " + e.getMessage());
                    DebugLogger.log("Retrying request...");

                    // Wait a bit before retrying (exponential backoff)
                    try {
                        Thread.sleep((attemptNumber + 1) * 1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }

                    // Retry recursively
                    try {
                        return sendChatRequestWithRetry(apiKey, model, systemPrompt,
                            conversationHistory, userMessage, attemptNumber + 1).get();
                    } catch (Exception retryException) {
                        throw new RuntimeException("Failed to get LLM response after retries: " +
                                                 retryException.getMessage(), retryException);
                    }
                } else {
                    throw new RuntimeException("Failed to get LLM response after " + (attemptNumber + 1) +
                                             " attempts: " + e.getMessage(), e);
                }
            }
        });
    }
    
    /**
     * Build the JSON request body for the API call
     */
    private JsonObject buildRequestBody(
            String model,
            String systemPrompt,
            List<ChatMessage> history,
            String userMessage) {
        
        JsonObject request = new JsonObject();
        request.addProperty("model", model);
        
        JsonArray messages = new JsonArray();
        
        // Add system prompt
        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", systemPrompt);
        messages.add(systemMsg);
        
        // Add conversation history
        for (ChatMessage msg : history) {
            JsonObject historyMsg = new JsonObject();
            historyMsg.addProperty("role", msg.role);
            historyMsg.addProperty("content", msg.content);
            messages.add(historyMsg);
        }
        
        // Add current user message only if provided (for legacy API)
        if (userMessage != null) {
            JsonObject userMsg = new JsonObject();
            userMsg.addProperty("role", "user");
            userMsg.addProperty("content", userMessage);
            messages.add(userMsg);
        }

        request.add("messages", messages);
        
        // Add config from settings
        request.addProperty("max_tokens", 
            LLMConfig.CLIENT.maxTokens.get());
        request.addProperty("temperature", 
            LLMConfig.CLIENT.temperature.get());
        
        return request;
    }
    
    /**
     * Parse the API response and extract the assistant's message
     */
    private String parseResponse(String responseBody) {
        JsonObject response = gson.fromJson(responseBody, JsonObject.class);
        
        if (response.has("error")) {
            JsonObject error = response.get("error").getAsJsonObject();
            String message = error.has("message") ? 
                error.get("message").getAsString() : "Unknown error";
            throw new RuntimeException("API returned error: " + message);
        }
        
        return response
            .getAsJsonArray("choices")
            .get(0).getAsJsonObject()
            .getAsJsonObject("message")
            .get("content").getAsString();
    }
    
    /**
     * Represents a chat message in the conversation
     */
    public static class ChatMessage {
        public final String role; // "user" or "assistant"
        public final String content;
        
        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}
