# Build and Test Checklist

Use this checklist to build, test, and deploy the LLM Conversations mod.

## ☑️ Pre-Build Checklist

- [ ] JDK 21 is installed and in PATH
  ```powershell
  java -version  # Should show version 21
  ```

- [ ] Gradle is installed or Gradle Wrapper is present
  ```powershell
  gradle -version  # Or use .\gradlew
  ```

- [ ] Internet connection available (for dependency downloads)

## 🔨 Build Steps

### 1. Initial Setup

- [ ] Navigate to project directory
  ```powershell
  cd "c:\Users\koenl\Documents\Minecolonies idea\minecoloniesllm"
  ```

- [ ] Clean any previous builds
  ```powershell
  .\gradlew clean
  ```

### 2. First Build

- [ ] Run first build (will download dependencies)
  ```powershell
  .\gradlew build
  ```

- [ ] Check for errors in console output
- [ ] Verify JAR created in `build/libs/` folder

### 3. IDE Setup (Optional)

**For IntelliJ IDEA:**
- [ ] Generate IntelliJ run configurations
  ```powershell
  .\gradlew genIntellijRuns
  ```
- [ ] Open project in IntelliJ
- [ ] Wait for indexing to complete
- [ ] Verify no import errors

**For VS Code:**
- [ ] Install Java Extension Pack
- [ ] Open project folder
- [ ] Wait for Java language server to start
- [ ] Check "Problems" panel for errors

**For Eclipse:**
- [ ] Generate Eclipse run configurations
  ```powershell
  .\gradlew genEclipseRuns
  ```
- [ ] Import as existing Gradle project

## 🧪 Testing Steps

### 1. Development Environment Test

- [ ] Run Minecraft client
  ```powershell
  .\gradlew runClient
  ```

- [ ] Watch console for mod loading messages
- [ ] Look for: "Initializing LLM Conversations for Minecolonies"
- [ ] Check for any error messages

### 2. Configuration Test

- [ ] Close Minecraft if running
- [ ] Navigate to `run/config/`
- [ ] Verify `llm-conversations-client.toml` exists
- [ ] Open config file in text editor
- [ ] Add your OpenRouter API key:
  ```toml
  api_key = "sk-or-v1-..."
  ```
- [ ] Save config file

### 3. In-Game Testing

**Basic Conversation:**
- [ ] Run Minecraft client again
- [ ] Create or load a world with Minecolonies
- [ ] Find any citizen
- [ ] Hold Shift + Right-click the citizen
- [ ] Verify message: "Started conversation with [Name]"
- [ ] Type a message in chat: "Hello!"
- [ ] Wait for AI response (1-5 seconds)
- [ ] Verify response appears with citizen's name
- [ ] Type "goodbye"
- [ ] Verify conversation ends

**Stat Freezing:**
- [ ] Start conversation with a citizen
- [ ] Note their hunger/saturation level
- [ ] Wait 30 seconds during conversation
- [ ] Verify stats don't decrease
- [ ] End conversation
- [ ] Verify stats resume normal behavior

**Memory System:**
- [ ] Talk to a citizen about a specific topic
- [ ] End conversation
- [ ] Start new conversation with same citizen
- [ ] Mention previous topic
- [ ] Verify citizen references past conversation

**Multiple Conversations:**
- [ ] Talk to first citizen
- [ ] Try to talk to second citizen
- [ ] Verify you can only talk to one at a time
- [ ] End first conversation
- [ ] Start conversation with second citizen
- [ ] Verify it works correctly

### 4. Error Handling Tests

**Missing API Key:**
- [ ] Remove API key from config
- [ ] Restart Minecraft
- [ ] Try starting conversation
- [ ] Verify error message about missing API key

**Invalid API Key:**
- [ ] Set invalid API key
- [ ] Try having conversation
- [ ] Verify appropriate error message

**Network Error:**
- [ ] Disconnect internet
- [ ] Try having conversation
- [ ] Verify timeout/network error message
- [ ] Reconnect internet

**Busy Citizen:**
- [ ] Find a citizen doing important task
- [ ] Try to start conversation
- [ ] Verify "too busy" message if applicable

### 5. Persistence Tests

**Save/Load:**
- [ ] Start conversation with citizen
- [ ] Exchange several messages
- [ ] Save and quit world
- [ ] Load world again
- [ ] Verify conversation history saved
- [ ] Start new conversation with same citizen
- [ ] Verify memory of previous conversation

### 6. Configuration Tests

**Different Models:**
- [ ] Try different models in config:
  - [ ] `anthropic/claude-3-haiku`
  - [ ] `openai/gpt-4o-mini`
  - [ ] `meta-llama/llama-3.1-8b-instruct`
- [ ] Verify each works correctly

**Custom System Prompt:**
- [ ] Edit system_prompt in config
- [ ] Change citizen personality
- [ ] Verify responses match new personality

**Token Limits:**
- [ ] Set max_tokens to 50
- [ ] Verify shorter responses
- [ ] Set max_tokens to 300
- [ ] Verify longer responses

## 🐛 Common Issues Checklist

If build fails:
- [ ] Check JDK version is 21
- [ ] Check internet connection
- [ ] Try `.\gradlew clean build`
- [ ] Check Minecolonies version in build.gradle
- [ ] Review error logs

If mod doesn't load:
- [ ] Check neoforge.mods.toml syntax
- [ ] Verify NeoForge version compatibility
- [ ] Check Minecolonies is installed
- [ ] Review latest.log in run/logs/

If conversations don't work:
- [ ] Verify API key is set correctly
- [ ] Check internet connection
- [ ] Test API key on OpenRouter website
- [ ] Check console for error messages
- [ ] Verify Minecolonies citizens exist

## 📦 Release Checklist

When ready to release:

**Pre-Release:**
- [ ] All tests passing
- [ ] No console errors
- [ ] Documentation complete
- [ ] README.md has screenshots
- [ ] Version number updated

**Build Release:**
- [ ] Clean build: `.\gradlew clean`
- [ ] Release build: `.\gradlew build`
- [ ] Verify JAR in build/libs/
- [ ] Test JAR in clean Minecraft install

**Documentation:**
- [ ] README.md complete
- [ ] QUICKSTART.md clear
- [ ] Changelog created
- [ ] Screenshots/video prepared
- [ ] License file added

**Distribution:**
- [ ] Upload to CurseForge
- [ ] Upload to Modrinth
- [ ] Create GitHub release
- [ ] Announce on Discord/Reddit
- [ ] Update project links

## 🎯 Performance Checklist

- [ ] Test with 10+ citizens in colony
- [ ] Verify no lag during conversations
- [ ] Check memory usage (F3 debug screen)
- [ ] Test on dedicated server
- [ ] Verify multiplayer compatibility

## 🔒 Security Checklist

- [ ] API keys not hardcoded
- [ ] Config file has proper permissions
- [ ] No sensitive data logged
- [ ] API errors handled gracefully
- [ ] Input validation on chat messages

## ✅ Final Checks

Before considering complete:
- [ ] All core features working
- [ ] No critical bugs
- [ ] Performance acceptable
- [ ] Documentation complete
- [ ] User experience smooth
- [ ] Ready for user feedback

---

## 📝 Notes Section

Use this space to track issues, ideas, or observations during testing:

```
Date: ___________
Issue: 
Resolution:

Date: ___________
Idea:
Priority:

Date: ___________
Test Result:
Notes:
```

---

**When all boxes are checked, your mod is ready! 🎉**
