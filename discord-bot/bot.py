from dotenv import load_dotenv
load_dotenv()
import discord
import os
from discord.ext import commands
from commands.basic import BasicCommands
from commands.player_infos import PlayerInfosCommands


intents = discord.Intents.default()
bot = commands.Bot(command_prefix="!", intents=intents)

@bot.event
async def on_ready():
    player_group = PlayerInfosCommands(bot)
    bot.tree.add_command(player_group, guild=discord.Object(id=int(os.getenv("GUILD_ID"))))
    basic_group = BasicCommands(bot)
    bot.tree.add_command(basic_group, guild=discord.Object(id=int(os.getenv("GUILD_ID"))))
    synced = await bot.tree.sync(guild=discord.Object(id=int(os.getenv("GUILD_ID"))))
    print(f"Synced {len(synced)} commands to guild {os.getenv('GUILD_ID')}")
    print(f"Logged in as {bot.user}")


bot.run(os.getenv("DISCORD_TOKEN"))
