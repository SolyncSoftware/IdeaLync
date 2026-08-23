import typing

import discord
from discord.ext import commands
from discord import app_commands

import logging

from idealync import Bot

logger = logging.getLogger(__name__)

class _ForwardView(discord.ui.View):
    def __init__(self):
        super().__init__()
        self.confirmed = None

    @discord.ui.button(label="Confirm?", style=discord.ButtonStyle.green)
    async def confirm(
        self, interaction: discord.Interaction, _: discord.ui.Button # type: ignore
    ) -> None:
        self.confirmed = True

        # acknoledge the interaction, basically like a 204 no content
        await interaction.response.defer(ephemeral=True)

        self.stop()

    @discord.ui.button(label="Cancel", style=discord.ButtonStyle.red)
    async def cancel(self, interaction: discord.Interaction, _: discord.ui.Button): # type: ignore
        await interaction.response.send_message("Cancelling", ephemeral=True)
        self.confirmed = False
        self.stop()


class PitchCreator(commands.Cog):
    def __init__(self, bot: Bot) -> None:
        self.bot = bot

    @app_commands.command(
        name="forward",
        description="Forward a pitch from the pitching board to the project board.",
    )
    @app_commands.guild_only
    async def forward(
        self, interaction: discord.Interaction, added_message: typing.Optional[str]
    ) -> None:
        config = self.bot.config

        if (
            not isinstance(interaction.channel, discord.Thread)
            or interaction.channel.parent_id != config.pitching_board_forum_id
        ):
            await interaction.response.send_message(
                "please run this command in the pitching board!", ephemeral=True
            )
            return

        if interaction.guild is None:
            return  # impossible

        view = _ForwardView()
        await interaction.response.send_message(
            "Are you sure you want to forward this pitch to the project board?",
            ephemeral=True,
            view=view,
        )

        await view.wait()

        thread = interaction.channel
        applied_tags = [
        discord.Object(id=int(config.help_wanted_tag_id)),
        discord.Object(id=int(config.pending_tag_id)),
        ]

        if view.confirmed:
            msg = await thread.fetch_message(thread.id)

            await interaction.followup.send(
                "Forwarding your pitch to the project board...", ephemeral=True
            )
            project_forum = await interaction.guild.fetch_channel(
                config.project_board_forum_id
            )
            typing.cast(discord.ForumChannel, project_forum)

            formatted_msg = (
                f"{msg.content}\n{added_message}\n\n\\- Forwarded by IdeaLync."
                if added_message is not None
                else f"{msg.content}\n\n\\- Forwarded by IdeaLync."
            )

            try:
                # create_thread has content but pyright is tripping
                await project_forum.create_thread(  # type: ignore
                    name=thread.name,
                    content=formatted_msg,  # type: ignore
                    applied_tags=applied_tags, # type: ignore
                )
            except discord.errors.NotFound:
                await interaction.followup.send(
                    "something's not right, project board forum isn't found!"
                )
                raise

        else:
            await interaction.followup.send("nevermind...", ephemeral=True)


async def setup(bot: Bot) -> None:
    await bot.add_cog(PitchCreator(bot))
