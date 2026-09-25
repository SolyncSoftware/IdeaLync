# IdeaLync - internal bot for Solync

## Features

- Pitch Creator
  1. Posts a fillable form in the specified channel for pitch submissions
  2. Once submitted, pitches are then posted in a configured forum channel, where pitches are stored (#pitching-board)
- Meet Scheduler
  1. Use ``/schedule`` command to open a modal for meeting information
  2. Posts in the configured channel once submitted
- Role Selection
  1. Posts a selection in the configured channel.
  2. Select the specified roles defined in the config
- Pitch Forwarding
  1. Use ``/forward`` to forward pitches from the #pitching-board to the #project-board (configurable channels)
  2. Add a message to the forwarded pitch so members can get clarification

## Getting Started

To begin development and testing locally, please follow these steps in your terminal of choice:

1. Clone the repo by running `git clone https://github.com/SolyncSoftware/IdeaLync.git`.
2. Go inside the newly cloned folder (`cd IdeaLync`).
3. Configure `.env` by populating the values in `.env_example`.
4. Install JDK 25.
5. Now run `./gradlew run` to run the bot.
6. See the bot come to life! :)

### Nix

1. (optional) Update deps: `nix build .#idealync.mitmCache.updateScript --print-out-paths`
2. `nix build .` to build, **or**
3. Enter a shell `nix shell .` and run `IdeaLync`

## Goals

- Improve our workflow internally
- Open source
- Internal initiative-driven tasks with a [Kanban Board](https://github.com/orgs/SolyncSoftware/projects/8).

## Resources

- [Kord dokka docs](https://dokka.kord.dev/)
- [Kord wiki](https://github.com/kordlib/kord/wiki)
- [Discord docs](https://docs.discord.com/developers/intro)
- [Kotlin docs](https://kotlinlang.org/docs/getting-started.html)
- [Solync Community Discord](https://discord.com/invite/nUeRyRtDYC)
