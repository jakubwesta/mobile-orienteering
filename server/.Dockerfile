FROM python:3.14-slim AS base

WORKDIR /app
ENV PYTHONUNBUFFERED=1
ENV VIRTUAL_ENV=/app/.venv
ENV PATH="$VIRTUAL_ENV/bin:$PATH"

COPY --from=ghcr.io/astral-sh/uv:latest /uv /usr/local/bin/uv
ENV UV_COMPILE_BYTECODE=1 UV_LINK_MODE=copy

# --- dependencies ---
FROM base AS dependencies
COPY pyproject.toml ./
RUN uv sync --no-dev

# --- production ---
FROM base AS production
COPY --from=dependencies --chown=app:app /app/.venv /app/.venv
COPY --chown=app:app . .
RUN uv pip install -e .
RUN groupadd -r app && useradd -r -g app app
USER app

EXPOSE 8000

HEALTHCHECK --interval=30s --timeout=30s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8000/api/health || exit 1

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
