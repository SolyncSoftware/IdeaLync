FROM ghcr.io/astral-sh/uv:python3.12-bookworm-slim AS base

WORKDIR /app

# Enable bytecode compilation and use standard copies for virtualenv dependencies
ENV UV_COMPILE_BYTECODE=1 \
    UV_LINK_MODE=copy

# 1. Install dependencies first (cached layer unless lock file/pyproject changes)
RUN --mount=type=cache,target=/root/.cache/uv \
    --mount=type=bind,source=uv.lock,target=uv.lock \
    --mount=type=bind,source=pyproject.toml,target=pyproject.toml \
    uv sync --frozen --no-install-project --no-dev

# 2. Copy application source code
COPY . .

# 3. Build and install the project package itself
RUN --mount=type=cache,target=/root/.cache/uv \
    uv sync --frozen --no-dev

# Place virtual environment binaries in PATH
ENV PATH="/app/.venv/bin:$PATH"

CMD ["uv", "run", "idealync"]