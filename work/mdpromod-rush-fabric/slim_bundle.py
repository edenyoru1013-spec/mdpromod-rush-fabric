"""把 601MB 的完整 JAR 裁剪成只含 Rush Duel 资源的精简版。

三步合一，一次性产出可直接安装的成品：
1. 剔除 Master 专属资源（art / closeup / runtime / cards.json）
2. 用 Rush-only 的新 class 替换 net/mdpromod/**
3. 重写 manifest.txt（ModBundle 靠它逐条解包，必须与产物完全自洽）
"""
import os
import sys
import zipfile

ROOT = r"D:\codexdate\2026-10-30\zhe"
WORK = ROOT + r"\work"
# 源完整 JAR 在发行目录里，不在 work 下
SRC = ROOT + r"\outputs\mdpromod-fabric-0.1.0\mdpromod-0.1.0-fabric-full.jar"
DEV = WORK + r"\mdpromod-rush-fabric\build\libs\mdpromod-0.1.0-fabric.jar"
DST_DIR = WORK + r"\mdpromod-rush-fabric\full"
DST = DST_DIR + r"\mdpromod-0.1.0-fabric-full.jar"

BUNDLE = "data/mdpromod/bundle/"
# Master 专属：这些 Rush 一条都用不到
DROP_PREFIXES = (
    BUNDLE + "art/",
    BUNDLE + "closeup/",
    BUNDLE + "runtime/",
)
DROP_EXACT = (
    "data/mdpromod/cards.json",
)
MANIFEST = BUNDLE + "manifest.txt"
CODE_PREFIX = "net/mdpromod/"
# 宿主（duel_host.dll）不走 Gradle：改了 Program.cs 就得在这里换进去，
# 否则首次启动解包出来的还是旧的，修好的行为不会生效。
HOST_DLL = WORK + r"\mdpro-repo\tools\duel_host\bin\Release\net8.0\duel_host.dll"
HOST_ENTRY = BUNDLE + "duelhost/duel_host.dll"
HOST_MANIFEST_PATH = "duelhost/duel_host.dll"


def dropped(name):
    return name.startswith(DROP_PREFIXES) or name in DROP_EXACT


def main():
    os.makedirs(DST_DIR, exist_ok=True)
    src = zipfile.ZipFile(SRC)
    dev = zipfile.ZipFile(DEV)
    dev_items = {i.filename: i for i in dev.infolist()}

    # 源 jar 里 net/mdpromod 的条目和新 dev jar 的差集：新版本删掉的就是该删的
    old_code = {i.filename for i in src.infolist() if i.filename.startswith(CODE_PREFIX)}
    new_code = {n for n in dev_items if n.startswith(CODE_PREFIX)}
    removed_code = sorted(old_code - new_code)

    stats = {"kept": 0, "dropped": 0, "code_replaced": 0, "code_added": 0}
    kept_from_dev = set()

    with zipfile.ZipFile(DST, "w", zipfile.ZIP_DEFLATED, compresslevel=6) as out:
        manifest_lines = []
        for item in src.infolist():
            name = item.filename
            if dropped(name):
                stats["dropped"] += 1
                continue
            if name == MANIFEST:
                # 攒到下一阶段重写
                raw = src.read(name).decode("utf-8")
                for line in raw.splitlines():
                    if not line.strip():
                        continue
                    path = line.split("\t")[0]
                    if dropped(BUNDLE + path):
                        stats["dropped"] += 1
                        continue
                    manifest_lines.append(line)
                continue
            if name.startswith(CODE_PREFIX):
                continue          # 代码统一走 dev jar，下面单独写
            if name == HOST_ENTRY:
                continue          # 宿主走新构建出来的那份，下面单独写
            out.writestr(name, src.read(name))
            stats["kept"] += 1

        # 代码：全部来自新的 Rush-only dev jar
        for info in dev.infolist():
            if not info.filename.startswith(CODE_PREFIX):
                continue
            out.writestr(info.filename, dev.read(info.filename))
            kept_from_dev.add(info.filename)
            if info.filename in old_code:
                stats["code_replaced"] += 1
            else:
                stats["code_added"] += 1

        # 宿主：换成新构建的那份（大小变了，manifest 里的字节数要跟着改）
        if os.path.exists(HOST_DLL):
            with open(HOST_DLL, "rb") as handle:
                host_bytes = handle.read()
            out.writestr(HOST_ENTRY, host_bytes)
            stats["kept"] += 1
            stats["host_bytes"] = len(host_bytes)
            manifest_lines = [
                line if line.split("\t")[0] != HOST_MANIFEST_PATH
                else HOST_MANIFEST_PATH + "\t" + str(len(host_bytes))
                for line in manifest_lines
            ]
        else:
            print("!! 找不到宿主 dll，沿用包内的旧份：%s" % HOST_DLL)

        out.writestr(MANIFEST, "\n".join(manifest_lines) + "\n")
        stats["kept"] += 1

    src.close()
    dev.close()
    print("== 裁剪完成 ==")
    print("  删除 Master 资源条目 : %d" % stats["dropped"])
    print("  保留非代码条目       : %d" % stats["kept"])
    print("  替换的旧 class       : %d" % stats["code_replaced"])
    print("  新引入的 class       : %d" % stats["code_added"])
    print("  删掉的旧 class       : %d -> %s" % (len(removed_code), removed_code))
    if "host_bytes" in stats:
        print("  宿主 dll 字节数      : {:,}".format(stats["host_bytes"]))
    print("  产物大小             : {:,} B".format(os.path.getsize(DST)))


if __name__ == "__main__":
    main()
