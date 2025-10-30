# Quick Start Guide

## Getting Started with LLM Conversations

### Step 1: Get an OpenRouter API Key

1. Visit [https://openrouter.ai](https://openrouter.ai)
2. Sign up for a free account
3. Navigate to your API Keys section
4. Create a new API key
5. Copy the key (you'll need it in Step 3)

### Step 2: Install the Mod

1. Install NeoForge 21.1.80+ for Minecraft 1.21.1
2. Install Minecolonies 1.21.1
3. Place this mod's JAR in your `mods` folder
4. Launch Minecraft once to generate the config file

### Step 3: Configure the Mod

1. Close Minecraft
2. Open `config/llm-conversations-client.toml`
3. Paste your API key:
   ```toml
   api_key = "your-api-key-here"
   ```
4. Save and restart Minecraft

### Step 4: Start Chatting!

1. Load your world with a Minecolonies colony
2. Find any citizen
3. **Look at the citizen** and **Press V** (default key, customizable)
4. Type a message in chat: "Hello! How are you today?"
5. Wait a few seconds for the AI response
6. Continue the conversation!
7. When done, either:
   - Type one of the exit words: `goodbye`, `bye`, `exit`, or `stop`
   - Press **ESC** (default cancel key, customizable)

**Note:** You can customize both keybinds in Options > Controls > Key Binds > LLM Conversations

## Example Conversation

```
[You shift+right-click a Builder named "John"]

System: Started conversation with John. Type in chat to talk, or say 'goodbye' to end.

You: Hello John! How's the building going?
John: Hello! The building is going well, though I could use more materials. 
      I'm currently working on the town hall expansion. What brings you by?

You: Do you need any help with resources?
John: That would be wonderful! We're running low on oak planks and stone bricks. 
      If you could bring some by, it would really speed things up!

You: goodbye
System: Ended conversation with John
```

## Tips

- **Be patient**: AI responses take 1-5 seconds
- **Ask about their work**: Citizens know their job and colony
- **Keep it brief**: Short messages work best
- **Multiple conversations**: You can talk to multiple citizens (one at a time)
- **Remember context**: Citizens remember recent conversations

## Common Questions

**Q: How much does this cost?**
A: Very little! Most conversations cost fractions of a cent. OpenRouter offers free credits to start.

**Q: Can I use other AI models?**
A: Yes! Edit the config and change the `model` setting. Popular options:
- `anthropic/claude-3-haiku` (fast, cheap, default)
- `openai/gpt-4o-mini` (good balance)
- `meta-llama/llama-3.1-8b-instruct` (free!)

**Q: Will citizens remember what we talked about?**
A: Yes! They remember the last 5 conversations (configurable).

**Q: Can other players see my conversations?**
A: No, conversations are private between you and the citizen.

**Q: What if the citizen gives a weird response?**
A: AI can be unpredictable! Adjust the `system_prompt` in the config to guide behavior.

## Next Steps

- Customize the system prompt to change citizen personalities
- Try different AI models for varied responses
- Enable idle conversations (citizens randomly talk to you)
- Adjust `max_tokens` and `temperature` for different response styles

## Need Help?

- Check the [README.md](README.md) for full documentation
- Review [plan.md](plan.md) for technical details
- Open an issue on GitHub for bugs or questions

Happy chatting! 🎮💬
