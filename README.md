# LLM Conversations for Minecolonies

A companion mod for [Minecolonies](https://github.com/ldtteam/minecolonies) that adds AI-powered conversations with NPCs using Large Language Models (LLMs) via the OpenRouter API.

## 🎮 Features

- **Natural Conversations**: Talk to Minecolonies citizens using natural language
- **Context-Aware**: Citizens know their job, happiness, colony, and remember past conversations
- **Stat Freezing**: Hunger and sleep don't decrease during conversations
- **Task Pausing**: Citizens pause their work while talking
- **Conversation Memory**: Citizens remember previous conversations
- **Configurable**: Customize API settings, models, and prompts

## 📋 Requirements

- **Minecraft**: 1.21.1
- **Mod Loader**: NeoForge 21.1.80 or higher
- **Dependencies**: Minecolonies 1.21.1
- **OpenRouter API Key**: Get one free at [openrouter.ai](https://openrouter.ai)

## 🚀 Installation

1. Download and install NeoForge for Minecraft 1.21.1
2. Install the latest compatible Minecolonies mod
3. Place this mod's JAR file in your `mods` folder
4. Launch Minecraft and configure your API key (see Configuration)

## ⚙️ Configuration

The configuration file is located at `config/llm-conversations-client.toml`

### First-Time Setup

1. Get an API key from [OpenRouter](https://openrouter.ai) (free tier available)
2. Edit the config file and set your `api_key`
3. Optionally customize the `model` (default: anthropic/claude-3-haiku)
4. Customize the `system_prompt` if desired

### Configuration Options

```toml
[llm_settings]
    # Your OpenRouter API key
    api_key = ""
    
    # Model to use (examples: anthropic/claude-3-haiku, openai/gpt-4o-mini)
    model = "anthropic/claude-3-haiku"
    
    # System prompt template with placeholders
    system_prompt = "You are {name}, a {job} in the colony of {colony_name}..."
    
    # Maximum tokens per response (50-500)
    max_tokens = 150
    
    # Temperature for responses (0.0-2.0)
    temperature = 0.7

[idle_conversations]
    # Allow citizens to randomly start conversations
    enable_idle_conversations = false
    
    # Chance for idle conversation (0.0-1.0)
    idle_conversation_chance = 0.01

[memory]
    # Number of past conversations to remember (0-20)
    max_history_length = 5
```

### System Prompt Placeholders

You can use these placeholders in your system prompt:
- `{name}` - Citizen's name
- `{job}` - Citizen's job title
- `{colony_name}` - Colony name
- `{player_name}` - Player's name
- `{happiness}` - Happiness level (0-10)
- `{saturation}` - Hunger level (0-20)
- `{skills}` - Citizen's skills

## 🎯 Usage

### Starting a Conversation

1. **Shift + Right-Click** on any Minecolonies citizen
2. Type your message in chat
3. The citizen will respond using AI
4. Continue the conversation by typing more messages

### Ending a Conversation

You can end a conversation in two ways:
1. **Type an exit word** in chat:
   - `goodbye`
   - `bye`
   - `exit`
   - `stop`
2. **Press ESC** (or your configured cancel conversation key)

### During Conversation

- Your messages will only be seen by the citizen (not global chat)
- The citizen's stats won't decrease
- The citizen will pause their current task
- All messages are shown with colored names for clarity

## 🔧 Building from Source

### Prerequisites

- JDK 21
- Git

### Build Steps

```bash
# Clone the repository
git clone https://github.com/thereallemon/llm-conversations.git
cd llm-conversations

# Build with Gradle
./gradlew build

# The mod JAR will be in build/libs/
```

## 🐛 Troubleshooting

### "No OpenRouter API key configured"

- Edit `config/llm-conversations-client.toml` and add your API key
- Restart Minecraft after changing the config

### "Citizen is too busy right now"

- The citizen is in an important task that can't be interrupted
- Try again when they're doing a different task

### "Citizen is already in a conversation"

- Another player is talking to this citizen
- Wait for them to finish or talk to a different citizen

### Long Response Times

- LLM responses typically take 1-5 seconds
- Check your internet connection
- Try a faster model (e.g., smaller models respond quicker)

### API Errors

- Check your API key is valid
- Ensure you have API credits on OpenRouter
- Check the model name is correct
- Review OpenRouter's status page

## 💰 Cost Considerations

- OpenRouter charges per token used
- Free tier includes limited credits
- Costs are typically very low (fractions of a cent per conversation)
- Configure `max_tokens` to control costs
- Keep conversations brief to minimize usage

## 🔒 Privacy & Safety

- Your conversations are sent to OpenRouter's API
- Review OpenRouter's privacy policy
- API keys are stored locally in your config file
- Never share your API key with others
- Conversation history is stored locally in your world save

## 🤝 Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## 📝 License

This mod is licensed under the MIT License. See LICENSE file for details.

## 🙏 Credits

- **Minecolonies Team**: For the amazing Minecolonies mod
- **NeoForge Team**: For the modding framework
- **OpenRouter**: For providing the LLM API infrastructure

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/thereallemon/llm-conversations/issues)
- **Discord**: Join the Minecolonies Discord for community support
- **Documentation**: See the [plan.md](plan.md) file for implementation details

## 🗺️ Roadmap

Future features planned:
- Voice synthesis for citizen responses
- Multi-language support
- Custom GUI for conversations
- Group conversations
- Quest generation via LLM
- Emotion indicators
- Enhanced memory system

## ⚠️ Important Notes

- This is a companion mod, not part of official Minecolonies
- Requires an active internet connection for LLM features
- API costs apply based on OpenRouter's pricing
- Test in creative mode first before using in survival worlds
- Back up your worlds regularly

## 📚 Additional Resources

- [OpenRouter Documentation](https://openrouter.ai/docs)
- [Minecolonies Wiki](https://wiki.minecolonies.ldtteam.com/)
- [NeoForge Documentation](https://docs.neoforged.net/)
- [Implementation Plan](plan.md) - Detailed technical documentation

---

**Enjoy AI-powered conversations with your Minecolonies citizens!** 🎮🤖
