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
    role_id = int(
        os.environ.get("IDEALYNC_ROLE_ID")
        or sys.exit(
            "error! IDEALYNC_ROLE_ID is not optional\n"
            "please set it in environment variables."
        )
    )

    token = (
        os.environ.get("IDEALYNC_DISCORD_TOKEN")
        or sys.exit(
            "error! IDEALYNC_DISCORD_TOKEN is not optional\n"
            "please set it in environment variables."
        )
    )

    bot = Bot(role_id)
    bot.run(token)


if __name__ == "__main__":
    main()
