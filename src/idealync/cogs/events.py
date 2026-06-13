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
        logger.info("Member %s joined", member.name)

    @commands.Cog.listener()
    async def on_member_update(self, before: discord.Member, after: discord.Member):
        logger.info("Member %s updated roles", after.name)
