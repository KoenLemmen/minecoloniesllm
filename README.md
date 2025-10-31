# LLM Conversations for Minecolonies

A companion mod for [Minecolonies](https://github.com/ldtteam/minecolonies) that adds AI-powered conversations with NPCs using Large Language Models (LLMs) via the OpenRouter API.

**Tested with**: [Minecolonies Official 1.21-1.18](https://www.curseforge.com/minecraft/modpacks/minecolonies-official) modpack on CurseForge.

⚠️ **Version Notice**: This mod is made specifically for Minecraft 1.21.1 and may not be actively maintained. Pull requests for updates and new features are welcome!

## 🎮 Features

- **Natural Conversations**: Talk to Minecolonies citizens using natural language
- **Context-Aware**: Citizens know their job, happiness, colony, and remember past conversations
- **Stat Freezing**: Hunger and saturation don't decrease during conversations
- **Movement Control**: Citizens stop walking and face you during conversations
- **Auto-End on Distance**: Conversations automatically end if you walk too far away
- **Task Pausing**: Citizens pause their work while talking
- **Conversation Memory**: Citizens remember previous conversations
- **Configurable**: Customize API settings, models, prompts, and behavior
- **In-Game Config**: Edit most settings directly in-game (except system prompt)

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

The configuration file is located at `config/llmconversations-client.toml`

### In-Game Configuration

Most settings can be edited **in-game** through the Mod Options menu:
1. Go to **Mods** menu
2. Find **LLM Conversations**
3. Click **Config**
4. Edit settings and click **Save**

**Note**: The system prompt is too large to edit in-game and must be edited in the config file directly.

### First-Time Setup

1. Get an API key from [OpenRouter](https://openrouter.ai) (free tier available)
2. Edit the config in-game or in the file at `config/llmconversations-client.toml`
3. Set your `api_key`
4. Optionally customize the `model` (default: anthropic/claude-3-haiku)
5. Customize other settings as desired

### Configuration Options

```toml
[llm_settings]
    # Your OpenRouter API key
    api_key = ""
    
    # Model to use (examples: anthropic/claude-3-haiku, openai/gpt-4o-mini)
    model = "anthropic/claude-3-haiku"
    
    # System prompt template with placeholders
    system_prompt = "You are {name}, a {job} in the colony of {colony_name}..."
    
    # Maximum tokens per response (100-1000)
    max_tokens = 300
    
    # Temperature for responses (0.0-2.0)
    temperature = 0.7

[idle_conversations]
    # Allow citizens to randomly start conversations (not implemented)
    enable_idle_conversations = false
    
    # Chance for idle conversation (0.0-1.0) (not implemented)
    idle_conversation_chance = 0.01

[memory]
    # Number of past conversations to remember (0-20)
    max_history_length = 5

[conversation_control]
    # Words that end conversations (comma-separated)
    exit_words = "goodbye,bye,cya,exit,stop,later"
    
    # Max distance in blocks before auto-ending (0 = disabled)
    # Range: 0.0 - 50.0
    max_conversation_distance = 10.0

[debug]
    # Enable debug logging
    debug_mode = false
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

1. **Press V** on any Minecolonies citizen to start a conversation
2. Type your message in chat
3. The citizen will respond using AI
4. Continue the conversation by typing more messages

### Ending a Conversation

You can end a conversation in three ways:
1. **Type an exit word** in chat:
   - `goodbye`
   - `bye`
   - `cya`
   - `exit`
   - `stop`
   - `later`
2. **Press ESC** (or your configured cancel conversation key)
3. **Walk too far away** - Conversations automatically end if you exceed the configured distance (default: 10 blocks)

### During Conversation

- Your messages will only be seen by the citizen (not global chat)
- The citizen's stats (hunger/saturation) won't decrease
- The citizen will stop walking and face you
- The citizen will pause their current task
- The citizen will track your movement if you walk around
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

- Edit `config/llmconversations-client.toml` and add your API key
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

- OpenRouter charges per token used based on the model selected
- Free tier includes limited credits to get started
- **Typical costs are very low**: Most conversations cost **less than $0.001 (0.1 cents)** per conversation
  - Tested with OpenAI's OSS 120B model
  - A typical 3-5 message conversation uses ~500-1000 tokens total
- **Cost factors**:
  - Model selection: Cheaper models (e.g., claude-3-haiku) cost less than premium models (e.g., GPT-4)
  - Conversation length: Longer conversations use more tokens
  - `max_tokens` setting: Controls response length and cost
  - Conversation frequency: More conversations = higher total cost
- **Cost control**:
  - Use efficient models like `anthropic/claude-3-haiku` or `openai/gpt-4o-mini`
  - Set `max_tokens` to 300 or lower for brief responses
  - Keep conversations focused and concise
  - Configure `max_conversation_distance` to auto-end idle conversations
- **Example costs** (approximate, varies by model):
  - 1 brief conversation: $0.0001 - $0.001 (0.01 - 0.1 cents)
  - 100 conversations: $0.01 - $0.10 (1 - 10 cents)
  - Daily casual play: < $0.05 (5 cents)
  
OpenRouter provides detailed pricing on their [models page](https://openrouter.ai/models)

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

None lmao

## 🗺️ Roadmap

Probably nothing really. But here's some ideas I had, feel free to contribute!
- Relation system between players and citizens
- Citizens initiating conversations based on context or chance
- Citizens having conversations with each other, that the player can overhear or talk about / refer to later
- Conversations with multiple citizens at once
- Conversations that can be joined

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
