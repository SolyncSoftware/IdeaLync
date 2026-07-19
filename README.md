# IdeaLync - internal bot for Solync

## Features
- IdeaMaker/PitchCreator
  1. Posts a fillable form in the specified channel for idea posting
  2. Once submitted, ideas are then posted in a configured form channel, where ideas are stored (#idea-board)
- Meet Scheduler
  1.  Use ``/schedule`` command to open a modal for meeting information
  2.  Posts in the configured channel once submitted
- Role Selection
  1. Posts a selection in the configured channel.
  2. Select the specified roles defined in the config 

## Getting Started

> [!IMPORTANT]
> Please make sure to install [uv](https://github.com/astral-sh/uv). This project is optimized for uv.
> Also install [Ruff](https://docs.astral.sh/ruff/).

To begin development and testing locally, please follow these steps in your terminal of choice:

1. Install uv and Ruff (optional but recommended) first using a package manager
or by directly installing it from uv's [GitHub Releases](https://github.com/astral-sh/uv/releases/latest)
and Ruff's [GitHub Releases](https://github.com/astral-sh/ruff/releases/latest)

2. Clone the repo by running `git clone https://github.com/SolyncSoftware/IdeaLync.git`.
3. Go inside the newly cloned folder (`cd IdeaLync`).
4. Run `uv sync` to install the packages.
5. Great! Now configure `.env` by populating the values in `.env_example`.
6. Now run `uv run idealync` to run the bot.
7. See the bot come to life! :)

## Goals

- Improve our workflow internally
- Open source
- Internal initiative-driven tasks with a [Kanban Board](https://github.com/orgs/SolyncSoftware/projects/8).

## Resources

- [discord.py documentation](https://discordpy.readthedocs.io/)
- [uv Documentation](https://docs.astral.sh/uv/)
- [Python Documentation](https://docs.python.org/3/)
- [Solync Community Discord](https://discord.com/invite/nUeRyRtDYC)
