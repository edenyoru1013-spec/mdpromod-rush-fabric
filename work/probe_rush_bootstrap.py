"""Probe Rush Lua bootstrap behavior against the bundled ocgcore."""

from __future__ import annotations

import argparse
import ctypes as c
import json
import zipfile
from pathlib import Path


class CardData(c.Structure):
    _fields_ = [
        ("code", c.c_uint32),
        ("alias", c.c_uint32),
        ("setcode", c.c_uint16 * 16),
        ("type", c.c_uint32),
        ("level", c.c_uint32),
        ("attribute", c.c_uint32),
        ("race", c.c_uint32),
        ("attack", c.c_int32),
        ("defense", c.c_int32),
        ("lscale", c.c_uint32),
        ("rscale", c.c_uint32),
        ("link_marker", c.c_uint32),
    ]


READ_SCRIPT = c.CFUNCTYPE(c.c_void_p, c.c_char_p, c.POINTER(c.c_int32))
READ_CARD = c.CFUNCTYPE(c.c_uint32, c.c_uint32, c.POINTER(CardData))
MESSAGE = c.CFUNCTYPE(c.c_uint32, c.c_void_p, c.c_uint32)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--root",
        type=Path,
        default=Path(r"D:\codexdate\2026-10-30\zhe\work\rush-build\runtime-fixed"),
    )
    args = parser.parse_args()
    root = args.root.resolve()
    core = c.CDLL(str(root / "ocgcore.dll"))

    with zipfile.ZipFile(root / "script.zip") as archive:
        scripts = {
            name.replace("\\", "/").removeprefix("./"): archive.read(name)
            for name in archive.namelist()
            if name.lower().endswith(".lua")
        }
    buffers = {
        name: c.create_string_buffer(content)
        for name, content in scripts.items()
    }
    read_names: list[str] = []
    logs: list[dict[str, object]] = []
    duel = c.c_void_p()

    @READ_SCRIPT
    def read_script(name: bytes | None, length: c.POINTER(c.c_int32)) -> int:
        key = (name or b"").decode("utf-8", errors="replace").replace("\\", "/").removeprefix("./")
        read_names.append(key)
        buffer = buffers.get(key) or buffers.get(f"script/{key}")
        if buffer is None:
            length[0] = 0
            return 0
        length[0] = len(buffer) - 1
        return c.addressof(buffer)

    @READ_CARD
    def read_card(code: int, output: c.POINTER(CardData)) -> int:
        c.memset(output, 0, c.sizeof(CardData))
        return 0

    @MESSAGE
    def on_message(duel_ptr: int, kind: int) -> int:
        message = c.create_string_buffer(16 * 1024)
        core.get_log_message(duel_ptr, message)
        logs.append(
            {
                "kind": kind,
                "message": message.value.decode("utf-8", errors="replace"),
                "readNames": list(read_names),
            }
        )
        read_names.clear()
        return 0

    core.set_script_reader.argtypes = [READ_SCRIPT]
    core.set_script_reader.restype = None
    core.set_card_reader.argtypes = [READ_CARD]
    core.set_card_reader.restype = None
    core.set_message_handler.argtypes = [MESSAGE]
    core.set_message_handler.restype = None
    core.create_duel.argtypes = [c.c_uint32]
    core.create_duel.restype = c.c_void_p
    core.preload_script.argtypes = [c.c_void_p, c.c_char_p]
    core.preload_script.restype = c.c_int32
    core.start_duel.argtypes = [c.c_void_p, c.c_uint32]
    core.start_duel.restype = None
    core.process.argtypes = [c.c_void_p]
    core.process.restype = c.c_uint32
    core.end_duel.argtypes = [c.c_void_p]
    core.end_duel.restype = None
    core.get_log_message.argtypes = [c.c_void_p, c.c_void_p]
    core.get_log_message.restype = None

    core.set_script_reader(read_script)
    core.set_card_reader(read_card)
    core.set_message_handler(on_message)

    results: list[dict[str, object]] = []
    for name, source, stage in (
        ("special.lua", None, "pre"),
        ("script/special.lua", None, "pre"),
        ("rush-base-probe.lua", b'assert(type(utility) == "table", "utility missing")', "pre"),
        (
            "utility-probe.lua",
            b'assert(Duel.LoadScript("utility.lua"), "load utility failed") '
            b'assert(type(Auxiliary) == "table", "Auxiliary missing")',
            "pre",
        ),
        (
            "probe-special.lua",
            b'local ok = Duel.LoadScript("special.lua") '
            b'assert(ok, "load special failed") '
            b'assert(type(Auxiliary) == "table", "Auxiliary missing") '
            b'assert(type(Auxiliary.PreloadUds) == "function", "PreloadUds missing")',
            "pre",
        ),
        (
            "probe-rush-init.lua",
            b'local ok = Duel.LoadScript("special.lua") '
            b'assert(ok, "load special failed") '
            b'Auxiliary.PreloadUds() '
            b'assert(type(RushDuel) == "table", "RushDuel missing") '
            b'assert(type(RD) == "table", "RD missing")',
            "pre",
        ),
        (
            "api-before-start.lua",
            b'assert(Duel.LoadScript ~= nil, "Duel.LoadScript missing") '
            b'assert(Duel.RegisterEffect ~= nil, "Duel.RegisterEffect missing") '
            b'assert(Effect ~= nil and Effect.GlobalEffect ~= nil, "Effect.GlobalEffect missing")',
            "pre",
        ),
        (
            "api-before-register.lua",
            b'assert(Duel.RegisterEffect ~= nil, "Duel.RegisterEffect missing")',
            "pre",
        ),
        (
            "api-before-effect.lua",
            b'assert(Effect ~= nil, "Effect missing") '
            b'assert(Effect.GlobalEffect ~= nil, "Effect.GlobalEffect missing")',
            "pre",
        ),
        (
            "api-before-auxiliary.lua",
            b'assert(Auxiliary ~= nil, "Auxiliary missing") '
            b'assert(Auxiliary.Stringid ~= nil, "Auxiliary.Stringid missing")',
            "pre",
        ),
        ("special-after-start.lua", None, "start"),
        (
            "api-after-start.lua",
            b'assert(Duel.LoadScript ~= nil, "Duel.LoadScript missing") '
            b'assert(Duel.RegisterEffect ~= nil, "Duel.RegisterEffect missing") '
            b'assert(Effect ~= nil and Effect.GlobalEffect ~= nil, "Effect.GlobalEffect missing")',
            "start",
        ),
        (
            "probe-rush-init-after-start.lua",
            b'local ok = Duel.LoadScript("special.lua") '
            b'assert(ok, "load special failed") '
            b'assert(type(Auxiliary.PreloadUds) == "function", "PreloadUds missing") '
            b'Auxiliary.PreloadUds() '
            b'assert(type(RushDuel) == "table", "RushDuel missing") '
            b'assert(type(RD) == "table", "RD missing")',
            "start",
        ),
        ("special-after-process.lua", None, "process"),
        (
            "api-after-process.lua",
            b'assert(Duel.LoadScript ~= nil, "Duel.LoadScript missing") '
            b'assert(Duel.RegisterEffect ~= nil, "Duel.RegisterEffect missing") '
            b'assert(Effect ~= nil and Effect.GlobalEffect ~= nil, "Effect.GlobalEffect missing")',
            "process",
        ),
        (
            "probe-rush-init-after-process.lua",
            b'local ok = Duel.LoadScript("special.lua") '
            b'assert(ok, "load special failed") '
            b'assert(type(Auxiliary.PreloadUds) == "function", "PreloadUds missing") '
            b'Auxiliary.PreloadUds() '
            b'assert(type(RushDuel) == "table", "RushDuel missing") '
            b'assert(type(RD) == "table", "RD missing")',
            "process",
        ),
    ):
        duel = c.c_void_p(core.create_duel(20261031))
        if not duel:
            raise RuntimeError("create_duel returned null")
        read_names.clear()
        logs.clear()
        if source is not None:
            buffers[f"script/{name}"] = c.create_string_buffer(source)
            name = f"./script/{name}"
        try:
            if stage in ("start", "process"):
                core.start_duel(duel, 0)
            if stage == "process":
                process_result = core.process(duel)
            else:
                process_result = None
            result = core.preload_script(duel, name.encode("utf-8"))
        finally:
            core.end_duel(duel)
        results.append(
            {
                "name": name,
                "stage": stage,
                "processResult": process_result,
                "result": result,
                "readNames": list(read_names),
                "logs": list(logs),
            }
        )

    duel = c.c_void_p(core.create_duel(20261031))
    if not duel:
        raise RuntimeError("create_duel returned null for sequential probe")
    read_names.clear()
    logs.clear()
    sequential_results: list[dict[str, object]] = []
    try:
        for name in (
            "RDBase.lua",
            "RDLegend.lua",
            "RDRule.lua",
            "RDMaximum.lua",
            "RDFunction.lua",
            "RDCondition.lua",
            "RDCost.lua",
            "RDTarget.lua",
            "RDValue.lua",
            "RDContinuous.lua",
            "RDAttach.lua",
            "RDLimit.lua",
            "RDAction.lua",
            "RDFusion.lua",
            "RDRitual.lua",
            "RDEquip.lua",
            "RDSummon.lua",
            "RDUnion.lua",
        ):
            before_logs = len(logs)
            result = core.preload_script(duel, name.encode("utf-8"))
            sequential_results.append(
                {
                    "name": name,
                    "result": result,
                    "readNames": list(read_names),
                    "logs": list(logs[before_logs:]),
                }
            )
            read_names.clear()
        bootstrap = b"RD = RushDuel; Auxiliary.PreloadUds()"
        buffers["script/rush-bootstrap-sequential.lua"] = c.create_string_buffer(bootstrap)
        before_logs = len(logs)
        result = core.preload_script(duel, b"rush-bootstrap-sequential.lua")
        sequential_results.append(
            {
                "name": "rush-bootstrap-sequential.lua",
                "result": result,
                "readNames": list(read_names),
                "logs": list(logs[before_logs:]),
            }
        )
        direct_bootstrap = (
            b'RD = RushDuel '
            b'assert(type(RD) == "table", "RushDuel missing") '
            b'assert(type(RD.Init) == "function", "RushDuel.Init missing") '
            b'RD.Init()'
        )
        buffers["script/rush-direct-init.lua"] = c.create_string_buffer(direct_bootstrap)
        before_logs = len(logs)
        result = core.preload_script(duel, b"rush-direct-init.lua")
        sequential_results.append(
            {
                "name": "rush-direct-init.lua",
                "result": result,
                "readNames": list(read_names),
                "logs": list(logs[before_logs:]),
            }
        )
    finally:
        core.end_duel(duel)

    print(json.dumps({"single": results, "sequential": sequential_results}, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
