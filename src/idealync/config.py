from dataclasses import dataclass


@dataclass
class IdeaLyncConfig:
    """General config for IdeaLync."""

    role_channel_id: int  # the channel for posting role selection
    member_role_id: int  # the member role id
    observer_role_id: int  # observer/inactive role id
    brainstorming_tag_id: int # for pitches 
    help_wanted_tag_id: int # for projects
    pending_tag_id: int # for projects
    meeting_voice_channel_id: int  # meeting voice channel id for meet scheduler
    meeting_announce_id: int # meeting announcement
    pitching_channel_id: int # channel where you can fill in pitches
    pitching_board_forum_id: int  # pitching board forum channel id
    project_board_forum_id: int  # project board forum channel id
