import logging
import discord
from discord.ext import commands

from idealync import Bot

logger = logging.getLogger(__name__)

async def setup(bot: Bot) -> None:
    await bot.add_cog(Events(bot))

class Events(commands.Cog):
    def __init__(self, bot: Bot) -> None:
        self.bot = bot

    @commands.Cog.listener()
    async def on_member_join(self, member: discord.Member):
        if any(role.id == self.bot.config.role_id for role in member.roles):
            logger.info("Attempting to ban %s (joined with role).", member.name)
            await member.ban(reason="Banned user based on bot role.")
            logger.info("Banned %s", member.name)

    @commands.Cog.listener()
    async def on_member_update(self, before: discord.Member, after: discord.Member):
        role_id = self.bot.config.role_id
        if not any(role.id == role_id for role in before.roles) and any(role.id == role_id for role in after.roles):
            logger.info("Attempting to ban %s (role added).", after.name)
            await after.ban(reason="Banned user based on bot role.")
            logger.info("Banned %s", after.name)
