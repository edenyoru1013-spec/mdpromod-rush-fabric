"""把 D:\\Game\\MDPro3\\Deck 里能通过模组校验的 Rush 卡组装进游戏实例。

目标目录严格取自 ModPaths.deckDir()（=<实例 config>/mdpromod/decks），
也就是 /mdpromod case give <名字> 的第一个搜索路径。

只装能通过 DeckRules.problems(deck, RUSH) 的卡组；文件名里的空格替换成下划线，
因为 case give 的参数是 StringArgumentType.word()，带空格的名字在命令里拼不出来。
"""
import json
import os
import shutil
import sys
import hashlib

sys.path.insert(0, r"D:\codexdate\2026-10-30\zhe\work\rush-invest")
import scan_mdpro3_decks as S
import build_test_decks as B

SRC = r"D:\Game\MDPro3\Deck"
DST = r"D:\QQ\聊天数据\pcl\.minecraft\versions\1.20.1-Fabric 0.16.14\config\mdpromod\decks"
MANIFEST = None  # 运行时填


def safe_name(fn):
    base = fn[:-4]
    # 超长 MDPro3 导出名（base64）在命令里没法用，取分号后面那段有意义的短名
    if len(base) > 40 and ";" in base:
        base = base.split(";")[-1].strip()
    if not base:
        base = "imported"
    return base.replace(" ", "_").replace("　", "_") + ".ydk"


def sha256(path):
    return hashlib.sha256(open(path, "rb").read()).hexdigest()


def main():
    os.makedirs(DST, exist_ok=True)
    files = sorted(p for p in os.listdir(SRC) if p.lower().endswith(".ydk"))
    installed, skipped = [], []
    for fn in files:
        m, e, s = S.parse(os.path.join(SRC, fn))
        pool = S.pool_of(m + e + s)
        if pool != "RUSH":
            skipped.append((fn, pool, ["混池" if pool == "MIXED" else pool]))
            continue
        issues = B.check(m, e, s)
        if issues:
            skipped.append((fn, "RUSH", issues))
            continue
        target = os.path.join(DST, safe_name(fn))
        shutil.copyfile(os.path.join(SRC, fn), target)
        installed.append((fn, os.path.basename(target), len(m), len(e), len(s)))

    print("已安装 %d / 跳过 %d" % (len(installed), len(skipped)))
    print("目标目录：%s" % DST)
    print()
    # 复制后逐个重新校验，确保落盘内容与源一致且仍然合法
    bad = []
    for fn, target, mn, en, sn in installed:
        path = os.path.join(DST, target)
        if sha256(path) != sha256(os.path.join(SRC, fn)):
            bad.append((target, "内容与源不一致"))
            continue
        m, e, s = S.parse(path)
        if (len(m), len(e), len(s)) != (mn, en, sn):
            bad.append((target, "重新解析张数不符"))
            continue
        issues = B.check(m, e, s)
        if issues:
            bad.append((target, "; ".join(issues[:2])))
    print("安装后复校：共 %d 个，异常 %d 个" % (len(installed), len(bad)))
    for t, why in bad:
        print("   !", t, why)
    print()
    print("=== 跳过的卡组（无法在游戏中开局）===")
    for fn, pool, issues in skipped:
        print("  %-38s %s" % (fn, "; ".join(issues[:2]) if isinstance(issues, list) else pool))
    json.dump(
        {"dst": DST, "installed": installed, "skipped": [x[0] for x in skipped]},
        open(r"D:\codexdate\2026-10-30\zhe\work\rush-invest\installed-decks.json", "w", encoding="utf-8"),
        ensure_ascii=False, indent=1,
    )


if __name__ == "__main__":
    main()
