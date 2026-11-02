"""Maximum / Rush 融合 / 仪式 的脚本层端到端探测。

为什么不走 prompt 应答：现有的自动应答脚本遇到 SelectTribute / AnnounceCard 之类
会给出非法应答，超过重试上限就被宿主判"连续应答非法"。所以这里开局就带 auto=true，
让宿主自己应答（AutoAnswer），从而验证的是"引擎 + Lua 脚本能不能把这类卡组跑完"，
不覆盖 UI 层。

判据：
  - 没有 luaError / error
  - 对局正常分出胜负（有 Win 消息）
  - 卡组里那几张关键卡的脚本确实被加载（没有 initial_effect 报错）
"""
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
    r"D:\QQ\聊天数据\pcl\.minecraft\versions\1.20.1-Fabric 0.16.14\mdpromod\rush-runtime",
)
DECK_DIR = os.environ.get(
    "MDPRO_DECK_DIR",
    r"D:\codexdate\2026-10-30\zhe\work\rush-invest",
)
SEEDS = [int(s) for s in os.environ.get("MDPRO_SEEDS", "12345,777").split(",")]
DECKS = os.environ.get(
    "MDPRO_DECKS",
    "RDRushDemo,RDRushEffectDemo,RDRushFusionDemo,RDMaximumDemo",
).split(",")


def run(deck, seed):
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
    deadline = time.time() + 120
    messages = {}
    lua_errors = []
    errors = []
    winner = None
    rush_bootstrap = None
    try:
        for line in proc.stdout:
            if time.time() > deadline:
                errors.append("timeout")
                break
            try:
                event = json.loads(line)
            except json.JSONDecodeError:
                continue
            kind = event.get("type")
            if kind == "ready":
                proc.stdin.write(json.dumps({
                    "cmd": "start",
                    "deck0": os.path.join(DECK_DIR, deck + ".ydk"),
                    "deck1": os.path.join(DECK_DIR, deck + ".ydk"),
                    "auto": True,
                    "mode": "rush",
                    "ai": False,
                    "aiSeat": 1,
                    "timing": "default",
                    "seed": seed,
                }, ensure_ascii=False) + "\n")
                proc.stdin.flush()
            elif kind == "message":
                name = event.get("name")
                messages[name] = messages.get(name, 0) + 1
                if name == "Win":
                    winner = event.get("winner")
            elif kind == "luaError":
                lua_errors.append(event.get("message", ""))
            elif kind == "error":
                errors.append(event.get("message", ""))
            elif kind == "rushBootstrap":
                rush_bootstrap = event
            elif kind == "ended":
                break
    finally:
        try:
            proc.stdin.write(json.dumps({"cmd": "stop"}) + "\n")
            proc.stdin.flush()
        except Exception:
            pass
        try:
            proc.wait(timeout=5)
        except subprocess.TimeoutExpired:
            proc.kill()
    return {
        "deck": deck,
        "seed": seed,
        "winner": winner,
        "luaErrors": len(lua_errors),
        "luaErrorSample": lua_errors[:3],
        "errors": errors[:3],
        "bootstrap": rush_bootstrap is not None,
        "summon": messages.get("Summoning", 0),
        "spSummon": messages.get("SpSummoning", 0),
        "attack": messages.get("Attack", 0),
        "damage": messages.get("Damage", 0),
        "turns": messages.get("NewTurn", 0),
        "win": messages.get("Win", 0),
    }


def main():
    rows = []
    for deck in DECKS:
        path = os.path.join(DECK_DIR, deck + ".ydk")
        if not os.path.exists(path):
            print(f"{deck}: 卡组不存在，跳过")
            continue
        for seed in SEEDS:
            rows.append(run(deck, seed))
    print(f'{"卡组":<20}{"seed":>7}{"winner":>7}{"luaErr":>8}{"err":>5}'
          f'{"普召":>6}{"特召":>6}{"攻击":>6}{"回合":>6}{"Win":>5}')
    bad = 0
    for r in rows:
        ok = r["luaErrors"] == 0 and not r["errors"] and r["win"] > 0
        if not ok:
            bad += 1
        print(f'{r["deck"]:<20}{r["seed"]:>7}{str(r["winner"]):>7}{r["luaErrors"]:>8}'
              f'{len(r["errors"]):>5}{r["summon"]:>6}{r["spSummon"]:>6}{r["attack"]:>6}'
              f'{r["turns"]:>6}{r["win"]:>5}{"" if ok else "   <-- 有问题"}')
        if r["luaErrorSample"]:
            print("      lua:", r["luaErrorSample"])
        if r["errors"]:
            print("      err:", r["errors"])
    print()
    print(f"共 {len(rows)} 局，有问题的 {bad} 局")
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
