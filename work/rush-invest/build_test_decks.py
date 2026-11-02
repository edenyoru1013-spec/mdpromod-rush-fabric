"""Rush Duel 测试卡组生成器 + 合法性校验。

校验规则完全复刻 net.mdpromod.DeckRules.problems(deck, RUSH)：
  主 40..60 / 额外 <=15 / 副 <=15
  Rush 模式下每张卡 <=3（按 alias 分组）
  非 Rush 卡不能在 Rush 卡组里
  传说卡：每张 1 张，且传说怪兽 / 魔法 / 陷阱各最多 1 种
  额外卡组专用卡不能进主卡组（融合 / 同调 / 超量 / 连接）
  Maximum(type&0x8000) 不属于额外卡组，可以进主卡组
"""
import json
import sqlite3
import collections

ROOT = r"D:\codexdate\2026-10-30\zhe\work"
CARDS = json.load(open(ROOT + r"\full\rush-overlay\data\mdpromod\rush_cards.json", encoding="utf-8"))
CDB = sqlite3.connect(ROOT + r"\rush-build\runtime-fixed\cards.cdb")
NAMES = {r[0]: r[1] for r in CDB.execute("select id,name from texts")}
DESC = {r[0]: (r[2] or "") for r in CDB.execute("select id,name,desc from texts")}

M, S, T = 0x1, 0x2, 0x4
NORMAL, EFFECT = 0x10, 0x20
FUSION, SYNCHRO, XYZ, LINK = 0x40, 0x2000, 0x800000, 0x4000000
RITUAL, QUICK, CONT, EQUIP, FIELD, COUNTER = 0x80, 0x10000, 0x20000, 0x40000, 0x80000, 0x100000
MAXIMUM = 0x8000
# 衍生物 / 陷阱怪兽不能作为卡组里的普通怪兽来召唤，挑卡时排除
TOKEN, TRAPMONSTER = 0x4000, 0x100


def info(code):
    return CARDS.get(str(code))


def name(code):
    v = info(code)
    return NAMES.get(code) or (v["name"] if v else "#%d" % code)


def is_legend(code):
    return "<传说卡>" in DESC.get(code, "")


def is_extra(code):
    v = info(code)
    return bool(v and v["type"] & (FUSION | SYNCHRO | XYZ | LINK))


def check(main, extra=(), side=()):
    """复刻 DeckRules.problems(deck, RUSH)，返回问题列表。"""
    issues = []
    allc = list(main) + list(extra) + list(side)
    if not (40 <= len(main) <= 60):
        issues.append("主卡组 %d 张，应在 40..60" % len(main))
    if len(extra) > 15:
        issues.append("额外卡组超过 15 张")
    if len(side) > 15:
        issues.append("副卡组超过 15 张")

    def key(c):
        v = info(c)
        return v["alias"] if v and v["alias"] else c

    groups = collections.Counter(key(c) for c in allc)
    sample = {}
    for c in allc:
        sample.setdefault(key(c), c)
    for g, n in groups.items():
        if n > 3:
            issues.append("%s 最多 3 张，当前 %d 张" % (name(sample[g]), n))

    for c in allc:
        if not info(c):
            issues.append("%d 不是 Rush Duel 卡" % c)

    legend_type = [0, 0, 0]
    seen = set()
    for c in allc:
        v = info(c)
        if not v or not is_legend(c):
            continue
        if c not in seen:
            seen.add(c)
            copies = allc.count(c)
            if copies > 1:
                issues.append("传说卡「%s」最多 1 张，当前 %d 张" % (name(c), copies))
            t = v["type"]
            if t & M:
                legend_type[0] += 1
            elif t & S:
                legend_type[1] += 1
            elif t & T:
                legend_type[2] += 1
    labels = ["传说怪兽卡", "传说魔法卡", "传说陷阱卡"]
    for i, n in enumerate(legend_type):
        if n > 1:
            issues.append("%s最多 1 种，当前 %d 种" % (labels[i], n))

    for c in main:
        if is_extra(c):
            issues.append("%s 是额外卡组专用，不能放进主卡组" % name(c))
    for c in extra:
        v = info(c)
        if v and not is_extra(c):
            issues.append("%s 不能放进额外卡组" % name(c))
    return issues


def pool():
    """按类型把卡池分类，全部排除传说卡与额外卡组专用卡。"""
    vanilla, effect, normal_spell, normal_trap, maximum = [], [], [], [], []
    for c, v in CARDS.items():
        code = int(c)
        t, lv = v["type"], v["level"]
        if is_legend(code) or is_extra(code):
            continue
        if desc := DESC.get(code):
            pass
        if t & M:
            if lv <= 4 and not (t & (FUSION | TOKEN | TRAPMONSTER)):
                row = (code, name(code), lv, v["atk"], v["def"], bool(t & EFFECT))
                (effect if t & EFFECT else vanilla).append(row)
        elif t & S and not (t & (RITUAL | QUICK | CONT | EQUIP | FIELD)):
            normal_spell.append((code, name(code)))
        elif t & T and not (t & (CONT | COUNTER)):
            normal_trap.append((code, name(code)))
    return vanilla, effect, normal_spell, normal_trap


if __name__ == "__main__":
    vanilla, effect, nspell, ntrap = pool()
    print("可用通常怪兽(<=4星,非传说) %d" % len(vanilla))
    print("可用效果怪兽(<=4星,非传说) %d" % len(effect))
    print("可用通常魔法(非传说) %d" % len(nspell))
    print("可用通常陷阱(非传说) %d" % len(ntrap))
