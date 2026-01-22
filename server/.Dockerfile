FROM python:3.14-slim

WORKDIR /app
COPY . .

RUN pip install --upgrade pip
RUN pip install uv
RUN pip install -e .

ENV PYTHONPATH=/app/src

EXPOSE 8000

HEALTHCHECK --interval=30s --timeout=30s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8000/api/health || exit 1

CMD ["uv", "run", "server"]
