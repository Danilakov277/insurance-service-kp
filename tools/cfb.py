"""Минимальное чтение OLE/CFB-файлов (Altium .SchDot/.SchDoc) без внешних библиотек."""
import struct


class CFB:
    def __init__(self, path):
        self.d = open(path, "rb").read()
        h = self.d[:512]
        assert h[:8] == bytes.fromhex("D0CF11E0A1B11AE1"), "не CFB"
        self.ss = 1 << struct.unpack_from("<H", h, 0x1E)[0]
        self.mss = 1 << struct.unpack_from("<H", h, 0x20)[0]
        nfat = struct.unpack_from("<I", h, 0x2C)[0]
        self.dir_start = struct.unpack_from("<I", h, 0x30)[0]
        self.cutoff = struct.unpack_from("<I", h, 0x38)[0]
        minifat_start = struct.unpack_from("<I", h, 0x3C)[0]
        difat_start, ndifat = struct.unpack_from("<II", h, 0x44)
        difat = list(struct.unpack_from("<109I", h, 0x4C))
        s = difat_start
        for _ in range(ndifat):
            sec = self._sec(s)
            vals = struct.unpack("<%dI" % (self.ss // 4), sec)
            difat += vals[:-1]
            s = vals[-1]
        self.fat = []
        for s in difat[:nfat]:
            self.fat += struct.unpack("<%dI" % (self.ss // 4), self._sec(s))
        self.minifat = []
        if minifat_start < 0xFFFFFFFA:
            data = self._chain(minifat_start)
            self.minifat = list(struct.unpack("<%dI" % (len(data) // 4), data))
        dirdata = self._chain(self.dir_start)
        self.entries = []
        for i in range(len(dirdata) // 128):
            e = dirdata[i * 128:(i + 1) * 128]
            nl = struct.unpack_from("<H", e, 64)[0]
            name = e[:max(nl - 2, 0)].decode("utf-16le")
            typ = e[66]
            start, size = struct.unpack_from("<IQ", e, 116)
            self.entries.append((name, typ, start, size))
        root = self.entries[0]
        self.ministream = self._chain(root[2])[:root[3]]

    def _sec(self, n):
        o = 512 + n * self.ss
        return self.d[o:o + self.ss]

    def _chain(self, s):
        out = []
        while s < 0xFFFFFFFA:
            out.append(self._sec(s))
            s = self.fat[s]
        return b"".join(out)

    def _minichain(self, s):
        out = []
        while s < 0xFFFFFFFA:
            out.append(self.ministream[s * self.mss:(s + 1) * self.mss])
            s = self.minifat[s]
        return b"".join(out)

    def names(self):
        return [e[0] for e in self.entries if e[1] == 2]

    def stream(self, name):
        for n, typ, start, size in self.entries:
            if n == name and typ == 2:
                data = self._chain(start) if size >= self.cutoff else self._minichain(start)
                return data[:size]
        raise KeyError(name)


def altium_records(data):
    """Записи Altium: [длина 3 байта][тип 1 байт] затем |KEY=VALUE|... ."""
    recs, i = [], 0
    while i + 4 <= len(data):
        ln = int.from_bytes(data[i:i + 3], "little")
        payload = data[i + 4:i + 4 + ln].rstrip(b"\0")
        i += 4 + ln
        d = {}
        for part in payload.split(b"|"):
            if b"=" in part:
                k, v = part.split(b"=", 1)
                d[k.decode("latin1").upper()] = v
        recs.append(d)
    return recs
