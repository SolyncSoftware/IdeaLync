import logging
import os
import sys
from typing import Never
import dotenv

from idealync import Bot

dotenv.load_dotenv()

logging.basicConfig(
    level=logging.INFO,
    format="[{levelname}] on {asctime} at {name}: {message}",
    style="{",
    datefmt="%m-%d %H-%M-%S",
    handlers=[
        logging.FileHandler("app.log"),
        logging.StreamHandler(),
    ],
)

def required_env_variable_error(env_var: str) -> Never:
    sys.exit(
        f"error! {env_var} is not optional\n"
        "please set it in environment variables"
    )

def main() -> None:
    role_channel_id = int(
        os.environ.get("ROLE_CHANNEL_ID")
        or required_env_variable_error("ROLE_CHANNEL_ID")
    )

    member_role_id = int(
        os.environ.get("MEMBER_ID")
        or required_env_variable_error("MEMBER_ID")
    )

    observer_role_id = int(
        os.environ.get("OBSERVER_ID")
        or required_env_variable_error("OBSERVER_ID")
    )

    meeting_voice_channel_id = int(
        os.environ.get("MEETING_VOICE_CHANNEL_ID")
        or required_env_variable_error("MEETING_VOICE_CHANNEL_ID")
    )

    token = (
        os.environ.get("APP_DISCORD_TOKEN")
        or required_env_variable_error("APP_DISCORD_TOKEN")
    )

    idea_board_forum_id = int(
        os.environ.get("IDEA_BOARD_FORUM_ID")
        or required_env_variable_error("IDEA_BOARD_FORUM_ID")
    )

    project_board_forum_id = int(
        os.environ.get("PROJECT_BOARD_FORUM_ID")
        or required_env_variable_error("PROJECT_BOARD_FORUM_ID")
    )

    bot = Bot(
        role_channel_id,
        member_role_id,
        observer_role_id,
        meeting_voice_channel_id,
        idea_board_forum_id,
        project_board_forum_id
    )
    bot.run(token)


if __name__ == "__main__":
    main()
