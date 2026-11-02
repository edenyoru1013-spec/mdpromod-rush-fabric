"""探测实际打包的 duel_host 是否在 prompt 里带 player（= 等待应答的席位）。

关心两件事：
1. 每条 prompt 是否都有 "player"
2. 是否存在 player != 当前回合玩家 的提示（对方回合应答），
   这正是 answerSeat() 退回 turnPlayer 会出错的场景。
"""
import json
import os
import subprocess
import sys
import time

HOST = os.environ.get(
    "MDPRO_HOST",
    r"D:\QQ\聊天数据\pcl\.minecraft\versions\1.20.1-Fabric 0.16.14\mdpromod\duelhost\duel_host.exe",
)
ROOT = os.environ.get(
    "MDPRO_RUSH_ROOT",
    r"D:\QQ\聊天数据\pcl\.minecraft\versions\1.20.1-Fabric 0.16.14\mdpromod\rush-runtime",
)
# master 模式下宿主不会自动代答席位 1，可以用来验证 player 字段是否真的跟着"被问的席位"走
MODE = os.environ.get("MDPRO_PROBE_MODE", "rush")
MASTER_ROOT = os.environ.get(
    "MDPRO_MASTER_ROOT",
    r"D:\QQ\聊天数据\pcl\.minecraft\versions\1.20.1-Fabric 0.16.14\mdpromod\runtime",
)
MASTER_DECK = os.environ.get(
    "MDPRO_MASTER_DECK",
    r"D:\QQ\聊天数据\pcl\.minecraft\versions\1.20.1-Fabric 0.16.14\config\mdpromod\decks\AI_BlueEyes.ydk",
)
DECK = os.environ.get(
    "MDPRO_RUSH_DECK",
    r"D:\codexdate\2026-10-30\zhe\work\rush-invest\RDRushEffectDemo.ydk",
)
# 关掉 AI：席位 1 的提示才会外发，否则 WindBot 在进程内自己应答，我们只看到席位 0
USE_AI = os.environ.get("MDPRO_PROBE_AI", "0") not in ("0", "", "false", "False")
# 主动应答（连锁选是 / 效果选是），制造更多对方回合的应答窗口
AGGRESSIVE = os.environ.get("MDPRO_PROBE_AGGRESSIVE", "1") not in ("0", "", "false", "False")
HUMAN_SEAT1 = os.environ.get("MDPRO_PROBE_HUMAN_SEAT1", "0") not in ("0", "", "false", "False")
SEEDS = [int(s) for s in os.environ.get("MDPRO_PROBE_SEEDS", "12345,777,2024,31,9001").split(",")]


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
        send(proc, {"cmd": "response", "value": 1 if AGGRESSIVE else 0})
    elif function == 14:
        options = prompt.get("options") or []
        send(proc, {"cmd": "response", "value": options[0]["index"] if options else 0})
    elif function == 15:
        cards = prompt.get("cards") or []
        minimum = max(1, int(prompt.get("min", 1)))
        count = min(len(cards), minimum)
        respond_bytes(proc, bytes([count, *range(count)]))
    elif function == 16:
        # 连锁：主动应答（选第一个候选）才能制造对方回合的应答窗口
        cards = prompt.get("chainCards") or []
        send(proc, {"cmd": "response", "value": 0 if (AGGRESSIVE and cards) else -1})
    elif function in (18, 24):
        places = prompt.get("places") or []
        if not places:
            raise RuntimeError(f"no legal places: {prompt}")
        place = places[0]
        respond_bytes(proc, bytes([place["player"], place["location"], place["sequence"]]))
    elif function == 19:
        options = prompt.get("options") or []
        send(proc, {"cmd": "response", "value": options[0]["index"] if options else 1})
    elif function == 20:
        # SelectTribute：交出前 min 张（不够就取消）
        cards = prompt.get("cards") or []
        minimum = int(prompt.get("min", 1))
        if len(cards) < minimum:
            send(proc, {"cmd": "response", "value": -1})
        else:
            respond_bytes(proc, bytes([minimum, *range(minimum)]))
    elif function == 22:
        # SelectCounter：把 quantity 个指示物全给第一张
        quantity = int(prompt.get("quantity", 0))
        count = int(prompt.get("count", 0))
        if count <= 0:
            respond_bytes(proc, bytes([0]))
        else:
            values = [0] * count
            values[0] = quantity & 0xFF
            respond_bytes(proc, bytes(values))
    elif function == 23:
        # SelectSum：先交必选的 must 张
        must = int(prompt.get("must", 0))
        respond_bytes(proc, bytes([must, *range(must)]))
    elif function == 25:
        # SortCard：恒等排列
        count = int(prompt.get("count", 0))
        respond_bytes(proc, bytes(range(count)))
    elif function == 26:
        # SelectUnselectCard：能取消就取消，否则点完成
        send(proc, {"cmd": "response", "value": -1 if prompt.get("cancelable") else 1})
    elif function == 142:
        candidates = prompt.get("candidateCodes") or []
        if not candidates:
            raise RuntimeError(f"AnnounceCard has no candidates: {prompt}")
        send(proc, {"cmd": "response", "value": candidates[0]})
    elif function == 143:
        raw = bytes.fromhex(prompt.get("raw") or "")
        if len(raw) < 3:
            raise RuntimeError(f"AnnounceNumber payload truncated: {prompt}")
        count = raw[2]
        values = [int.from_bytes(raw[3 + i * 4:7 + i * 4], "little") for i in range(count)]
        if not values:
            raise RuntimeError(f"AnnounceNumber has no values: {prompt}")
        send(proc, {"cmd": "response", "value": values[0]})
    else:
        raise RuntimeError(f"unsupported prompt function {function}: {prompt.get('summary')}")


def run(seed):
    env = os.environ.copy()
    # 出厂那份是自包含布局（runtime 就在 exe 旁边）；本地构建出来的要靠 DOTNET_ROOT 找 runtime
    env["DOTNET_ROOT"] = os.environ.get("MDPRO_DOTNET_ROOT") or os.path.dirname(HOST)
    root = MASTER_ROOT if MODE == "master" else ROOT
    deck = MASTER_DECK if MODE == "master" else DECK
    proc = subprocess.Popen(
        [HOST, "--root", root],
        cwd=root,
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
    turn = 0
    turn_hexes = []
    records = []
    missing = 0
    try:
        for line in proc.stdout:
            if time.time() > deadline:
                break
            try:
                event = json.loads(line)
            except json.JSONDecodeError:
                continue
            kind = event.get("type")
            if kind == "ready":
                send(proc, {
                    "cmd": "start",
                    "deck0": deck,
                    "deck1": deck,
                    "auto": False,
                    "mode": MODE,
                    "ai": USE_AI,
                    "aiSeat": 1,
                    "humanSeat1": HUMAN_SEAT1,
                    "aiName": "AI_OldSchool",
                    "aiDeck": "AI_OldSchool",
                    "aiDeckFile": DECK,
                    "aiDialog": "cirno.zh-CN",
                    "timing": "default",
                    "seed": seed,
                })
            elif kind == "message":
                # NewTurn 的 hex 第二个字节是回合玩家
                if event.get("name") == "NewTurn":
                    hexv = event.get("hex")
                    if isinstance(hexv, str) and len(hexv) >= 6:
                        turn = bytes.fromhex(hexv)[1] & 1
                        if len(turn_hexes) < 8:
                            turn_hexes.append(hexv)
            elif kind == "prompt":
                function = event.get("function")
                if "player" in event:
                    records.append({
                        "function": function,
                        "name": event.get("name"),
                        "player": event["player"],
                        "turn": turn,
                        "cross": event["player"] != turn,
                    })
                else:
                    missing += 1
                    records.append({
                        "function": function,
                        "name": event.get("name"),
                        "player": None,
                        "turn": turn,
                        "cross": None,
                    })
                answer_prompt(proc, event)
            elif kind == "ended":
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
    return records, missing, turn_hexes


def main():
    if not os.path.exists(DECK):
        print("deck not found:", DECK)
        return 2
    total = 0
    missing_total = 0
    cross_total = 0
    by_function = {}
    for seed in SEEDS:
        records, missing, turn_hexes = run(seed)
        if seed == SEEDS[0]:
            print("  NewTurn hex 样例:", turn_hexes[:6])
        total += len(records)
        missing_total += missing
        for r in records:
            if r["cross"]:
                cross_total += 1
            key = f'{r["function"]}:{r["name"]}'
            stat = by_function.setdefault(key, {"count": 0, "missingPlayer": 0, "cross": 0, "players": set()})
            stat["count"] += 1
            if r["player"] is None:
                stat["missingPlayer"] += 1
            else:
                stat["players"].add(r["player"])
                if r["cross"]:
                    stat["cross"] += 1
        print(f"seed={seed} prompts={len(records)} missingPlayer={missing} cross={sum(1 for r in records if r['cross'])}")
    print()
    print(f"总提示 {total}，缺 player 的 {missing_total}，player != 回合玩家的 {cross_total}")
    print()
    for key in sorted(by_function):
        stat = by_function[key]
        print(f'  {key:28s} n={stat["count"]:4d} 缺player={stat["missingPlayer"]:3d} '
              f'跨回合={stat["cross"]:3d} 出现过的席位={sorted(stat["players"])}')
    return 0


if __name__ == "__main__":
    sys.exit(main())
