import argparse
import os
import time
from typing import Any, Dict, Tuple
import requests
from flask import Flask, jsonify, request
from flask_cors import CORS
from dotenv import load_dotenv
import json


load_dotenv()

def create_app() -> Flask:
    app = Flask(__name__)
    CORS(app)

    def _send_telegram_message(message: str, chat_id: str) -> Tuple[Dict[str, Any], int]:
        bot_token = os.getenv("TELEGRAM_BOT_TOKEN")
        if not bot_token:
            return {"error": "server misconfiguration: TELEGRAM_BOT_TOKEN not set"}, 500

        url = f"https://api.telegram.org/bot{bot_token}/sendMessage"
        payload = {"chat_id": chat_id, "text": message}

        try:
            resp = requests.post(url, json=payload, timeout=10)
        except requests.RequestException as exc:
            return {"error": "failed to contact Telegram API", "details": str(exc)}, 502

        if resp.status_code != 200:
            try:
                details = resp.json()
            except ValueError:
                details = {"status_code": resp.status_code, "text": resp.text}
            return {"error": "telegram_api_error", "details": details}, 502

        try:
            result = resp.json()
        except ValueError:
            result = {"status": "ok", "note": "telegram returned non-json response"}

        return {"status": "message sent", "telegram": result}, 200
    
    def _get_telegram_updates() -> Tuple[Dict[str, Any], int]:
        bot_token = os.getenv("TELEGRAM_BOT_TOKEN")
        if not bot_token:
            return {"error": "server misconfiguration: TELEGRAM_BOT_TOKEN not set"}, 500
        
        url = f"https://api.telegram.org/bot{bot_token}/getUpdates"

        try:
            resp = requests.get(url, timeout=10)
        except requests.RequestException as exc:
            return {"error": "failed to contact Telegram API", "details": str(exc)}, 502
        
        if resp.status_code != 200:
            try:
                details = resp.json()
            except ValueError:
                details = {"status_code": resp.status_code, "text": resp.text}
            return {"error": "telegram_api_error", "details": details}, 502

        try:
            result = resp.json()
        except ValueError:
            result = {"status": "ok", "note": "telegram returned non-json response"}

        return {"status": "message sent", "telegram": result}, 200

    @app.get("/ping")
    def ping():
        return jsonify({"message": "pong", "ts_ms": int(time.time() * 1000)})

    @app.post("/im-in-danger")
    def send_text():
        data = request.get_json(silent=True)
        if data is None or "username" not in data:
            return jsonify({"error": "expected JSON body with 'username' field"}), 400
        username = data["username"]

        message = f"{data['username']} is in danger!"

        payload = {}
        status = 200

        for contact in user_contacts.get(username, []):
            chat_id = contact_to_chat_id.get(contact, None)
            if chat_id is None:
                continue
            
            payload, status = _send_telegram_message(message, chat_id)

            if status != 200:
                return payload, status

        return jsonify(payload), status

    @app.post("/add-contact")
    def add_contact():
        data = request.get_json(silent=True)
        if data is None or "username" not in data or "contact_name" not in data:
            return jsonify({"error": "expected JSON body with 'username' and 'contact_name' fields"}), 400
        username = data["username"]
        contact_name = data["contact_name"]

        if contact_name not in contact_to_chat_id:
            return jsonify({"error": "unknown contact name"}), 400
        
        if username not in user_contacts:
            user_contacts[username] = []
        
        user_contacts[username].append(contact_name)

        try:
            with open(contacts_path, "w", encoding="utf-8") as fh:
                json.dump(user_contacts, fh, indent=4)
        except OSError:
            pass
        
        return jsonify({"status": "contact added"}), 200

    return app

def _parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="WatchHer Flask backend")

    parser.add_argument("--host", default="localhost", help="Bind address (default: %(default)s)")
    parser.add_argument("--port", type=int, default=8080, help="Bind port (default: %(default)s)")

    parser.add_argument("--debug", action="store_true", help="Enable debug mode")

    args = parser.parse_args()

    if not args.debug:
        args.debug = False
    return args

if __name__ == "__main__":
    contact_chat_id_path = os.path.join(os.path.dirname(__file__), "contact_to_chat_id.json")
    
    contact_to_chat_id = {}

    try:
        if os.path.exists(contact_chat_id_path):
            with open(contact_chat_id_path, "r", encoding="utf-8") as fh:
                data = json.load(fh)
                if isinstance(data, dict):
                    contact_to_chat_id = {
                        k: str(v) for k, v in data.items()
                    }
                else:
                    contact_to_chat_id = {}
        else:
            contact_to_chat_id = {}
    except (OSError, json.JSONDecodeError, ValueError):
        contact_to_chat_id = {}

    contacts_path = os.path.join(os.path.dirname(__file__), "contacts.json")

    user_contacts = {}
    try:
        if os.path.exists(contacts_path):
            with open(contacts_path, "r", encoding="utf-8") as fh:
                data = json.load(fh)
                if isinstance(data, dict):
                    user_contacts = {
                        k: (v if isinstance(v, list) else [v]) for k, v in data.items()
                    }
                else:
                    user_contacts = {}
        else:
            user_contacts = {}
    except (OSError, json.JSONDecodeError, ValueError):
        user_contacts = {}
    
    args = _parse_args()

    # Expose a WSGI app for hosting (servers can import `backend.main:app`)
    app = create_app()

    # print(get_list_of_users())

    app.run(host=args.host, port=args.port, debug=args.debug)

# def get_list_of_users() -> Tuple[Dict[str, Any], int]:
#         bot_token = os.getenv("TELEGRAM_BOT_TOKEN")
#         if not bot_token:
#             return {"error": "server misconfiguration: TELEGRAM_BOT_TOKEN not set"}, 500
        
#         url = f"https://api.telegram.org/bot{bot_token}/getUpdates"

#         try:
#             resp = requests.get(url, timeout=10)
#         except requests.RequestException as exc:
#             return {"error": "failed to contact Telegram API", "details": str(exc)}, 502
        
#         if resp.status_code != 200:
#             try:
#                 details = resp.json()
#             except ValueError:
#                 details = {"status_code": resp.status_code, "text": resp.text}
#             return {"error": "telegram_api_error", "details": details}, 502

#         try:
#             result = resp.json()
#         except ValueError:
#             result = {"status": "ok", "note": "telegram returned non-json response"}

#         return {"status": "message sent", "telegram": result}, 200

# print(get_list_of_users())
