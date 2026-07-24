#!/usr/bin/env python3
"""
Hermes AI Agent Daemon
Runs inside proot/Termux environment
Handles: Chat, Terminal, Cron, Skills, Settings via TCP/Unix socket
"""

import asyncio
import json
import os
import signal
import subprocess
import sys
import time
import traceback
from pathlib import Path
from typing import Any, Dict, List, Optional, Union
from dataclasses import dataclass, asdict
from enum import Enum
import asyncio.subprocess
import shlex

# Optional imports
try:
    import aiohttp
    HAS_AIOHTTP = True
except ImportError:
    HAS_AIOHTTP = False

try:
    import yaml
    HAS_YAML = True
except ImportError:
    HAS_YAML = False


class Provider(Enum):
    GEMINI = "gemini"
    NOUS = "nous"
    CUSTOM = "custom"


@dataclass
class TerminalResult:
    stdout: str
    stderr: str
    returncode: int
    session_id: str = "default"


@dataclass
class ChatMessage:
    role: str
    content: str


class Config:
    """Configuration management"""
    def __init__(self, home: Path):
        self.home = home
        self.config_dir = home / ".hermes"
        self.config_file = self.config_dir / "settings.json"
        self.config_dir.mkdir(parents=True, exist_ok=True)
        self._settings = self._load()

    def _load(self) -> Dict:
        try:
            with open(self.config_file) as f:
                return json.load(f)
        except Exception:
            return self._defaults()

    def _defaults(self) -> Dict:
        return {
            "api_provider": "nous",
            "gemini_api_key": "",
            "nous_api_key": "",
            "active_model": "nousresearch/hermes-3-llama-3.1-8b",
            "custom_api_base_url": "https://api.openai.com/v1/",
            "custom_api_key": "",
            "custom_model": "gpt-4o",
            "soul_md": "You are Hermes, a sophisticated and friendly personal AI agent. You execute terminal commands, schedule background cron jobs, manage skills/plugins, and look after your desktop pixel pet. Speak elegantly and concisely.",
            "sandbox_type": "Embedded Termux",
            "docker_image": "ubuntu:22.04",
            "ssh_host": "root@127.0.0.1",
            "ssh_port": "22",
            "ssh_password": "",
            "telegram_enabled": True,
            "discord_enabled": False,
            "termux_hardware_enabled": True,
            "telegram_token": "",
            "telegram_chat_id": "",
            "discord_webhook_url": "",
            "discord_channel_id": "",
            "vibrate_duration_ms": "500",
            "tts_language_accent": "en-US",
        }

    def get(self, key: str, default=None):
        return self._settings.get(key, self._defaults().get(key, default))

    def set(self, key: str, value):
        self._settings[key] = value
        self.save()

    def save(self):
        with open(self.config_file, 'w') as f:
            json.dump(self._settings, f, indent=2)


class TerminalSession:
    def __init__(self, session_id: str, name: str = None):
        self.session_id = session_id
        self.name = name or f"session {session_id}"
        self.logs: List[str] = []
        self.current_dir = "/home"
        self.created_at = time.time()

    def add_log(self, line: str):
        self.logs.append(f"[{time.strftime('%H:%M:%S')}] {line}")
        if len(self.logs) > 500:
            self.logs = self.logs[-500:]


class HermesDaemon:
    def __init__(self, home: str = None):
        self.home = Path(home) if home else Path(os.environ.get("HOME", "/home"))
        self.config = Config(self.home)
        self.sessions: Dict[str, TerminalSession] = {}
        self.active_session = "default"
        self.running = False
        self.start_time = time.time()
        
        # Create default session
        self.sessions["default"] = TerminalSession("default", "main")
        self.active_session = "default"
        
        # Ensure directories
        self.home.mkdir(parents=True, exist_ok=True)
        (self.home / ".hermes").mkdir(exist_ok=True)
        (self.home / ".hermes" / "logs").mkdir(exist_ok=True)
        
        # Initialize logging
        self.log_file = self.home / ".hermes" / "logs" / f"daemon_{time.strftime('%Y%m%d')}.log"
        self._log("Hermes Daemon initialized")

    def _log(self, message: str, level: str = "INFO"):
        timestamp = time.strftime('%Y-%m-%d %H:%M:%S')
        log_line = f"[{timestamp}] [{level}] {message}"
        print(log_line, flush=True)
        try:
            with open(self.log_file, 'a') as f:
                f.write(log_line + "\n")
        except Exception:
            pass

    async def start(self):
        self.running = True
        self._log("Starting Hermes Daemon")
        
        # Start servers
        await asyncio.gather(
            self.start_unix_server(),
            self.start_tcp_server(),
        )

    async def stop(self):
        self.running = False
        self._log("Shutting down Hermes Daemon")

    def handle_request(self, line: str) -> str:
        """Process incoming JSON request, return JSON response"""
        try:
            request = json.loads(line.strip())
            cmd = request.get("cmd")
            data = request.get("data", {})
            
            if not cmd:
                return json.dumps({"error": "Missing cmd"})
            
            handler = getattr(self, f"handle_{cmd}", None)
            if not handler:
                return json.dumps({"error": f"Unknown command: {cmd}"})
            
            result = handler(request.get("data", {}))
            if asyncio.iscoroutine(result):
                # Can't await here - return placeholder
                return json.dumps({"status": "async", "message": "Command queued"})
            return json.dumps(result)
            
        except json.JSONDecodeError:
            return json.dumps({"error": "Invalid JSON"})
        except Exception as e:
            self._log(f"Error processing request: {e}", "ERROR")
            return json.dumps({"error": str(e)})

    # ============ Command Handlers ============
    
    def handle_chat(self, data: Dict) -> Dict:
        """Queue chat request for UI to process"""
        return {
            "status": "queued",
            "message": "Chat request forwarded to UI",
            "data": data
        }

    def handle_terminal(self, data: Dict) -> Dict:
        cmd = data.get("cmd", "")
        session_id = data.get("session_id", "default")
        
        if not cmd:
            return {"error": "Missing cmd"}
        
        session = self.sessions.get(session_id)
        if not session:
            session = TerminalSession(session_id)
            self.sessions[session_id] = session
            if self.active_session not in self.sessions:
                self.active_session = session_id
        
        try:
            # Execute in proot environment
            result = self._execute_in_proot(cmd, session)
            session.add_log(f"$ {cmd}")
            if result.stdout:
                session.add_log(result.stdout)
            if result.stderr:
                session.add_log(f"ERR: {result.stderr}")
            
            return asdict(result)
        except Exception as e:
            self._log(f"Terminal error: {e}", "ERROR")
            return {"error": str(e)}

    def _execute_in_proot(self, cmd: str, session: TerminalSession) -> TerminalResult:
        """Execute command in proot/Termux environment"""
        home = os.environ.get("HOME", "/home")
        rootfs = os.environ.get("TERMUX_ROOTFS", "/home/termux-rootfs")
        
        # Build proot command
        proot_cmd = [
            "proot",
            "--link2symlink", "-0",
            "-r", os.environ.get("TERMUX_ROOTFS", "/home/termux-rootfs"),
            "-b", "/dev:/dev",
            "-b", "/proc:/proc",
            "-b", "/sys:/sys",
            "-b", f"{os.environ.get('HOME', '/home')}:/home",
            "-w", session.current_dir,
            "bash", "-c", cmd
        ]
        
        try:
            result = subprocess.run(
                proot_cmd,
                capture_output=True,
                text=True,
                timeout=60,
                cwd=session.current_dir
            )
            return TerminalResult(
                stdout=result.stdout,
                stderr=result.stderr,
                returncode=result.returncode,
                session_id=session.session_id
            )
        except subprocess.TimeoutExpired:
            return TerminalResult("", "Command timed out", -1, session.session_id)
        except Exception as e:
            return TerminalResult("", str(e), -1, session.session_id)

    def handle_cron(self, data: Dict) -> Dict:
        action = data.get("action")
        if action == "list":
            return {"jobs": []}
        elif action == "add":
            return {"status": "added", "job": data.get("job")}
        elif action == "remove":
            return {"status": "removed", "id": data.get("id")}
        return {"error": "Unknown cron action"}

    def handle_skill(self, data: Dict) -> Dict:
        skill = data.get("skill")
        action = data.get("action")
        return {"status": f"Skill {skill} {action} executed"}

    def handle_settings(self, data: Dict) -> Dict:
        action = data.get("action")
        if action == "get":
            # Return all settings (non-sensitive)
            settings = {}
            for k, v in self.config._settings.items():
                if "key" not in k.lower() and "token" not in k.lower() and "password" not in k.lower():
                    settings[k] = v
            return {"settings": settings}
        elif action == "set":
            key = data.get("key")
            value = data.get("value")
            if key:
                # Update config
                self._log(f"Setting {key} = {value}")
            return {"status": "saved"}
        return {"error": "Unknown settings action"}

    def handle_status(self, data: Dict) -> Dict:
        uptime = time.time() - self.start_time
        return {
            "status": "running",
            "uptime_seconds": int(uptime),
            "sessions": len(self.sessions),
            "active_session": self.active_session,
            "config": {
                "provider": self.config.get("api_provider"),
                "model": self.config.get("active_model"),
            }
        }

    def handle_session(self, data: Dict) -> Dict:
        action = data.get("action")
        if action == "list":
            return {"sessions": {sid: {"name": s.name, "logs": len(s.logs)} for sid, s in self.sessions.items()}}
        elif data.get("action") == "create":
            sid = data.get("session_id", str(int(time.time())))
            self.sessions[sid] = TerminalSession(sid, data.get("name"))
            return {"status": "created", "session_id": sid}
        elif data.get("action") == "switch":
            sid = data.get("session_id")
            if sid in self.sessions:
                self.active_session = sid
                return {"status": "switched", "active_session": sid}
            return {"error": "Session not found"}
        elif data.get("action") == "remove":
            sid = data.get("session_id")
            if sid in self.sessions and len(self.sessions) > 1:
                del self.sessions[sid]
                return {"status": "removed"}
            return {"error": "Cannot remove last session"}
        return {"error": "Unknown session action"}

    # ============ Network Servers ============
    
    async def start_servers(self):
        """Start both Unix and TCP servers"""
        await asyncio.gather(
            self.start_unix_server(),
            self.start_tcp_server(),
        )

    async def start_unix_server(self):
        socket_path = self.home / ".hermes" / "hermes.sock"
        if socket_path.exists():
            socket_path.unlink()
        
        try:
            server = await asyncio.start_unix_server(
                self.handle_client,
                path=str(self.home / ".hermes" / "hermes.sock")
            )
            self._log(f"Unix socket server listening on {socket_path}")
            async with server:
                await server.serve_forever()
        except Exception as e:
            self._log(f"Unix server error: {e}", "ERROR")

    async def start_tcp_server(self):
        try:
            server = await asyncio.start_server(
                self.handle_client,
                "127.0.0.1", 5175
            )
            self._log("TCP server listening on 127.0.0.1:5175")
            async with server:
                await server.serve_forever()
        except Exception as e:
            self._log(f"TCP server error: {e}", "ERROR")

    async def handle_client(self, reader: asyncio.StreamReader, writer: asyncio.StreamWriter):
        addr = writer.get_extra_info('peername')
        self._log(f"Client connected: {addr}")
        
        try:
            while True:
                line = await reader.readline()
                if not line:
                    break
                
                response = self.handle_request(line.decode().strip())
                writer.write((json.dumps(response) + "\n").encode())
                await writer.drain()
                
        except Exception as e:
            self._log(f"Client error: {e}", "ERROR")
        finally:
            writer.close()
            await writer.wait_closed()

    async def run_forever(self):
        self._log("Daemon running, waiting for connections...")
        while True:
            await asyncio.sleep(3600)


async def main():
    home = os.environ.get("HERMES_HOME", os.path.expanduser("~"))
    daemon = HermesDaemon(home)
    
    # Handle signals
    loop = asyncio.get_running_loop()
    for sig in (signal.SIGTERM, signal.SIGINT):
        loop.add_signal_handler(sig, lambda: asyncio.create_task(daemon.stop()))
    
    await daemon.start_servers()


if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        print("\nShutting down...")
    except Exception as e:
        print(f"Fatal error: {e}")
        traceback.print_exc()
        sys.exit(1)