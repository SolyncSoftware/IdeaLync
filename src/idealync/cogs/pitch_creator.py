import logging
from typing import cast

import discord
from discord import ui
from discord.ext import commands

from idealync import Bot

logger = logging.getLogger(__name__)

PITCH_GUIDELINES_TEXT = (
    "Add a title and description. Ping members to see if they're interested. "
    "Projects we want to move on "
    "can move into the project channel using `/forward`. *(think of pitching threads as preproduction, idea space)*\n\n"
    "**remember to think of this:**\n"
    "- What would this solve?\n"
    "- Why now?\n"
    "- Will anything change if we don't follow through?\n"
    "- Scope of project\n"
    "- Will it block or help with other work?"
)


class PitchSubmitModal(ui.Modal, title="Submit a Pitch"):
    def __init__(self, bot: Bot):
        super().__init__()
        self.bot = bot

    pitch_title = ui.Label(
        text="Title",
        component=ui.TextInput(
            placeholder="Title for your idea",
            max_length=45,
            required=True,
        ),
    )

    pitch_description = ui.Label(
        text="Description",
        component=ui.TextInput(
            style=discord.TextStyle.paragraph,
            placeholder="Explain the idea, problem solved, scope, and impact...",
            required=True,
        ),
    )

    user_select = ui.Label(
        text="Recruit members (this will ping them)",
        component=ui.UserSelect(
            max_values=25,
            min_values=0,
            placeholder="check members to ping for feedback",
        ),
    )

    async def on_submit(self, interaction: discord.Interaction) -> None:
        if interaction.guild is None:
            return

        title_val = cast(ui.TextInput, self.pitch_title.component).value
        desc_val = cast(ui.TextInput, self.pitch_description.component).value
        attendees = cast(ui.UserSelect, self.user_select.component).values

        forum = await interaction.guild.fetch_channel(
            self.bot.config.pitching_board_forum_id
        )
        if not isinstance(forum, discord.ForumChannel):
            await interaction.response.send_message(
                "Configured pitching board forum channel is invalid.", ephemeral=True
            )
            return

        applied_tags = [discord.Object(id=self.bot.config.brainstorming_tag_id)]

        # this is so the same members are pinged in the forwarded channel
        ping_mentions = (
            " ".join([f"<@{m.id}>" for m in attendees]) if attendees else "None"
        )

        content = (
            f"**Submitted by:** {interaction.user.mention}\n"
            f"**Members recruited:** {ping_mentions}\n\n"
            f"### Description\n{desc_val}\n\n"
            f"Remember to use `/forward` once you're done brainstorming!"
        )

        try:
            thread_with_msg = await forum.create_thread(
                name=title_val,
                content=content,
                applied_tags=applied_tags,
            )

            if attendees:
                pings_str = " ".join([f"<@{m.id}>" for m in attendees])
                await thread_with_msg.thread.send(
                    content=f"you have been recruited for this pitch! \n{pings_str}"
                )

            await interaction.response.send_message(
                f"your pitch **{title_val}** was posted to <#{forum.id}>",
                ephemeral=True,
            )

        except discord.HTTPException as e:
            logger.error(f"Failed to create pitch thread: {e}")
            await interaction.response.send_message(
                "Failed to post pitch to the forum board.", ephemeral=True
            )


class PitchPromptView(ui.View):
    def __init__(self, bot: Bot):
        super().__init__(timeout=None)  # Persistent view
        self.bot = bot

    @ui.button(
        label="Submit Pitch",
        style=discord.ButtonStyle.primary,
        custom_id="idealync:submit_pitch_button",
        emoji="💡",
    )
    async def open_modal(
        self, interaction: discord.Interaction, _: ui.Button
    ) -> None:
        await interaction.response.send_modal(PitchSubmitModal(self.bot))


class PitchSubmission(commands.Cog):
    def __init__(self, bot: Bot) -> None:
        self.bot = bot

    async def cog_load(self) -> None:
        self.bot.add_view(PitchPromptView(self.bot))

    async def _find_existing_prompt(self, channel: discord.TextChannel) -> int | None:
            async for message in channel.history(limit=20):
                if message.author == self.bot.user and any(
                    embed.title == "Pitch an idea!" for embed in message.embeds
                ):
                    return message.id
            return None
    

    async def _send_pitch_prompt(self) -> None:
            channel = self.bot.get_channel(self.bot.config.pitching_channel_id)
            if not isinstance(channel, discord.TextChannel):
                logger.warning(
                    "Pitching channel %s is not a text channel.",
                    self.bot.config.pitching_channel_id,
                )
                return
    
            existing_message_id = await self._find_existing_prompt(channel)
            if existing_message_id is not None:
                self.role_message_id = existing_message_id
                return
    
            embed = discord.Embed(
                title="Pitch an idea!",
                description=PITCH_GUIDELINES_TEXT,
                color=discord.Color(0xF36647),
            )
    
            message = await channel.send(embed=embed, view=PitchPromptView(self.bot))
    
            self.role_message_id = message.id
            logger.info("Posted pitching prompt in channel %s", channel.id)

    @commands.Cog.listener()
    async def on_ready(self) -> None:
            await self._send_pitch_prompt()


async def setup(bot: Bot) -> None:
    await bot.add_cog(PitchSubmission(bot))