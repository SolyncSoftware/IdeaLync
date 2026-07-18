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
    async def confirm(self, interaction: discord.Interaction, _: discord.ui.Button) -> None:
        self.confirmed = True

        # acknoledge the interaction, basically like a 204 no content
        await interaction.response.defer(ephemeral=True)

        self.stop()

    @discord.ui.button(label='Cancel', style=discord.ButtonStyle.red)
    async def cancel(self, interaction: discord.Interaction, _: discord.ui.Button):
        await interaction.response.send_message('Cancelling', ephemeral=True)
        self.confirmed = False
        self.stop()


class IdeaMaker(commands.Cog):
    def __init__(self, bot: Bot) -> None:
        self.bot = bot

    @app_commands.command(name="forward", description="Forward an idea from the idea board to the project board.")
    @app_commands.guild_only
    async def forward(self, interaction: discord.Interaction, added_message: typing.Optional[str]) -> None:
        config = self.bot.config

        if not isinstance(interaction.channel, discord.Thread) or interaction.channel.parent_id != config.idea_board_forum_id:
            await interaction.response.send_message("please run this command in the idea board!", ephemeral=True)
            return

        if interaction.guild is None:
            return # impossible

        view = _ForwardView()
        await interaction.response.send_message(
            "Are you sure you want to forward this idea to the project board?",
            ephemeral=True,
            view=view,
        )

        await view.wait()

        thread = interaction.channel

        if view.confirmed:
            msg = await thread.fetch_message(thread.id)

            await interaction.followup.send("Forwarding your idea to the project board...", ephemeral=True)
            project_forum = await interaction.guild.fetch_channel(config.project_board_forum_id)
            typing.cast(discord.ForumChannel, project_forum)

            # create_thread has content but pyright is tripping
            try:
                await project_forum.create_thread( # type: ignore
                    name=thread.name,
                    content=f"{msg.content}\n\n{added_message}\n\n\\- Forwarded by IdeaLync." # type: ignore
                )
            except discord.errors.NotFound:
                await interaction.followup.send("something's not right, project board forum isn't found!")
                raise

        else:
            await interaction.followup.send("nevermind...", ephemeral=True)

async def setup(bot: Bot) -> None:
    await bot.add_cog(IdeaMaker(bot))
