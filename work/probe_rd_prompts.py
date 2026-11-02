"""打印一副 RD 卡组在开局若干条提示的形状，用来判断"自动应答为什么答不对"。

用法：MDPRO_DECK=RDMaximumDemo python probe_rd_prompts.py
"""
import json
import os
import subprocess
import sys
import time

DOTNET = os.environ.get("MDPRO_DOTNET", r"D:\codexdate\2026-10-30\zhe\work\dotnet-sdk\dotnet.exe")
HOST = os.environ.get(
    "MDPRO_HOST_DLL",
    r"D:\codexdate\2026-10-30\zhe\work\mdpro-repo\tools\duel_host\bin\Release\net8.0\duel_host.dll",
)
ROOT = os.environ.get(
    "MDPRO_RUSH_ROOT",
    r"D:\QQ\聊天数据\pcl\.minecraft\versions\1.20.1-Fabric 0.16.14\mdpromod\rush-runtime",
)
DECK_DIR = os.environ.get("MDPRO_DECK_DIR", r"D:\codexdate\2026-10-30\zhe\work\rush-invest")
DECK = os.environ.get("MDPRO_DECK", "RDMaximumDemo")
SEED = int(os.environ.get("MDPRO_SEED", "12345"))
MAX = int(os.environ.get("MDPRO_MAX", "40"))


def send(proc, payload):
    proc.stdin.write(json.dumps(payload, ensure_ascii=False) + "\n")
    proc.stdin.flush()


def options_of(prompt):
    opts = prompt.get("options") or []
    return [(o.get("index"), o.get("label") or o.get("description")) for o in opts]


def answer(proc, p):
    f = p.get("function")
    if f == 11:
        for key, value in (("summonCards", 0), ("spsummonCards", 1), ("positionCards", 2),
                           ("monsterSetCards", 3), ("spellSetCards", 4), ("activateCards", 5)):
            if p.get(key):
                send(proc, {"cmd": "response", "value": value})
                return f"11 -> {value} ({key})"
        send(proc, {"cmd": "response", "value": 6 if p.get("toBattle") else 7})
        return "11 -> battle/end"
    if f == 10:
        send(proc, {"cmd": "response", "value": 1 if p.get("attackCards") else 3})
        return "10 -> 1/3"
    if f in (12, 13):
        send(proc, {"cmd": "response", "value": 1})
        return f"{f} -> yes"
    if f == 14:
        o = p.get("options") or []
        send(proc, {"cmd": "response", "value": o[0]["index"] if o else 0})
        return "14 -> first option"
    if f == 15:
        cards = p.get("cards") or []
        n = max(1, int(p.get("min", 1)))
        n = min(n, len(cards))
        send(proc, {"cmd": "responseBytes", "hex": bytes([n, *range(n)]).hex().upper()})
        return f"15 -> {n} of {len(cards)}"
    if f == 16:
        send(proc, {"cmd": "response", "value": -1})
        return "16 -> pass"
    if f in (18, 24):
        places = p.get("places") or []
        if not places:
            return "18 -> NO PLACES"
        pl = places[0]
        send(proc, {"cmd": "responseBytes",
                    "hex": bytes([pl["player"], pl["location"], pl["sequence"]]).hex().upper()})
        return "18 -> first place"
    if f == 19:
        o = p.get("options") or []
        send(proc, {"cmd": "response", "value": o[0]["index"] if o else 1})
        return "19 -> first position"
    if f == 20:
        cards = p.get("cards") or []
        n = int(p.get("min", 1))
        if len(cards) < n:
            send(proc, {"cmd": "response", "value": -1})
            return "20 -> cancel"
        send(proc, {"cmd": "responseBytes", "hex": bytes([n, *range(n)]).hex().upper()})
        return f"20 -> {n}"
    if f == 22:
        q = int(p.get("quantity", 0))
        c = int(p.get("count", 0))
        v = [0] * c
        if c:
            v[0] = q & 0xFF
        send(proc, {"cmd": "responseBytes", "hex": bytes(v).hex().upper()})
        return "22 -> all to first"
    if f == 23:
        m = int(p.get("must", 0))
        send(proc, {"cmd": "responseBytes", "hex": bytes([m, *range(m)]).hex().upper()})
        return f"23 -> must {m}"
    if f == 25:
        c = int(p.get("count", 0))
        send(proc, {"cmd": "responseBytes", "hex": bytes(range(c)).hex().upper()})
        return f"25 -> identity {c}"
    if f == 26:
        send(proc, {"cmd": "response", "value": -1 if p.get("cancelable") else 1})
        return "26 -> cancel/finish"
    raise RuntimeError(f"unsupported {f}")


def main():
    env = os.environ.copy()
    env["DOTNET_ROOT"] = os.path.dirname(DOTNET)
    proc = subprocess.Popen(
        [DOTNET, HOST, "--root", ROOT], cwd=ROOT, env=env,
        stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
        text=True, encoding="utf-8", errors="replace", bufsize=1,
    )
    shown = 0
    retries = 0
    deadline = time.time() + 120
    try:
        for line in proc.stdout:
            if time.time() > deadline:
                print("...超时")
                break
            try:
                e = json.loads(line)
            except json.JSONDecodeError:
                continue
            k = e.get("type")
            if k == "ready":
                send(proc, {
                    "cmd": "start",
                    "deck0": os.path.join(DECK_DIR, DECK + ".ydk"),
                    "deck1": os.path.join(DECK_DIR, DECK + ".ydk"),
                    "auto": False, "mode": "rush", "ai": False, "aiSeat": 1,
                    "timing": "default", "seed": SEED,
                })
            elif k == "prompt":
                shown += 1
                if shown <= MAX:
                    keys = [x for x in ("summonCards", "spsummonCards", "positionCards",
                                        "monsterSetCards", "spellSetCards", "activateCards",
                                        "cards", "options", "places")
                            if e.get(x)]
                    print(f'#{shown:3d} f={e.get("function"):>2} {e.get("name"):<22} '
                          f'p={e.get("player")} summary={e.get("summary")}')
                    if keys:
                        print(f'      有: {keys}')
                    if e.get("options"):
                        print(f'      选项: {options_of(e)}')
                    try:
                        act = answer(proc, e)
                        print(f'      应答: {act}')
                    except RuntimeError as ex:
                        print(f'      无法应答: {ex}')
                        break
                else:
                    answer(proc, e)
            elif k == "retry":
                retries += 1
                if retries <= 5:
                    print(f'      !! RETRY #{retries}: {e.get("message")}')
                if retries > 8:
                    print("...重试过多，停止")
                    break
            elif k == "ended":
                print("对局结束")
                break
            elif k == "error":
                print("ERROR:", e.get("message"))
                break
    finally:
        try:
            send(proc, {"cmd": "stop"})
        except Exception:
            pass
        try:
            proc.wait(timeout=5)
        except subprocess.TimeoutExpired:
            proc.kill()
    print(f"共 {shown} 条提示，{retries} 次重试")
    return 0


if __name__ == "__main__":
    sys.exit(main())
