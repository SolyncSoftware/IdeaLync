import logging
import os
import sys
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


def main() -> None:
    role_channel_id = int(
        os.environ.get("ROLE_CHANNEL_ID")
        or sys.exit(
            "error! ROLE_CHANNEL_ID is not optional\n"
            "please set it in environment variables."
        )
    )

    member_role_id = int(
        os.environ.get("MEMBER_ID")
        or sys.exit(
            "error! MEMBER_ID is not optional\n"
            "please set it in environment variables."
        )
    )

    observer_role_id = int(
        os.environ.get("OBSERVER_ID")
        or sys.exit(
            "error! OBSERVER_ID is not optional\n"
            "please set it in environment variables."
        )
    )

    token = (
        os.environ.get("APP_DISCORD_TOKEN")
        or sys.exit(
            "error! APP_DISCORD_TOKEN is not optional\n"
            "please set it in environment variables."
        )
    )

    bot = Bot(role_channel_id, member_role_id, observer_role_id)
    bot.run(token)


if __name__ == "__main__":
    main()
