"""
WatchHer backend (Flask).
"""

import argparse
import time
from flask import Flask, jsonify, request
from flask_cors import CORS


def create_app() -> Flask:
    app = Flask(__name__)
    CORS(app)

    @app.get("/health")
    def health():
        return jsonify({"status": "ok"})

    @app.get("/api/ping")
    def ping():
        return jsonify({"message": "pong", "ts_ms": int(time.time() * 1000)})

    @app.post("/api/echo")
    def echo():
        data = request.get_json(silent=True)
        if data is None:
            return jsonify({"error": "expected JSON body"}), 400
        return jsonify(data)

    return app


def _parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="WatchHer Flask backend")

    parser.add_argument("--host", default="localhost", help="Bind address (default: %(default)s)")
    parser.add_argument("--port", type=int, default=8080, help="Bind port (default: %(default)s)")

    debug_group = parser.add_mutually_exclusive_group()
    debug_group.add_argument("--debug", action="store_true", help="Enable debug mode")
    debug_group.add_argument("--no-debug", action="store_true", help="Disable debug mode")

    args = parser.parse_args()
    if args.debug:
        args.debug = True
    else:
        args.debug = False
    return args


# Expose a WSGI app for hosting (servers can import `backend.main:app`).
app = create_app()

if __name__ == "__main__":
    args = _parse_args()
    app.run(host=args.host, port=args.port, debug=args.debug)
