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
- Debug: disabled

Override via parameters:

```bash
# Debug
python backend/main.py --host localhost --port 8080 --debug
```

## Example Requests

```bash
curl http://localhost:8080/health
curl http://localhost:8080/api/ping

curl -X POST http://localhost:8080/api/echo \
  -H 'Content-Type: application/json' \
  -d '{"hello": "world"}'
```

## References

- [Scikit-Learn Documentation](https://scikit-learn.org/stable/documentation.html)
- [Scikit-Learn Tutorial](https://www.geeksforgeeks.org/machine-learning/scikit-learn-tutorial/)
- [Random Forrest Tutorial](https://www.geeksforgeeks.org/machine-learning/random-forest-algorithm-in-machine-learning/)
