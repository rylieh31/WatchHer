# WatchHer Backend (Flask)

## Setup

From the repo root:

```bash
python -m venv .venv
source .venv/bin/activate
pip install -r backend/requirements.txt
```

## Run

```bash
python backend/main.py
```

Defaults:
- Host: `localhost`
- Port: `8080`
- Debug: enabled

Override via parameters:

```bash
# Debug
python backend/main.py --host localhost --port 8080 --debug

# No Debug (default)
python backend/main.py --host localhost --port 8080 --no-debug
```

## Example Requests

```bash
curl http://127.0.0.1:8080/health
curl http://127.0.0.1:8080/api/ping

curl -X POST http://127.0.0.1:8080/api/echo \
  -H 'Content-Type: application/json' \
  -d '{"hello": "world"}'
```
