import json
import os
import subprocess
import sys
import time


DOTNET = os.environ.get(
    "MDPRO_DOTNET",
    r"D:\codexdate\2026-10-30\zhe\work\dotnet-sdk\dotnet.exe",
)
HOST = os.environ.get(
    "MDPRO_HOST_DLL",
    r"D:\codexdate\2026-10-30\zhe\work\mdpro-repo\tools\duel_host\bin\Release\net8.0\duel_host.dll",
)
ROOT = os.environ.get(
    "MDPRO_RUSH_ROOT",
    r"D:\QQ\聊天数据\pcl\.minecraft\versions\1.20.1-Fabric 0.16.14\mdpromod\runtime",
)
DECK = os.environ.get(
    "MDPRO_RUSH_DECK",
    r"D:\codexdate\2026-10-30\zhe\work\rush-invest\RushDemo.ydk",
)
SEED = int(os.environ.get("MDPRO_RUSH_SEED", "0"))
OPTIONS_TEXT = os.environ.get("MDPRO_RUSH_OPTIONS")
OPTIONS = int(OPTIONS_TEXT, 0) if OPTIONS_TEXT is not None else 0


def send(proc, payload):
    proc.stdin.write(json.dumps(payload, ensure_ascii=False) + "\n")
    proc.stdin.flush()


def respond_bytes(proc, data):
    send(proc, {"cmd": "responseBytes", "hex": data.hex().upper()})


def answer_prompt(proc, prompt):
    function = prompt.get("function")
    if function == 10:
        send(proc, {"cmd": "response", "value": 1 if prompt.get("attackCards") else 3})
    elif function == 11:
        if prompt.get("spsummonCards"):
            send(proc, {"cmd": "response", "value": 1})
        elif prompt.get("summonCards"):
            send(proc, {"cmd": "response", "value": 0})
        elif prompt.get("activateCards"):
            send(proc, {"cmd": "response", "value": 5})
        elif prompt.get("toBattle"):
            send(proc, {"cmd": "response", "value": 6})
        else:
            send(proc, {"cmd": "response", "value": 7})
    elif function in (12, 13):
        send(proc, {"cmd": "response", "value": 0})
    elif function == 14:
        options = prompt.get("options") or []
        send(proc, {"cmd": "response", "value": options[0]["index"] if options else 0})
    elif function == 15:
        cards = prompt.get("cards") or []
        minimum = max(1, int(prompt.get("min", 1)))
        count = min(len(cards), minimum)
        respond_bytes(proc, bytes([count, *range(count)]))
    elif function == 16:
        send(proc, {"cmd": "response", "value": -1})
    elif function in (18, 24):
        places = prompt.get("places") or []
        if not places:
            raise RuntimeError(f"no legal places: {prompt}")
        place = places[0]
        respond_bytes(proc, bytes([place["player"], place["location"], place["sequence"]]))
    elif function == 19:
        options = prompt.get("options") or []
        send(proc, {"cmd": "response", "value": options[0]["index"] if options else 1})
    else:
        raise RuntimeError(f"unsupported prompt function {function}: {prompt.get('summary')}")


def main():
    env = os.environ.copy()
    env["DOTNET_ROOT"] = os.path.dirname(DOTNET)
    proc = subprocess.Popen(
        [DOTNET, HOST, "--root", ROOT],
        cwd=ROOT,
        env=env,
        stdin=subprocess.PIPE,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        encoding="utf-8",
        errors="replace",
        bufsize=1,
    )
    deadline = time.time() + 90
    prompts = 0
    retries = 0
    messages = {}
    summon_codes = []
    special_summon_codes = []
    summons_by_turn = [0]
    draw_messages_by_turn = [0]
    draw_cards_by_turn = [0]
    draw_events = []
    phases_by_turn = [[]]
    phase_hexes = []
    lua_errors = []
    rush_bootstrap = None
    winner = None
    try:
        for line in proc.stdout:
            if time.time() > deadline:
                raise TimeoutError("rush host did not finish within 90 seconds")
            try:
                event = json.loads(line)
            except json.JSONDecodeError:
                continue
            kind = event.get("type")
            if kind == "ready":
                command = {
                    "cmd": "start",
                    "deck0": DECK,
                    "deck1": DECK,
                    "auto": False,
                    "mode": "rush",
                    "ai": True,
                    "aiSeat": 1,
                    "aiName": "AI_OldSchool",
                    "aiDeck": "AI_OldSchool",
                    "aiDeckFile": DECK,
                    "aiDialog": "cirno.zh-CN",
                    "timing": "default",
                }
                if SEED:
                    command["seed"] = SEED
                if OPTIONS_TEXT is not None:
                    command["options"] = OPTIONS
                send(proc, command)
            elif kind == "prompt":
                prompts += 1
                answer_prompt(proc, event)
            elif kind == "message":
                name = event.get("name")
                messages[name] = messages.get(name, 0) + 1
                if name == "NewTurn":
                    summons_by_turn.append(0)
                    draw_messages_by_turn.append(0)
                    draw_cards_by_turn.append(0)
                    phases_by_turn.append([])
                if name == "NewPhase" and isinstance(event.get("hex"), str) and len(event["hex"]) >= 6:
                    raw = bytes.fromhex(event["hex"])
                    phases_by_turn[-1].append(int.from_bytes(raw[1:3], "little"))
                if name == "NewPhase" and isinstance(event.get("hex"), str) and len(phase_hexes) < 24:
                    phase_hexes.append(event["hex"])
                if name == "Draw":
                    draw_messages_by_turn[-1] += 1
                    hex_value = event.get("hex")
                    draw_count = 0
                    if isinstance(hex_value, str) and len(hex_value) >= 6:
                        draw_count = bytes.fromhex(hex_value)[2]
                    draw_cards_by_turn[-1] += draw_count
                    if len(draw_events) < 32:
                        draw_events.append({
                            "turn": len(draw_cards_by_turn) - 1,
                            "count": draw_count,
                            "hex": hex_value,
                        })
                if name == "Summoning" and isinstance(event.get("hex"), str) and len(event["hex"]) >= 10:
                    summon_codes.append(int.from_bytes(bytes.fromhex(event["hex"][2:10]), "little"))
                    summons_by_turn[-1] += 1
                if name == "SpSummoning" and isinstance(event.get("hex"), str) and len(event["hex"]) >= 10:
                    special_summon_codes.append(int.from_bytes(bytes.fromhex(event["hex"][2:10]), "little"))
                if name == "Win":
                    winner = event.get("winner")
            elif kind == "ended":
                break
            elif kind == "rushBootstrap":
                rush_bootstrap = event
            elif kind == "luaError":
                lua_errors.append(event.get("message", ""))
            elif kind == "retry":
                retries += 1
            elif kind == "error":
                raise RuntimeError(event.get("message"))
    finally:
        try:
            send(proc, {"cmd": "stop"})
        except Exception:
            pass
        try:
            proc.wait(timeout=5)
        except subprocess.TimeoutExpired:
            proc.kill()
    print(json.dumps({
        "passed": (
            winner is not None
            and retries == 0
            and not lua_errors
            and rush_bootstrap is not None
            and max(summons_by_turn, default=0) >= 2
            and len(draw_cards_by_turn) > 1
            and draw_cards_by_turn[0] == 10
            and draw_cards_by_turn[1] == 1
            and not any(2 in phases for phases in phases_by_turn)
            and not any(16 in phases for phases in phases_by_turn)
        ),
        "winner": winner,
        "rushBootstrap": rush_bootstrap,
        "luaErrors": lua_errors,
        "prompts": prompts,
        "retries": retries,
        "summons": messages.get("Summoning", 0),
        "summonCodes": summon_codes,
        "summonsByTurn": summons_by_turn,
        "drawMessagesByTurn": draw_messages_by_turn,
        "drawCardsByTurn": draw_cards_by_turn,
        "drawEvents": draw_events,
        "phasesByTurn": phases_by_turn,
        "phaseHexes": phase_hexes,
        "hasStandbyPhase": any(2 in phases for phases in phases_by_turn),
        "hasMain2Phase": any(16 in phases for phases in phases_by_turn),
        "specialSummonCodes": special_summon_codes,
        "attacks": messages.get("Attack", 0),
        "damage": messages.get("Damage", 0),
        "winMessages": messages.get("Win", 0),
        "messageNames": dict(sorted(messages.items())),
    }, ensure_ascii=False, indent=2))
    return 0 if winner is not None else 1


if __name__ == "__main__":
    sys.exit(main())
