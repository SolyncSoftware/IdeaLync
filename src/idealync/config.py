from dataclasses import dataclass

@dataclass
class IdeaLyncConfig:
    """General config for IdeaLync."""

    role_channel_id: int # the channel for posting role selection
    member_role_id: int # the member role id
    observer_role_id: int # observer/inactive role id
    meeting_voice_channel_id: int # meeting voice channel id for meet scheduler
    idea_board_forum_id: int # idea board forum channel id
    project_board_forum_id: int # project board forum channel id
