import discord
from discord import app_commands
from mc_bridge import MinecraftBridge
import json
from datetime import datetime

class PlayerInfosCommands(app_commands.Group):
    def __init__(self, bot):
        super().__init__(name="player", description="Commandes d'informations sur les joueurs")
        self.bot = bot

    def format_timestamp(self, ts):
        try:
            # Assume ts is a UNIX timestamp (int or str)
            ts = int(ts)/1000
            return datetime.fromtimestamp(ts).strftime("%Y-%m-%d %H:%M:%S")
        except Exception as e:
            print(f"Error formatting timestamp: {e}")
            return str(ts)

    @app_commands.command(name="infos", description="Infos d’un joueur")
    async def player_infos(self, interaction: discord.Interaction, player: str):
        fields = []

        keys = [
            ("get-exp", "Expérience", "🟢", lambda d: d["exp"]),
            ("get-health", "Santé", "❤️", lambda d: f"{round(d['health'], 1)}/20"),
            ("armor", "Armure", "🛡️", lambda d: f"{d['armor']}/20"),
            ("food", "Nourriture", "🍗", lambda d: f"{d['food_level']}/20"),
            ("first-join", "Première connexion", "📅", lambda d: self.format_timestamp(d["first_join"])),
            ("last-join", "Dernière connexion", "📅", lambda d: self.format_timestamp(d["last_join"])),
            ("ping", "Ping", "📶", lambda d: d["ping"]),
            ("online-since", "Connecté depuis", "⌛", lambda d: str(d["online_since"])+" secondes"),
        ]

        fields.append(f"**👤 Nom**: {player}\n")
        for cmd, label, icon, fun in keys:
            data = await MinecraftBridge.query(f"{cmd} {player}")
            fields.append(f"**{icon} {label}**: {fun(data)}")

        stats_data = await MinecraftBridge.query(f"stats {player}")
        # Format stats_data as pretty JSON or key-value pairs
        if isinstance(stats_data, dict):
            stats_lines = [f"{k}: {v}" for k, v in stats_data.items()]
            stats_str = "\n".join(stats_lines)
        else:
            stats_str = str(stats_data)
        fields.append(f"\n**📊 Stats**\n```\n{stats_str}\n```")

        await interaction.response.send_message("\n".join(fields))

    @app_commands.command(name="advancements", description="Liste des progrès d’un joueur")
    async def advancements(self, interaction: discord.Interaction, player: str):
        data = await MinecraftBridge.query(f"advancements {player}")
        progress = data["completed_advancements"]

        try:
            with open("adv_name_map.json", "r") as f:
                mapping = json.load(f)
        except Exception:
            mapping = {}

        output = []
        for adv_id in progress:
            name = mapping.get(adv_id, adv_id)
            output.append(f"✅ {name}")

        message = "\n".join(output) or "Aucun progrès."
        # Discord message limit is 2000 characters
        if len(message) > 2000:
            message = message[:1997] + "…"

        await interaction.response.send_message(message)
