# RealCraftBridge Project

## Disclaimer: Almost every text/interaction with the user is in French
## 2nd disclaimer: This is a temp project, I don't plan to maintain it a lot, so ABSOLUTLY not production-ready!

A Minecraft-Discord integration system that allows communication between a Minecraft server and a Discord bot.

## Project Overview

This project consists of two main components:

1. **Minecraft Plugin** - A Paper/Spigot plugin that creates an HTTP API to expose Minecraft server data
2. **Discord Bot** - A Python-based Discord bot that communicates with the Minecraft server

## Features

### Minecraft Plugin
- HTTP API for accessing player and server information
- Automatic API key generation for security
- Server status notifications via Discord webhook
- Command handling system

### Discord Bot
- Player information commands
- Server status commands
- In-game messaging from Discord

## Commands

### Discord Bot Commands

#### Player Commands
- `/player infos <player>` - Get detailed information about a player (health, experience, armor, etc.)
- `/player advancements <player>` - List all completed advancements for a player [NOT WORKING RN]. I'm working on a fix for this

#### Basic Commands
- `/print <message>` - Send a message to the Minecraft server chat
- `/player-list` - Show currently online players
- `/time` - Show current in-game time
- `/weather` - Show current in-game weather

## Setup Instructions

### Minecraft Plugin
1. Build the plugin with Gradle:
   ```
   cd mc-plugin
   ./gradlew build
   ```
2. Copy the generated JAR from `build/libs/RealCraftBridge-1.0-SNAPSHOT.jar` to your server's `plugins` folder
3. Set the correct environment variables yourserverdir/.env:
    - `DISCORD_WEBHOOK_URL` - The webhook to send the status of the server (ex. \[STATUS\] Serveur allumé)
4 Start your Minecraft server
5. An API key will be generated at the server root directory in `.api_key`

### Discord Bot
1. Create a .env file in the discord-bot directory (use .env.example as a template)
2. Set the required environment variables:
   - `MC_API` - Copy from `.api_key` in your Minecraft server
   - `MC_HTTP_ADDRESS` - URL to your Minecraft server's HTTP API (default: `http://localhost:8090`)
   - `DISCORD_TOKEN` - Your Discord bot token
   - `GUILD_ID` - Your Discord server ID
3. Install dependencies:
   ```
   pip install discord.py python-dotenv aiohttp
   ```
4. Run the bot:
   ```
   python bot.py
   ```

## Requirements

### Minecraft Plugin
- Java 21+
- Paper/Spigot Minecraft server 1.21+

### Discord Bot
- Python 3.9+
- discord.py
- python-dotenv
- aiohttp

## License

This project is available under the Apache License 2.0.

## Contributors

- Lyam Zambaz
- Github Copilot for the readme