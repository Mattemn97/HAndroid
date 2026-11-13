"""Broker MQTT 3.1.1 minimale in puro Python (asyncio), add-on di esempio di HAndroid.

Niente dipendenze: amqtt richiede psutil, argon2-cffi, PyYAML e websockets, che non
hanno wheel Android. Copre quello che serve a Home Assistant e ai dispositivi di casa:
QoS 0/1/2 in ingresso (consegna fino a QoS 1), messaggi retained, will, keepalive,
utente e password. Sessioni non persistenti (clean session).
"""

import asyncio
import json
import os
import struct

CONNECT, CONNACK, PUBLISH, PUBACK, PUBREC, PUBREL, PUBCOMP = 1, 2, 3, 4, 5, 6, 7
SUBSCRIBE, SUBACK, UNSUBSCRIBE, UNSUBACK, PINGREQ, PINGRESP, DISCONNECT = 8, 9, 10, 11, 12, 13, 14


def carica_opzioni():
    try:
        with open(os.environ.get("HANDROID_ADDON_OPTIONS", "opzioni.json"), encoding="utf-8") as fh:
            return json.load(fh)
    except FileNotFoundError:
        return {}


def corrisponde(filtro, topic):
    """Filtro MQTT con + e #; i topic che iniziano con $ non li prendono i caratteri jolly."""
    f, t = filtro.split("/"), topic.split("/")
    if topic.startswith("$") and f[0] in ("+", "#"):
        return False
    for i, parte in enumerate(f):
        if parte == "#":
            return True
        if i >= len(t) or (parte != "+" and parte != t[i]):
            return False
    return len(f) == len(t)


def stringa(buf, i):
    n = struct.unpack_from("!H", buf, i)[0]
    return buf[i + 2:i + 2 + n], i + 2 + n


def pacchetto(tipo, flag, corpo):
    n, lung = len(corpo), bytearray()
    while True:
        b, n = n % 128, n // 128
        lung.append(b | (0x80 if n else 0))
        if not n:
            break
    return bytes([tipo << 4 | flag]) + bytes(lung) + corpo


def utf8(s):
    b = s.encode() if isinstance(s, str) else s
    return struct.pack("!H", len(b)) + b


class Broker:
    def __init__(self, opzioni):
        self.utente = opzioni.get("username") or ""
        self.password = opzioni.get("password") or ""
        self.anonimi = opzioni.get("allow_anonymous", True)
        self.clienti = {}    # client id -> Cliente
        self.retained = {}   # topic -> (payload, qos)

    def autorizza(self, utente, password):
        if utente is None:
            return self.anonimi
        return not self.utente or (utente == self.utente and password == self.password)

    def pubblica(self, topic, payload, qos, retain):
        if retain:
            if payload:
                self.retained[topic] = (payload, qos)
            else:
                self.retained.pop(topic, None)
        for c in list(self.clienti.values()):
            q = c.qos_per(topic)
            if q is not None:
                c.invia_publish(topic, payload, min(q, qos, 1), False)


class Cliente:
    def __init__(self, broker, reader, writer):
        self.b, self.r, self.w = broker, reader, writer
        self.id = None
        self.sub = {}        # filtro -> qos
        self.will = None
        self.pid = 0
        self.qos2 = set()

    def qos_per(self, topic):
        qs = [q for f, q in self.sub.items() if corrisponde(f, topic)]
        return max(qs) if qs else None

    def invia(self, dati):
        if not self.w.is_closing():
            self.w.write(dati)

    def invia_publish(self, topic, payload, qos, retain):
        corpo = utf8(topic)
        if qos:
            self.pid = self.pid % 65535 + 1
            corpo += struct.pack("!H", self.pid)
        self.invia(pacchetto(PUBLISH, qos << 1 | int(retain), corpo + payload))

    async def leggi(self):
        h = (await self.r.readexactly(1))[0]
        n, mult = 0, 1
        for _ in range(4):
            b = (await self.r.readexactly(1))[0]
            n += (b & 0x7F) * mult
            mult *= 128
            if not b & 0x80:
                break
        return h >> 4, h & 0x0F, await self.r.readexactly(n)

    async def servi(self):
        tipo, _, corpo = await asyncio.wait_for(self.leggi(), 10)
        if tipo != CONNECT:
            return
        keepalive = await self.connetti(corpo)
        if keepalive is None:
            return
        pulito = False
        try:
            while True:
                attesa = keepalive * 1.5 if keepalive else None
                tipo, flag, corpo = await asyncio.wait_for(self.leggi(), attesa)
                if tipo == DISCONNECT:
                    pulito = True
                    return
                self.gestisci(tipo, flag, corpo)
                await self.w.drain()
        finally:
            if self.b.clienti.get(self.id) is self:
                del self.b.clienti[self.id]
            if not pulito and self.will:
                self.b.pubblica(*self.will)

    async def connetti(self, c):
        nome, i = stringa(c, 0)
        livello, flag = c[i], c[i + 1]
        keepalive = struct.unpack_from("!H", c, i + 2)[0]
        i += 4
        cid, i = stringa(c, i)
        if flag & 0x04:
            wt, i = stringa(c, i)
            wm, i = stringa(c, i)
            self.will = (wt.decode(), wm, (flag >> 3) & 3, bool(flag & 0x20))
        utente = password = None
        if flag & 0x80:
            utente, i = stringa(c, i)
            utente = utente.decode()
        if flag & 0x40:
            password, i = stringa(c, i)
            password = password.decode()
        if nome not in (b"MQTT", b"MQIsdp") or livello not in (3, 4):
            self.invia(pacchetto(CONNACK, 0, b"\x00\x01"))
            return None
        if not self.b.autorizza(utente, password):
            self.invia(pacchetto(CONNACK, 0, b"\x00\x05"))
            return None
        self.id = cid.decode() or f"anonimo-{id(self)}"
        vecchio = self.b.clienti.get(self.id)
        if vecchio:
            vecchio.w.close()
        self.b.clienti[self.id] = self
        self.invia(pacchetto(CONNACK, 0, b"\x00\x00"))
        return keepalive

    def gestisci(self, tipo, flag, c):
        if tipo == PUBLISH:
            qos, retain = (flag >> 1) & 3, bool(flag & 1)
            topic, i = stringa(c, 0)
            pid = None
            if qos:
                pid = c[i:i + 2]
                i += 2
            if qos == 1:
                self.invia(pacchetto(PUBACK, 0, pid))
            elif qos == 2:
                self.invia(pacchetto(PUBREC, 0, pid))
                if pid in self.qos2:
                    return
                self.qos2.add(pid)
            self.b.pubblica(topic.decode(), c[i:], qos, retain)
        elif tipo == PUBREL:
            self.qos2.discard(c[:2])
            self.invia(pacchetto(PUBCOMP, 0, c[:2]))
        elif tipo == PUBREC:
            self.invia(pacchetto(PUBREL, 2, c[:2]))
        elif tipo == SUBSCRIBE:
            pid, i, esiti = c[:2], 2, bytearray()
            while i < len(c):
                f, i = stringa(c, i)
                q = min(c[i], 1)
                i += 1
                f = f.decode()
                self.sub[f] = q
                esiti.append(q)
                for topic, (payload, rq) in list(self.b.retained.items()):
                    if corrisponde(f, topic):
                        self.invia_publish(topic, payload, min(q, rq), True)
            self.invia(pacchetto(SUBACK, 0, pid + bytes(esiti)))
        elif tipo == UNSUBSCRIBE:
            i = 2
            while i < len(c):
                f, i = stringa(c, i)
                self.sub.pop(f.decode(), None)
            self.invia(pacchetto(UNSUBACK, 0, c[:2]))
        elif tipo == PINGREQ:
            self.invia(pacchetto(PINGRESP, 0, b""))
        # PUBACK e PUBCOMP dei client: niente ritrasmissioni, si ignorano.


async def main():
    opzioni = carica_opzioni()
    broker = Broker(opzioni)

    async def connessione(reader, writer):
        try:
            await Cliente(broker, reader, writer).servi()
        except (asyncio.IncompleteReadError, asyncio.TimeoutError, ConnectionError, IndexError, struct.error):
            pass
        finally:
            writer.close()

    porta = int(opzioni.get("port", 1883))
    server = await asyncio.start_server(connessione, "0.0.0.0", porta)
    print(f"Broker MQTT in ascolto sulla porta {porta}", flush=True)
    async with server:
        await server.serve_forever()


if __name__ == "__main__":
    asyncio.run(main())
