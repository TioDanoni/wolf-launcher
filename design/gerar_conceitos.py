"""Tres conceitos novos de tela inicial (2 telas cada), para escolher o caminho.

Uso: python3 design/gerar_conceitos.py   (os PNG saem em design/mockups/conceitos/)
Reaproveita os desenhos basicos de gerar_mockups.py.
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
import gerar_mockups as g  # noqa: E402
from gerar_mockups import (AZUL, FUNDO, L, LARANJA, SUP, SUP2, TEXTO, A,  # noqa: E402
                           barra_status, circ, icone, largura, ret, t)

g.PASTA = Path(__file__).parent / "mockups" / "conceitos"

VERDE = "#3DD68C"


def pessoa(cx, cy, r, letra, cor, aviso=False, op=1.0):
    s = circ(cx, cy, r, cor, 0.28 * op) + t(cx, cy + r * 0.36, letra, r, 700, cor, op, "middle")
    if aviso:
        s += circ(cx, cy, r + 4, "none", extra=f'stroke="{LARANJA}" stroke-width="2.5"')
    return s


def rotulo(x, y, txt, cor=TEXTO, op=0.45):
    return t(x, y, txt, 11, 700, cor, op, extra='letter-spacing="1.6"')


# =========================================================== A · UIVO (comando)
def a1_inicio():
    """Tela quase vazia: o relogio empilhado e uma barra so, na altura do polegar."""
    c = barra_status(mostrar_hora=False)
    c += t(30, 230, "14", 150, 200) + t(30, 370, "32", 150, 200, LARANJA)
    c += t(34, 412, "seg · 23 nov · ☁ 24°", 15, 500, TEXTO, 0.6)
    # sugestoes do momento: o que voce costuma abrir agora, e por que
    c += rotulo(30, 560, "PARA AGORA")
    sug = [("Spotify", "#1DB954", "fone conectado"), ("Maps", "#34A853", "casa · 18 min"), ("WhatsApp", "#25D366", "Ana respondeu")]
    for i, (n, cor, por) in enumerate(sug):
        y = 580 + i * 56
        c += ret(20, y, L - 40, 48, 24, "#fff", 0.06, borda="#ffffff14")
        c += icone(46, y + 24, 14, cor, n[0]) + t(70, y + 30, n, 16, 600) + t(L - 38, y + 30, por, 12.5, 400, TEXTO, 0.55, "end")
    # a barra de comando: escreve app, pessoa, conta ou ajuste
    y = 776
    c += ret(16, y, L - 32, 58, 29, SUP, 0.95, borda=LARANJA)
    c += circ(46, y + 29, 15, LARANJA) + t(46, y + 35, "◗", 14, 700, FUNDO, ancora="middle")
    c += t(72, y + 35, "ligar mãe · timer 5 min · wi-fi", 15, 400, TEXTO, 0.45)
    c += t(L - 44, y + 36, "🎙", 17, 400, TEXTO, 0.7, "middle")
    c += t(L / 2, 860, "desenhe uma letra em qualquer lugar da tela", 11.5, 400, TEXTO, 0.45, "middle")
    g.tela("A1-uivo-inicio", c, 0.4)


def a2_letra():
    """Desenhar uma letra com o dedo: os apps dela abrem em leque em volta do dedo."""
    c = barra_status(mostrar_hora=False)
    c += t(30, 230, "14", 150, 200, op=0.12) + t(30, 370, "32", 150, 200, LARANJA, 0.12)
    c += ret(0, 0, L, A, 0, FUNDO, 0.5)
    # o rastro do dedo formando um S
    rastro = "M262 300 C 200 250, 130 290, 160 350 S 290 420, 250 480 S 140 520, 120 470"
    c += '<g transform="translate(20 -110)">'
    c += f'<path d="{rastro}" stroke="{LARANJA}" stroke-opacity="0.25" stroke-width="22" fill="none" stroke-linecap="round"/>'
    c += f'<path d="{rastro}" stroke="{LARANJA}" stroke-width="5" fill="none" stroke-linecap="round"/></g>'
    cx, cy = 206, 610
    c += circ(cx, cy, 30, LARANJA) + t(cx, cy + 12, "S", 34, 700, FUNDO, ancora="middle")
    # leque: o mais usado mais perto do polegar
    import math
    apps = [("Spotify", "#1DB954", 1.0), ("Signal", "#3A76F0", .9), ("Steam", "#2A475E", .8), ("Shopee", "#EE4D2D", .7), ("Strava", "#FC4C02", .6), ("Som", "#5F6368", .55)]
    for i, (n, cor, op) in enumerate(apps):
        ang = math.radians(186 + i * 33.6)
        x, y = cx + 128 * math.cos(ang), cy + 128 * math.sin(ang)
        r = 26 if i == 0 else 22
        if i == 0:
            c += circ(x, y, r + 7, LARANJA, 0.25)
        c += icone(x, y, r, cor, n[0]) + t(x, y + r + 16, n, 12, 600, TEXTO, op, "middle")
    c += t(L / 2, 790, "solte em cima de um app para abrir", 13, 500, TEXTO, 0.6, "middle")
    c += t(L / 2, 812, "ou continue escrevendo: Sp → Spotify", 12, 400, TEXTO, 0.4, "middle")
    g.tela("A2-uivo-letra", c, 0.4)


# =========================================================== B · RIO (linha do tempo)
def no(x, y, cor=TEXTO, op=0.35, r=5):
    return circ(x, y, r, cor, op)


def evento(y, hora, titulo, sub, app=None, op=1.0, botao=None):
    s = t(30, y + 5, hora, 12.5, 600, TEXTO, 0.5 * op) + no(86, y, TEXTO, 0.4 * op)
    s += t(106, y, titulo, 16.5, 500, TEXTO, op) + t(106, y + 20, sub, 12.5, 400, TEXTO, 0.5 * op)
    if app:
        s += icone(L - 40, y + 4, 14, app[1], app[0][0])
    if botao:
        w = 22 + largura(botao, 13, 600)
        s += ret(L - 26 - w, y - 14, w, 30, 15, LARANJA, 0.18, borda=LARANJA) + t(L - 26 - w / 2, y + 6, botao, 13, 600, LARANJA, ancora="middle")
    return s


def b1_rio():
    """O dia como um rio: o que passou em cima, o agora na altura do polegar, o que vem embaixo."""
    c = barra_status(mostrar_hora=False)
    c += f'<line x1="86" y1="70" x2="86" y2="840" stroke="{TEXTO}" stroke-opacity="0.12" stroke-width="2"/>'
    c += rotulo(106, 84, "ANTES")
    c += evento(120, "09:12", "Ana mandou 3 mensagens", "“chego em 10 min”", ("WhatsApp", "#25D366"), 0.55)
    c += evento(184, "11:40", "Você ouviu Lobos do Sul", "continuar de 2:14", ("Spotify", "#1DB954"), 0.55)
    c += evento(248, "13:05", "Foto salva", "Câmera · 4 fotos", ("Fotos", "#FBBC04"), 0.55)
    # AGORA: fixo na altura do polegar
    y = 330
    c += ret(14, y, L - 28, 250, 28, SUP, 0.9, borda="#F5A43266")
    c += circ(86, y - 2, 9, LARANJA) + circ(86, y - 2, 16, LARANJA, 0.2)
    c += t(30, y + 62, "14:32", 52, 200) + t(196, y + 44, "AGORA", 11, 700, LARANJA, extra='letter-spacing="1.6"')
    c += t(196, y + 64, "Segunda, 23 nov · ☁ 24°", 13, 500, TEXTO, 0.6)
    c += rotulo(30, y + 104, "VOCÊ COSTUMA ABRIR NESSA HORA", op=0.4)
    for i, (n, cor) in enumerate([("WhatsApp", "#25D366"), ("Spotify", "#1DB954"), ("Chrome", "#1A73E8"), ("Banco", "#8A05BE"), ("Câmera", "#5F6368")]):
        x = 52 + i * 76
        c += icone(x, y + 146, 22, cor, n[0]) + t(x, y + 184, n, 11.5, 500, TEXTO, 0.75, "middle")
    c += ret(30, y + 204, L - 60, 32, 16, "#fff", 0.06) + t(50, y + 225, "⌕  todos os apps, pessoas, ajustes…", 13, 400, TEXTO, 0.5)
    c += rotulo(106, 624, "DEPOIS", LARANJA, 0.8)
    c += evento(660, "15:00", "Reunião de projeto", "Meet · em 28 min", None, 1, "Entrar")
    c += evento(724, "18:10", "Sair para casa", "trânsito bom · 18 min", ("Maps", "#34A853"))
    c += evento(788, "22:30", "Hora de descansar", "o launcher fica mais calmo", None, 0.7)
    g.tela("B1-rio-dia", c, 0.55)


def b2_noite():
    """O mesmo rio as 23h: o launcher se acalma, so o essencial e o que vem amanha."""
    c = barra_status(mostrar_hora=False)
    c += ret(0, 0, L, A, 0, "#000", 0.35)
    c += f'<line x1="86" y1="70" x2="86" y2="840" stroke="{TEXTO}" stroke-opacity="0.08" stroke-width="2"/>'
    c += rotulo(106, 120, "HOJE", op=0.3)
    c += t(106, 150, "6 h de tela · 2 reuniões · 4 fotos", 14, 400, TEXTO, 0.4)
    y = 300
    c += circ(86, y - 2, 8, "#B98CFF") + circ(86, y - 2, 15, "#B98CFF", 0.2)
    c += t(30, y + 70, "23:10", 64, 200, TEXTO, 0.85)
    c += t(32, y + 104, "Modo noite · notificações em silêncio", 14, 500, "#B98CFF", 0.9)
    for i, (n, cor) in enumerate([("Despertador", "#5F6368"), ("Kindle", "#FF9900"), ("Spotify", "#1DB954")]):
        x = 60 + i * 96
        c += icone(x, y + 170, 24, cor, n[0]) + t(x, y + 212, n, 12, 500, TEXTO, 0.6, "middle")
    c += rotulo(106, 600, "AMANHÃ", "#B98CFF", 0.8)
    c += evento(640, "06:30", "Despertador", "toque suave · 7 h 20 min de sono", None, 0.8, "Mudar")
    c += evento(704, "09:00", "Daily do time", "Meet", None, 0.6)
    c += t(L / 2, 856, "os outros apps continuam a um deslizar", 11.5, 400, TEXTO, 0.35, "middle")
    g.tela("B2-rio-noite", c, 0.75)


# =========================================================== C · MATILHA (pessoas e verbos)
VERBOS = [("Conversar", 3), ("Ouvir", 0), ("Ir", 0), ("Ver", 0), ("Criar", 0), ("Trabalhar", 1)]


def c1_matilha():
    """A tela inicial em verbos (o que voce quer fazer) e a sua matilha (pessoas) no polegar."""
    c = barra_status(mostrar_hora=False)
    c += t(28, 120, "14:32", 44, 300) + t(150, 120, "seg, 23 nov", 15, 500, LARANJA)
    y = 260
    pre = {"Conversar": "Ana: chego em 10 min", "Ouvir": "▶ Lobos do Sul", "Ir": "casa · 18 min", "Trabalhar": "reunião às 15:00"}
    for v, n in VERBOS:
        c += t(26, y, v, 38, 300 if not n else 500, TEXTO, 1 if (n or v in pre) else 0.75)
        if n:
            c += circ(26 + largura(v, 38, 500) + 14, y - 26, 5, LARANJA)
        if v in pre:
            c += t(28, y + 22, pre[v], 13, 400, LARANJA if n else TEXTO, 0.85 if n else 0.5)
            y += 18
        y += 62
    # a matilha: as pessoas que voce mais fala, sempre embaixo
    y = 742
    c += ret(0, y - 30, L, A - y + 30, 0, FUNDO, 0.5)
    c += rotulo(26, y - 8, "MATILHA")
    for i, (l, cor, av) in enumerate([("A", "#25D366", True), ("M", LARANJA, True), ("L", AZUL, False), ("P", "#B98CFF", False), ("J", "#FF6B81", False)]):
        x = 48 + i * 64
        c += pessoa(x, y + 36, 24, l, cor, av)
    c += circ(48 + 5 * 64, y + 36, 24, "#fff", 0.06) + t(48 + 5 * 64, y + 43, "+", 20, 300, TEXTO, 0.6, "middle")
    g.tela("C1-matilha-inicio", c, 0.45)


def c2_conversar():
    """Tocou em Conversar: pessoas primeiro (e por onde falar com cada uma), depois os apps."""
    c = barra_status(mostrar_hora=False)
    c += t(26, 120, "Conversar", 44, 500) + t(L - 30, 118, "✕", 18, 400, TEXTO, 0.5, "end")
    c += rotulo(26, 172, "QUEM ESTÁ ESPERANDO")
    gente = [("Ana", "A", "#25D366", "chego em 10 min · agora", "WhatsApp"), ("Mãe", "M", LARANJA, "chamada perdida · 12 min", "Telefone"), ("Grupo Matilha", "G", AZUL, "Leo: bora sábado?", "WhatsApp")]
    y = 190
    for nome, l, cor, msg, via in gente:
        c += ret(16, y, L - 32, 70, 22, SUP, 0.85, borda="#ffffff12")
        c += pessoa(54, y + 35, 22, l, cor, True)
        c += t(90, y + 30, nome, 17, 600) + t(90, y + 51, msg, 13, 400, TEXTO, 0.6)
        c += t(L - 34, y + 30, via, 11, 500, TEXTO, 0.4, "end")
        c += t(L - 34, y + 52, "✆   ✉", 15, 400, LARANJA, ancora="end")
        y += 80
    c += rotulo(26, y + 30, "APPS PARA CONVERSAR")
    apps = [("WhatsApp", "#25D366", 3), ("Telegram", "#2AABEE", 0), ("Mensagens", "#1A73E8", 0), ("Telefone", "#34A853", 1), ("Gmail", "#EA4335", 0), ("Discord", "#5865F2", 0)]
    for i, (n, cor, k) in enumerate(apps):
        x = 56 + (i % 3) * 120
        yy = y + 90 + (i // 3) * 96
        c += icone(x, yy, 26, cor, n[0]) + t(x, yy + 46, n, 13, 500, TEXTO, 0.8, "middle")
        if k:
            c += circ(x + 20, yy - 20, 9, LARANJA) + t(x + 20, yy - 16, str(k), 11, 700, FUNDO, ancora="middle")
    c += t(L / 2, 850, "os apps entram no verbo sozinhos pela categoria do Android · dá para mudar", 10.5, 400, TEXTO, 0.4, "middle")
    g.tela("C2-matilha-conversar", c, 0.85)


if __name__ == "__main__":
    a1_inicio()
    a2_letra()
    b1_rio()
    b2_noite()
    c1_matilha()
    c2_conversar()
