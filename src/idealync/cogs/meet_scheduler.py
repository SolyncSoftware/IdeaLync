import discord
from discord.ext import commands
from discord import PrivacyLevel, ui, app_commands

import logging
import dateparser

from idealync import Bot

from typing import cast

logger = logging.getLogger(__name__)


class MeetSchedulerModal(ui.Modal, title="Schedule a meeting..."):
    def __init__(self, bot: Bot):
        super().__init__()
        self.bot = bot

    # title conflicts with the      ^ title above
    name = ui.Label(text="Title", component=ui.TextInput())
    description = ui.Label(
        text="Description", component=ui.TextInput(style=discord.TextStyle.paragraph)
    )
    # discord only allows a max of 25 people in a user select
    user_select = ui.Label(
        text="Who's attending?", component=ui.UserSelect(max_values=25, min_values=2)
    )
    timestamp = ui.Label(text="Timestamp", component=ui.TextInput())

    async def on_submit(self, interaction: discord.Interaction) -> None:
        if interaction.guild is None:
            return  # unreachable

        # casting is needed because pyright complains about missing .value and .values
        # they are present at runtime
        name = cast(ui.TextInput, self.name.component).value
        description = cast(ui.TextInput, self.description.component).value
        attendees = cast(ui.UserSelect, self.user_select.component).values

        timestamp = dateparser.parse(
            cast(ui.TextInput, self.timestamp.component).value,
            settings={
                "PREFER_DATES_FROM": "future",
                "RETURN_AS_TIMEZONE_AWARE": True,
                # "TIMEZONE": "America/Chicago", # i think most of texas is central time
            },
            languages=["en"],
        )

        if timestamp is None:
            await interaction.response.send_message(
                "couldn't parse timestamp, did you spell something wrong?"
            )
            return

        channel_id = await interaction.guild.fetch_channel(
            self.bot.config.meeting_voice_channel_id
        )
        if not isinstance(channel_id, discord.VoiceChannel):
            await interaction.response.send_message(
                "MEETING_VOICE_CHANNEL_ID is not a voice channel."
            )
            return

        try:
            await interaction.guild.create_scheduled_event(
                name=name,
                start_time=timestamp,
                description=f"{description}\n\nAttending: {', '.join([f'<@{member.id}>' for member in attendees])}",
                channel=channel_id,
                privacy_level=PrivacyLevel.guild_only,
            )
            await interaction.response.send_message(
                f"event {name} has been created", ephemeral=True
            )
        except discord.Forbidden as e:
            await interaction.response.send_message(
                "bot doesn't have permissions to create an event"
            )
            raise e


class MeetScheduler(commands.Cog):
    def __init__(self, bot: Bot) -> None:
        self.bot = bot

    @app_commands.command(name="schedule", description="Schedule a meeting")
    @app_commands.guild_only
    async def schedule(self, interaction: discord.Interaction) -> None:
        await interaction.response.send_modal(MeetSchedulerModal(self.bot))


async def setup(bot: Bot) -> None:
    await bot.add_cog(MeetScheduler(bot))
