import logging
import os
import discord
from .config import IdeaLyncConfig

from discord.ext import commands

intents = discord.Intents.default()
intents.members = True
intents.message_content = False

class Bot(commands.Bot):
    def __init__(
        self,
        role_channel_id: int,
        member_role_id: int,
        observer_role_id: int,
        meeting_voice_channel_id: int,
    ) -> None:
        # todo, make this shit dynamic
        super().__init__(intents=intents, command_prefix="unused")
        self.config = IdeaLyncConfig(
            role_channel_id=role_channel_id,
            member_role_id=member_role_id,
            observer_role_id=observer_role_id,
            meeting_voice_channel_id=meeting_voice_channel_id,
        )

    async def setup_hook(self) -> None:
        cogs_dir = os.path.join(os.path.dirname(__file__), "cogs")
        for filename in os.listdir(cogs_dir):
            if filename.endswith(".py"):
                await self.load_extension(f"idealync.cogs.{filename[:-3]}")
                logging.info("loaded cog: %s", filename[:-3])

        synced = await self.tree.sync()
        logging.info("synced %d commands", len(synced))
