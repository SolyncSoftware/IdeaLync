package org.solync.idealync

import dev.kord.common.entity.Snowflake
import io.github.cdimascio.dotenv.Dotenv
import org.koin.mp.KoinPlatform

/**
 * General config for IdeaLync.
 *
 * @param roleChannelId the channel for posting role selection
 * @param memberRoleId the member role id
 * @param observerRoleId observer/inactive role id
 * @param brainstormingTagId for pitches
 * @param helpWantedTagId for projects
 * @param pendingTagId for projects
 * @param meetingVoiceChannelId meeting voice channel id for meet scheduler
 * @param meetingAnnounceId meeting announcement
 * @param pitchingChannelId channel where you can fill in pitches
 * @param pitchingBoardForumId pitching board forum channel id
 * @param projectBoardForumId project board forum channel id
 */
data class IdeaLyncConfig(
    val roleChannelId: Snowflake,
    val memberRoleId: Snowflake,
    val observerRoleId: Snowflake,
    val brainstormingTagId: Snowflake,
    val helpWantedTagId: Snowflake,
    val pendingTagId: Snowflake,
    val meetingVoiceChannelId: Snowflake,
    val meetingAnnounceId: Snowflake,
    val pitchingChannelId: Snowflake,
    val pitchingBoardForumId: Snowflake,
    val projectBoardForumId: Snowflake
)

fun Dotenv.getIdeaLyncConfig() = IdeaLyncConfig(
    roleChannelId = Snowflake(this["ROLE_CHANNEL_ID"].toLong()),
    memberRoleId = Snowflake(this["MEMBER_ID"].toLong()),
    observerRoleId = Snowflake(this["OBSERVER_ID"].toLong()),
    brainstormingTagId = Snowflake(this["BRAINSTORMING_TAG_ID"].toLong()),
    helpWantedTagId = Snowflake(this["HELP_WANTED_TAG_ID"].toLong()),
    pendingTagId = Snowflake(this["PENDING_TAG_ID"].toLong()),
    meetingVoiceChannelId = Snowflake(this["MEETING_VOICE_CHANNEL_ID"].toLong()),
    meetingAnnounceId = Snowflake(this["MEETING_ANNOUNCE_ID"].toLong()),
    pitchingChannelId = Snowflake(this["PITCHING_CHANNEL_ID"].toLong()),
    pitchingBoardForumId = Snowflake(this["PITCHING_BOARD_FORUM_ID"].toLong()),
    projectBoardForumId = Snowflake(this["PROJECT_BOARD_FORUM_ID"].toLong())
)

val ideaLyncConfig get() = KoinPlatform.getKoin().get<IdeaLyncConfig>()
