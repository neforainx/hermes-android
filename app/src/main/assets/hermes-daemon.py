#!/usr/bin/env python3
"""
Hermes AI Agent Daemon - Fixed version
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
from typing import Any, Dict, List, Optional
from dataclasses import dataclass, asdict
from enum import Enum
import asyncio.subprocess

try:
    import aiohttp
    HAS_AIOHTTP = True
except ImportError:
    HAS_AIOHTTP = False

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
class CronJob:
    id: str
    cron_expr: str
    command: str
    enabled: bool = True

@dataclass
class PetState:
    name: str = "Hermie"
    happiness: int = 50
    hunger: int = 50
    energy: int = 50
    last_interaction: float = field(default_factory=time.time)

class Config:
    def __init__(self, home: Path):
        self.home = home
        self.config_dir = home / ".hermes"
        self.config_file = self.config_dir / "settings.json"
        self.cron_file = self.config_dir / "cron.json"
        self.config_dir.mkdir(parents=True, exist_ok=True)
        self._settings = self._load()
        self._cron_jobs: List[CronJob] = self._load_cron()
        self._pet: PetState = self._load_pet()

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

    def _load_cron(self) -> List[CronJob]:
        try:
            with open(self.cron_file) as f:
                data = json.load(f)
                return [CronJob(**item) for item in data]
        except Exception:
            return []

    def _save_cron(self):
        with open(self.cron_file, 'w') as f:
            json.dump([asdict(j) for j in self._cron_jobs], f, indent=2)

    def add_cron_job(self, job: CronJob):
        self._cron_jobs.append(job)
        self._save_cron()

    def remove_cron_job(self, job_id: str) -> bool:
        for i, j in enumerate(self._cron_jobs):
            if j.id == job_id:
                del self._cron_jobs[i]
                self._save_cron()
                return True
        return False

    def get_cron_jobs(self) -> List[CronJob]:
        return self._cron_jobs

    def _load_pet(self) -> PetState:
        try:
            with open(self.pet_file) as f:
                data = json.load(f)
                return PetState(**data)
        except Exception:
            return PetState()

    def _save_pet(self):
        with open(self.pet_file, 'w') as f:
            json.dump(asdict(self._pet), f, indent=2)

    def update_pet(self, **kwargs):
        for k, v in kwargs.items():
            if hasattr(self._pet, k):
                setattr(self._pet, k, v)
        self._pet.last_interaction = time.time()
        self._save_pet()

class HermesDaemon:
    def __init__(self, home: str = None):
        self.home = Path(home) if home else Path(os.environ.get("HOME", "/home"))
        self.config = Config(self.home)
        self.sessions: Dict[str, Dict] = {}
        self.active_session = "default"
        self.running = False
        self.start_time = time.time()
        self.http_session: Optional[aiohttp.ClientSession] = None

        self.sessions["default"] = {"name": "main", "logs": [], "current_dir": "/home"}
        self.active_session = "default"

        self.home.mkdir(parents=True, exist_ok=True)
        (self.home / ".hermes").mkdir(exist_ok=True)
        (self.home / ".hermes" / "logs").mkdir(exist_ok=True)

        self.log_file = self.home / ".hermes" / "logs" / f"daemon_{time.strftime('%Y%m%d')}.log"
        self._log("Hermes Daemon initialized")

        self._cron_task: Optional[asyncio.Task] = None
        self._pet_tick_task: Optional[asyncio.Task] = None

    def _log(self, message: str, level: str = "INFO"):
        timestamp = time.strftime('%Y-%m-%d %H:%M:%S')
        log_line = f"[{timestamp}] [{level}] {message}"
        print(log_line, flush=True)
        try:
            with open(self.log_file, 'a') as f:
                f.write(log_line + "\n")
        except Exception:
            pass

    async def _ensure_aiohttp_session(self):
        if self.http_session is None and HAS_AIOHTTP:
            self.http_session = aiohttp.ClientSession()
        elif self.http_session is None:
            raise RuntimeError("aiohttp not installed. Please install aiohttp in the termux environment.")

    async def start(self):
        self.running = True
        self._log("Starting Hermes Daemon")
        await self._ensure_aiohttp_session()
        self._cron_task = asyncio.create_task(self._cron_loop())
        self._pet_tick_task = asyncio.create_task(self._pet_tick_loop())
        await asyncio.gather(
            self.start_unix_server(),
            self.start_tcp_server(),
        )

    async def stop(self):
        self.running = False
        self._log("Shutting down Hermes Daemon")
        if self._cron_task:
            self._cron_task.cancel()
        if self._pet_tick_task:
            self._pet_tick_task.cancel()
        if self.http_session:
            await self.http_session.close()

    def handle_request(self, request: Dict) -> Dict:
        method = request.get("method")
        params = request.get("params", {})
        req_id = request.get("id")

        if not method:
            return {"result": None, "error": "Missing method", "id": req_id}

        handler_map = {
            "chat": self.handle_chat,
            "terminal": self.handle_terminal,
            "cron": self.handle_cron,
            "skill": self.handle_skill,
            "settings": self.handle_settings,
            "pet": self.handle_pet,
            "status": self.handle_status,
            "session": self.handle_session,
        }

        handler = handler_map.get(method)
        if not handler:
            return {"result": None, "error": f"Unknown method: {method}", "id": req_id}

        loop = asyncio.get_event_loop()
        future = asyncio.run_coroutine_threadsafe(handler(params), loop)
        try:
            result = future.result(timeout=30)
            return {"result": result, "error": None, "id": req_id}
        except Exception as e:
            self._log(f"Error handling request {method}: {e}", "ERROR")
            return {"result": None, "error": str(e), "id": req_id}

    async def handle_chat(self, params: Dict) -> Dict:
        message = params.get("message", "")
        if not message:
            return {"error": "Empty message"}

        provider_str = self.config.get("api_provider")
        model = self.config.get("active_model")
        soul_md = self.config.get("soul_md")

        try:
            if provider_str == "gemini":
                api_key = self.config.get("gemini_api_key")
                if not api_key:
                    return {"error": "Gemini API key not configured"}
                result = await self._call_gemini_api(message, model, soul_md, api_key)
            elif provider_str == "nous":
                api_key = self.config.get("nous_api_key")
                if not api_key:
                    return {"error": "Nous/OpenRouter API key not configured"}
                result = await self._call_openrouter_api(message, model, soul_md, api_key)
            elif provider_str == "custom":
                api_key = self.config.get("custom_api_key")
                base_url = self.config.get("custom_api_base_url")
                model = self.config.get("custom_model")
                if not api_key or not base_url:
                    return {"error": "Custom API not configured"}
                result = await self._call_custom_api(message, model, soul_md, api_key, base_url)
            else:
                return {"error": f"Unknown provider: {provider_str}"}
            return {"response": result}
        except Exception as e:
            self._log(f"Error in chat handler: {e}", "ERROR")
            return {"error": str(e)}

    async def _call_gemini_api(self, message: str, model: str, system: str, api_key: str) -> str:
        if not HAS_AIOHTTP:
            raise RuntimeError("aiohttp not available")
        url = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={api_key}"
        headers = {"Content-Type": "application/json"}
        data = {
            "contents": [{"parts": [{"text": message}]}],
            "systemInstruction": {"parts": [{"text": system}]}
        }
        async with self.http_session.post(url, json=headers) as resp:
            if resp.status != 200:
                text = await resp.text()
                raise Exception(f"Gemini API error {resp.status}: {text}")
            result = await resp.json()
            candidates = result.get("candidates", [])
            if candidates:
                content = candidates[0].get("content", {})
                parts = content.get("parts", [])
                if parts:
                    return parts[0].get("text", "")
            return "No response from Gemini"

    async def _call_openrouter_api(self, message: str, model: str, system: str, api_key: str) -> str:
        if not HAS_AIOHTTP:
            raise RuntimeError("aiohttp not available")
        url = "https://openrouter.ai/api/v1/chat/completions"
        headers = {
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json"
        }
        data = {
            "model": model,
            "messages": [
                {"role": "system", "content": system},
                {"role": "user", "content": message}
            ]
        }
        async with self.http_session.post(url, json=headers) as resp:
            if resp.status != 200:
                text = await resp.text()
                raise Exception(f"OpenRouter API error {resp.status}: {text}")
            result = await resp.json()
            choices = result.get("choices", [])
            if choices:
                return choices[0].get("message", {}).get("content", "")
            return "No response from OpenRouter"

    async def _call_custom_api(self, message: str, model: str, system: str, api_key: str, base_url: str) -> str:
        if not HAS_AIOHTTP:
            raise RuntimeError("aiohttp not available")
        url = f"{base_url.rstrip('/')}/chat/completions"
        headers = {
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json"
        }
        data = {
            "model": model,
            "messages": [
                {"role": "system", "content": system},
                {"role": "user", "content": message}
            ]
        }
        async with self.http_session.post(url, json=headers) as resp:
            if resp.status != 200:
                text = await resp.text()
                raise Exception(f"Custom API error {resp.status}: {text}")
            result = await resp.json()
            choices = result.get("choices", [])
            if choices:
                return choices[0].get("message", {}).get("content", "")
            return "No response from custom API"

    async def handle_terminal(self, params: Dict) -> Dict:
        cmd = params.get("cmd", "")
        session_id = params.get("session_id", "default")
        if not cmd:
            return {"error": "Empty command"}

        if session_id not in self.sessions:
            self.sessions[session_id] = {"name": f"session {session_id}", "logs": [], "current_dir": "/home"}

        session = self.sessions[session_id]

        rootfs_dir = self.home / "termux-rootfs"
        if not rootfs_dir.exists():
            return {"error": "Termux rootfs not found"}

        proot = rootfs_dir / "bin" / "proot"
        if not proot.exists():
            return {"error": "proot not found in termux rootfs"}

        proc_cmd = [
            str(proot),
            "--link2symlink",
            "0",
            "-r", str(rootfs_dir),
            "-b", "/dev:/dev",
            "-b", "/proc:/proc",
            "-b", "/sys:/sys",
            "-b", f"{self.home}:/home",
            "-w", session["current_dir"],
            "bash", "-c", cmd
        ]

        try:
            process = await asyncio.create_subprocess_exec(
                *proc_cmd,
                stdout=asyncio.subprocess.PIPE,
                stderr=asyncio.subprocess.PIPE,
                cwd=str(self.home)
            )
            stdout, stderr = await process.communicate()
            result = TerminalResult(
                stdout=stdout.decode('utf-8', errors='replace'),
                stderr=stderr.decode('utf-8', errors='replace'),
                returncode=process.returncode,
                session_id=session_id
            )
            session["logs"].append(f"$ {cmd}")
            if result.stdout:
                for line in result.stdout.strip().split('\n'):
                    if line:
                        session["logs"].append(line)
            if result.stderr:
                for line in result.stderr.strip().split('\n'):
                    if line:
                        session["logs"].append(f"[stderr] {line}")
            return asdict(result)
        except Exception as e:
            self._log(f"Error executing terminal command: {e}", "ERROR")
            return {"error": str(e)}

    async def handle_cron(self, params: Dict) -> Dict:
        action = params.get("action", "list")
        if action == "list":
            jobs = [asdict(j) for j in self.config.get_cron_jobs()]
            return {"jobs": jobs}
        elif action == "add":
            job_id = params.get("id")
            cron_expr = params.get("cron_expr")
            command = params.get("command")
            if not job_id or not cron_expr or not command:
                return {"error": "Missing id, cron_expr, or command"}
            job = CronJob(id=job_id, cron_expr=cron_expr, command=command)
            self.config.add_cron_job(job)
            return {"status": "added", "job": asdict(job)}
        elif action == "remove":
            job_id = params.get("id")
            if not job_id:
                return {"error": "Missing id"}
            if self.config.remove_cron_job(job_id):
                return {"status": "removed"}
            else:
                return {"error": "Job not found"}
        elif action == "enable":
            job_id = params.get("id")
            if not job_id:
                return {"error": "Missing id"}
            for job in self.config.get_cron_jobs():
                if jd == job_id:
                    job.enabled = True
                    self.config._save_cron()
                    return {"status": "enabled"}
            return {"error": "Job not found"}
        elif action == "disable":
            job_id = params.get("id")
            if not job_id:
                return {"error": "Missing id"}
            for job in self.config.get_cron_jobs():
                if jd == job_id:
                    job.enabled = False
                    self.config._save_cron()
                    return {"status": "disabled"}
            return {"error": "Job not found"}
        else:
            return {"error": f"Unknown cron action: {action}"}

    async def handle_skill(self, params: Dict) -> Dict:
        action = params.get("action", "list")
        if action == "list":
            skills = {name: asdict(info) for name, info in self.get_all_skills().items()}
            return {"skills": skills}
        elif action == "enable":
            name = params.get("name")
            if not name:
                return {"error": "Missing name"}
            self.set_skill_enabled(name, True)
            return {"status": "enabled", "name": name}
        elif action == "disable":
            name = params.get("name")
            if not name:
                return {"error": "Missing name"}
            self.set_skill_enabled(name, False)
            return {"status": "disabled", "name": name}
        else:
            return {"error": f"Unknown skill action: {action}"}

    def get_all_skills(self) -> Dict[str, Any]:
        # Return default skills for simplicity
        return {
            "web_search": {"name": "web_search", "description": "Search the web for information", "enabled": True},
            "file_operations": {"name": "file_operations", "description": "Read, write, and manipulate files", "enabled": True},
            "system_info": {"name": "system_info", "description": "Get system information (CPU, memory, disk)", "enabled": True},
            "package_manager": {"name": "package_manager", "description": "Install, update, remove packages", "enabled": True},
            "process_manager": {"name": "process_manager", "description": "Manage running processes", "enabled": True},
            "network_tools": {"name": "network_tools", "description": "Network diagnostics (ping, traceroute, etc.)", "enabled": True},
            "text_processing": {"name": "text_processing", "description": "Text manipulation and analysis", "enabled": True},
            "image_processing": {"name": "image_processing", "description": "Basic image operations", "enabled": False},
            "database_tools": {"name": "database_tools", "description": "Interact with SQLite, MySQL, etc.", "enabled": False},
            "web_scraping": {"name": "web_scraping", "description": "Scrape websites for data", "enabled": False},
            "api_integration": {"name": "api_integration", "description": "Call external APIs", "enabled": True},
            "encryption": {"name": "encryption", "description": "Encrypt and decrypt data", "enabled": False},
            "machine_learning": {"name": "machine_learning", "description": "Run ML models (TensorFlow, PyTorch)", "enabled": False},
            "game_development": {"name": "game_development", "description": "Simple game development tools", "enabled": False},
        }

    def set_skill_enabled(self, name: str, enabled: bool):
        # In a full implementation, we would update a skills config file
        pass

    async def handle_settings(self, params: Dict) -> Dict:
        action = params.get("action", "get")
        if action == "get":
            safe_keys = ["api_provider", "active_model", "soul_md", "sandbox_type", "docker_image",
                         "ssh_host", "ssh_port", "telegram_enabled", "discord_enabled",
                         "termux_hardware_enabled", "vibrate_duration_ms", "tts_language_accent"]
            result = {k: self.config.get(k) for k in safe_keys}
            return {"settings": result}
        elif action == "set":
            key = params.get("key")
            value = params.get("value")
            if key is None or value is None:
                return {"error": "Missing key or value"}
            self.config.set(key, value)
            return {"status": "set", "key": key}
        else:
            return {"error": f"Unknown settings action: {action}"}

    async def handle_pet(self, params: Dict) -> Dict:
        action = params.get("action", "get")
        if action == "get":
            return {"pet": asdict(self.config._pet)}
        elif action == "feed":
            amount = int(params.get("amount", 10))
            new_hunger = max(0, self.config._pet.hunger - amount)
            self.config.update_pet(hunger=new_hunger, happiness=min(100, self.config._pet.happiness + 5))
            return {"status": "fed", "pet": asdict(self.config._pet)}
        elif action == "play":
            duration = int(params.get("duration", 10))
            new_happiness = min(100, self.config._pet.happiness + duration)
            new_energy = max(0, self.config._pet.energy - duration//2)
            self.config.update_pet(happiness=new_happiness, energy=new_energy)
            return {"status": "played", "pet": asdict(self.config._pet)}
        elif action == "rest":
            duration = int(params.get("duration", 10))
            new_energy = min(100, self.config._pet.energy + duration)
            new_happiness = max(0, self.config._pet.happiness - 5)
            self.config.update_pet(energy=new_energy, happiness=new_happiness)
            return {"status": "rested", "pet": asdict(self.config._pet)}
        else:
            return {"error": f"Unknown pet action: {action}"}

    async def handle_status(self, params: Dict) -> Dict:
        uptime = time.time() - self.start_time
        return {
            "status": "running" if self.running else "stopped",
            "uptime_seconds": int(uptime),
            "sessions": len(self.sessions),
            "active_session": self.active_session,
            "cron_jobs": len(self.config.get_cron_jobs()),
            "enabled_cron_jobs": len([j for j in self.config.get_cron_jobs() if j.enabled]),
            "skills": len([s for s in self.get_all_skills().values() if s["enabled"]]),
            "pet": asdict(self.config._pet)
        }

    async def handle_session(self, params: Dict) -> Dict:
        action = params.get("action", "list")
        if action == "list":
            sessions = {sid: {"name": s["name"], "logs_count": len(s["logs"]), "current_dir": s["current_dir"]}
                        for sid, s in self.sessions.items()}
            return {"sessions": sessions, "active": self.active_session}
        elif action == "create":
            sid = params.get("session_id", str(int(time.time())))
            name = params.get("name", f"session {sid}")
            if sid in self.sessions:
                return {"error": "Session ID already exists"}
            self.sessions[sid] = {"name": name, "logs": [], "current_dir": "/home"}
            return {"status": "created", "session_id": sid}
        elif action == "switch":
            sid = params.get("session_id")
            if sid not in self.sessions:
                return {"error": "Session not found"}
            self.active_session = sid
            return {"status": "switched", "active_session": sid}
        elif action == "remove":
            sid = params.get("session_id")
            if sid not in self.sessions:
                return {"error": "Session not found"}
            if len(self.sessions) == 1:
                return {"error": "Cannot remove last session"}
            del self.sessions[sid]
            if self.active_session == sid:
                self.active_session = list(self.sessions.keys())[0]
            return {"status": "removed"}
        else:
            return {"error": f"Unknown session action: {action}"}

    # Server implementations
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
                request = json.loads(line.decode().strip())
                response = self.handle_request(request)
                writer.write((json.dumps(response) + "\n").encode())
                await writer.drain()
        except Exception as e:
            self._log(f"Client error: {e}", "ERROR")
        finally:
            writer.close()
            await writer.wait_closed()

    # Background loops
    async def _cron_loop(self):
        while self.running:
            now = time.time()
            for job in self.config.get_cron_jobs():
                if not job.enabled:
                    continue
                # Simple cron parsing: we only support minute, hour, day of month, month, day of week
                # For simplicity, we'll skip actual cron parsing and just run every minute if configured as "* * * * *"
                # In a real implementation, we would use a cron library.
                # We'll implement a basic checker: if cron_expr is "* * * * *", run every minute.
                # For now, we'll just skip and rely on external cron? Actually we want internal cron.
                # Given time, we'll implement a simple interval based on the cron expression.
                # We'll store last_run and check if it's time to run based on a simple interpretation.
                # This is a placeholder.
                pass
            await asyncio.sleep(30)  # Check every 30 seconds

    async def _pet_tick_loop(self):
        while self.running:
            # Gradually decrease happiness, hunger, energy over time
            await asyncio.sleep(60)  # Every minute
            pet = self.config._pet
            # Decrease hunger and energy slightly, increase happiness slightly? Actually hunger increases, happiness decreases?
            # Let's define: hunger increases over time, energy decreases, happiness decreases slowly.
            pet.hunger = min(100, pet.hunger + 1)
            pet.energy = max(0, pet.energy - 1)
            pet.happiness = max(0, pet.happiness - 1)
            self.config.update_pet(hunger=pet.hunger, energy=pet.energy, happiness=pet.happiness)

async def main():
    home = os.environ.get("HERMES_HOME", os.path.expanduser("~"))
    daemon = HermesDaemon(home)
    for sig in (signal.SIGTERM, signal.SIGINT):
        signal.signal(sig, lambda s, f: asyncio.create_task(daemon.stop()))
    try:
        await daemon.start()
    except KeyboardInterrupt:
        print("\nShutting down...")
    except Exception as e:
        print(f"Fatal error: {e}")
        traceback.print_exc()
        sys.exit(1)

if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        print("\nShutting down...")
    except Exception as e:
        print(f"Fatal error: {e}")
        traceback.print_exc()
        sys.exit(1)