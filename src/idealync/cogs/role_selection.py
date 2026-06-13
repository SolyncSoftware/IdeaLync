import logging

import discord
from discord.ext import commands

from idealync import Bot

from typing import Any;

logger = logging.getLogger(__name__)


class RoleSelect(discord.ui.Select[Any]):
    def __init__(self, bot: Bot) -> None:
        self.bot = bot
        options = [
            discord.SelectOption(label="Member", value="member", description="Members are also known as active contributors"),
            discord.SelectOption(label="Observer/Inactive", value="observer", description="Observer/Inactive are Members who are inactive for 1+ months and are unable to contribute for now"),
        ]
        super().__init__(
            placeholder="Choose your role",
            min_values=1,
            max_values=1,
            options=options,
        )

    async def callback(self, interaction: discord.Interaction) -> None:
        if interaction.guild is None:
            await interaction.response.send_message("this needs to be used in a server", ephemeral=True)
            return

        member = interaction.user
        if not isinstance(member, discord.Member):
            return

        target_role_id = (
            self.bot.config.member_role_id
            if self.values[0] == "member"
            else self.bot.config.observer_role_id
        )
        other_role_id = (
            self.bot.config.observer_role_id
            if self.values[0] == "member"
            else self.bot.config.member_role_id
        )

        target_role = interaction.guild.get_role(target_role_id)
        other_role = interaction.guild.get_role(other_role_id)

        if target_role is None:
            await interaction.response.send_message("That role is not available right now.", ephemeral=True)
            return

        if other_role is not None and other_role in member.roles:
            await member.remove_roles(other_role, reason="Switched role selection")

        if target_role not in member.roles:
            await member.add_roles(target_role, reason="Selected role from prompt")

        await interaction.response.send_message(
            f"You are now set as {target_role.name}.",
            ephemeral=True,
        )


class RoleSelection(commands.Cog):
    def __init__(self, bot: Bot) -> None:
        self.bot = bot
        self.role_message_id: int | None = None

    async def _find_existing_prompt(self, channel: discord.TextChannel) -> int | None:
        async for message in channel.history(limit=20):
            if message.author == self.bot.user and any(embed.title == "Choose your role" for embed in message.embeds):
                return message.id
        return None

    async def _ensure_role_prompt(self) -> None:
        channel = self.bot.get_channel(self.bot.config.role_channel_id)
        if not isinstance(channel, discord.TextChannel):
            logger.warning("Role channel %s is not a text channel.", self.bot.config.role_channel_id)
            return

        existing_message_id = await self._find_existing_prompt(channel)
        if existing_message_id is not None:
            self.role_message_id = existing_message_id
            return

        embed = discord.Embed(
            title="Choose your role",
            description="Use the dropdown below to switch between Member and Observer/Inactive.",
            color=discord.Color(0xF36647),
        )

        view = discord.ui.View()
        view.add_item(RoleSelect(self.bot))

        message = await channel.send(embed=embed, view=view)

        self.role_message_id = message.id
        logger.info("Posted role-selection prompt in channel %s", channel.id)

    @commands.Cog.listener()
    async def on_ready(self) -> None:
        await self._ensure_role_prompt()



async def setup(bot: Bot) -> None:
    await bot.add_cog(RoleSelection(bot))
