import discord
from discord import app_commands
from discord.ext import commands
from mc_bridge import MinecraftBridge
import datetime

class BasicCommands(app_commands.Group):
    def __init__(self, bot):
        super().__init__()
        self.bot = bot
        self.cooldown_users = {}

    @app_commands.command(name="print", description="Affiche un message dans le chat Minecraft")
    async def print(self, interaction: discord.Interaction, message: str):
        user_id = interaction.user.id
        if user_id in self.cooldown_users:
            await interaction.response.send_message("⏳ Attendez 1 minute avant de réessayer.", ephemeral=True)
            return

        self.cooldown_users[user_id] = True
        await MinecraftBridge.query(f"print {message}")
        await interaction.response.send_message("✅ Message envoyé.")
        await self.bot.loop.create_task(self.remove_timeout(user_id))
    
    async def remove_timeout(self, user_id):
        await discord.utils.sleep_until(datetime.datetime.utcnow() + datetime.timedelta(minutes=1))
        self.cooldown_users.pop(user_id, None)
        self.cooldown_users.pop(user_id, None)

    @app_commands.command(name="player-list", description="Liste des joueurs connectés")
    async def player_list(self, interaction: discord.Interaction):
        data = await MinecraftBridge.query("player-list")
        for data in data["players"]:
            if isinstance(data, dict):
                data = ", ".join(f"{k}: {v}" for k, v in data.items())
            else:
                data = str(data)
        await interaction.response.send_message(f"👥 {data}")

    @app_commands.command(name="time", description="Heure actuelle dans le monde Minecraft")
    async def time(self, interaction: discord.Interaction):
        data = await MinecraftBridge.query("time")
        await interaction.response.send_message(f"🕐 {data["time"]}")

    @app_commands.command(name="weather", description="Météo actuelle")
    async def weather(self, interaction: discord.Interaction):
        data = await MinecraftBridge.query("weather")
        emoji = {"Clair": "☀️", "Pluie": "🌧️", "Orage": "⛈️"}.get(data["weather"], "")
        await interaction.response.send_message(f"{emoji} {data["weather"]}")
