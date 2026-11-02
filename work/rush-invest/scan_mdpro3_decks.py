"""扫描 D:\\Game\\MDPro3\\Deck 下的 ydk，按 Rush / Master 分类并做合法性校验。

Dry-run 模式（默认）：只报告，不复制。
"""
import json
import os
import sys
import collections

sys.path.insert(0, r"D:\codexdate\2026-10-30\zhe\work\rush-invest")
import build_test_decks as B

SRC = r"D:\Game\MDPro3\Deck"
RUSH = set(int(c) for c in B.CARDS)


def parse(path):
    main, extra, side = [], [], []
    section = 0
    for raw in open(path, encoding="utf-8", errors="replace"):
        line = raw.strip()
        if not line:
            continue
        low = line.lower()
        if low == "#main":
            section = 0
            continue
        if low == "#extra":
            section = 1
            continue
        if low == "!side":
            section = 2
            continue
        if line.startswith("#") or line.startswith("!"):
            continue
        try:
            code = int(line)
        except ValueError:
            continue
        (main, extra, side)[section].append(code)
    return main, extra, side


def pool_of(codes):
    n = len(codes)
    if n == 0:
        return "EMPTY"
    rush = sum(1 for c in codes if c in RUSH)
    if rush == n:
        return "RUSH"
    if rush == 0:
        return "MASTER"
    return "MIXED"


def known(codes):
    """Master 侧我们不掌握完整卡池，这里只标未知数量。"""
    return sum(1 for c in codes if c in RUSH or B.info(c))


def main():
    files = sorted(p for p in os.listdir(SRC) if p.lower().endswith(".ydk"))
    rush_ok, rush_bad, master_deck, mixed, empty = [], [], [], [], []
    for fn in files:
        mainsec, extra, side = parse(os.path.join(SRC, fn))
        pool = pool_of(mainsec + extra + side)
        if pool == "EMPTY":
            empty.append(fn)
        elif pool == "RUSH":
            issues = B.check(mainsec, extra, side)
            (rush_ok if not issues else rush_bad).append((fn, len(mainsec), len(extra), len(side), issues))
        elif pool == "MASTER":
            master_deck.append((fn, len(mainsec), len(extra), len(side)))
        else:
            mixed.append(fn)

    print("总文件 %d" % len(files))
    print("Rush 且合法       : %d" % len(rush_ok))
    print("Rush 但不合法     : %d" % len(rush_bad))
    print("大师卡组          : %d" % len(master_deck))
    print("混池(两侧都有)    : %d" % len(mixed))
    print("空文件            : %d" % len(empty))
    print()
    print("=== Rush 合法卡组（可直接在 Rush 模式使用） ===")
    for fn, m, e, s, _ in rush_ok:
        print("  %-40s main=%d extra=%d side=%d" % (fn, m, e, s))
    if rush_bad:
        print()
        print("=== Rush 卡组但不合法（会被开局校验拦下） ===")
        for fn, m, e, s, iss in rush_bad:
            print("  %s (main=%d)" % (fn, m))
            for i in iss[:4]:
                print("      - %s" % i)
            if len(iss) > 4:
                print("      - ...共 %d 条" % len(iss))
    if mixed:
        print()
        print("=== 混池卡组 ===")
        for fn in mixed[:20]:
            print("  " + fn)


if __name__ == "__main__":
    main()
