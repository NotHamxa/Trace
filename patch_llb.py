#!/usr/bin/env python
"""Patch the serialVersionUID of com.trace.model.Wire inside .trc files to 1L."""
import sys, os

NEW_UID = (1).to_bytes(8, "big", signed=True)
NAME = b"com.trace.model.Wire"
# TC_CLASSDESC = 0x72, then UTF: 2-byte length, then name, then 8-byte serialVersionUID
PATTERN = bytes([0x72, 0x00, len(NAME)]) + NAME

def patch(path):
    with open(path, "rb") as f:
        data = bytearray(f.read())
    i = 0
    count = 0
    while True:
        idx = data.find(PATTERN, i)
        if idx < 0:
            break
        uid_off = idx + len(PATTERN)
        old = bytes(data[uid_off:uid_off+8])
        data[uid_off:uid_off+8] = NEW_UID
        count += 1
        print(f"  patched UID at offset {uid_off}: {old.hex()} -> {NEW_UID.hex()}")
        i = uid_off + 8
    if count:
        bak = path + ".bak"
        if not os.path.exists(bak):
            with open(bak, "wb") as f:
                f.write(open(path, "rb").read())
        with open(path, "wb") as f:
            f.write(data)
        print(f"{path}: {count} descriptor(s) patched (backup at {bak})")
    else:
        print(f"{path}: no Wire class descriptor found")

for p in sys.argv[1:]:
    patch(p)
