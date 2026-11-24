"""Gera os mockups do WolfLauncher (SVG -> PNG com rsvg-convert).

Uso: python3 design/gerar_mockups.py   (os PNG saem em design/mockups/)
Medidas em dp de um celular de 412x892; o PNG sai com 1080 de largura.
"""
import html
import subprocess
from pathlib import Path

from PIL import ImageFont

_PESOS = {200: "ExtraLight", 300: "Light", 400: "Regular", 500: "Medium", 600: "SemiBold", 700: "Bold", 800: "ExtraBold"}


def largura(texto, tam, peso=400):
    """Largura real do texto na Noto Sans (para encaixar selos e pilulas)."""
    nome = _PESOS.get(peso, "Regular")
    try:
        f = ImageFont.truetype(f"/usr/share/fonts/noto/NotoSans-{nome}.ttf", 100)
    except OSError:
        f = ImageFont.truetype("/usr/share/fonts/noto/NotoSans-Regular.ttf", 100)
    return f.getlength(texto) * tam / 100

PASTA = Path(__file__).parent / "mockups"
L, A = 412, 892

LARANJA = "#F5A432"
AZUL = "#4C8DFF"
FUNDO = "#0D0F16"
SUP = "#14161E"
SUP2 = "#1C1F2A"
TEXTO = "#ECEEF3"
FONTE = "Noto Sans"


def t(x, y, s, tam=16, peso=400, cor=TEXTO, op=1.0, ancora="start", extra=""):
    return (f'<text x="{x}" y="{y}" font-family="{FONTE}" font-size="{tam}" font-weight="{peso}" '
            f'fill="{cor}" fill-opacity="{op}" text-anchor="{ancora}" {extra}>{html.escape(s)}</text>')


def ret(x, y, w, h, r=0, cor=SUP, op=1.0, borda=None, extra=""):
    b = f'stroke="{borda}" stroke-width="1"' if borda else ""
    return f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{r}" fill="{cor}" fill-opacity="{op}" {b} {extra}/>'


def circ(cx, cy, r, cor, op=1.0, extra=""):
    return f'<circle cx="{cx}" cy="{cy}" r="{r}" fill="{cor}" fill-opacity="{op}" {extra}/>'


def icone(cx, cy, r, cor, letra):
    """Icone generico de app: circulo com a inicial (no app real vem o icone do pacote)."""
    return circ(cx, cy, r, cor) + t(cx, cy + r * 0.36, letra, r * 1.0, 700, "#fff", ancora="middle")


def selo(x, y, n, cor=LARANJA):
    w = 22 if len(str(n)) == 1 else 28
    return ret(x, y - 15, w, 20, 10, cor) + t(x + w / 2, y, str(n), 12, 700, FUNDO, ancora="middle")


def fundo(escuro=0.35):
    """Wallpaper (ceu noturno com montanhas) + escuro + a aurora do WolfOS."""
    return f"""
<defs>
  <linearGradient id="ceu" x1="0" y1="0" x2="0" y2="1">
    <stop offset="0" stop-color="#1b2033"/><stop offset="0.6" stop-color="#10131d"/><stop offset="1" stop-color="#07080c"/>
  </linearGradient>
  <radialGradient id="aur1" cx="0.1" cy="0.08" r="0.75"><stop offset="0" stop-color="{LARANJA}" stop-opacity="0.22"/><stop offset="1" stop-color="{LARANJA}" stop-opacity="0"/></radialGradient>
  <radialGradient id="aur2" cx="0.95" cy="0.9" r="0.75"><stop offset="0" stop-color="{AZUL}" stop-opacity="0.14"/><stop offset="1" stop-color="{AZUL}" stop-opacity="0"/></radialGradient>
  <linearGradient id="sombra" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#000" stop-opacity="0"/><stop offset="1" stop-color="#000" stop-opacity="0.55"/></linearGradient>
</defs>
<rect width="{L}" height="{A}" fill="url(#ceu)"/>
{''.join(circ(x, y, r, '#fff', o) for x, y, r, o in [(60,90,1,.5),(150,40,.8,.4),(300,120,1.2,.5),(360,60,.7,.4),(230,200,.8,.3),(90,260,.6,.3),(330,300,1,.35),(40,380,.7,.3),(200,330,.6,.25)])}
<path d="M0 640 L70 560 L120 600 L190 500 L260 590 L310 540 L412 620 L412 892 L0 892 Z" fill="#161a27"/>
<path d="M0 700 L90 630 L160 690 L240 610 L320 680 L412 640 L412 892 L0 892 Z" fill="#0f121b"/>
<rect width="{L}" height="{A}" fill="{FUNDO}" fill-opacity="{escuro}"/>
<rect width="{L}" height="{A}" fill="url(#aur1)"/>
<rect width="{L}" height="{A}" fill="url(#aur2)"/>
"""


def barra_status(hora="14:32", mostrar_hora=True):
    s = t(24, 30, hora, 13, 600) if mostrar_hora else ""
    # sinal, wifi, bateria
    s += "".join(ret(322 + i * 5, 26 - i * 3, 3, 4 + i * 3, 1, TEXTO) for i in range(4))
    s += f'<path d="M348 20 q9 -8 18 0 M351 24 q6 -5 12 0" stroke="{TEXTO}" stroke-width="2" fill="none"/>' + circ(357, 27, 1.8, TEXTO)
    s += ret(372, 18, 22, 11, 3, "none", borda=TEXTO) + ret(374, 20, 15, 7, 1.5, TEXTO) + ret(394.5, 21.5, 2, 4, 1, TEXTO)
    return s


def barra_gestos():
    return ret(L / 2 - 54, A - 12, 108, 4, 2, TEXTO, 0.7)


def tela(nome, corpo, escuro=0.35):
    svg = (f'<svg xmlns="http://www.w3.org/2000/svg" width="{L}" height="{A}" viewBox="0 0 {L} {A}">'
           f'<clipPath id="tela"><rect width="{L}" height="{A}" rx="28"/></clipPath><g clip-path="url(#tela)">'
           f'{fundo(escuro)}{corpo}{barra_gestos()}</g></svg>')
    PASTA.mkdir(parents=True, exist_ok=True)
    arq_svg = PASTA / f"{nome}.svg"
    arq_svg.write_text(svg, encoding="utf-8")
    subprocess.run(["rsvg-convert", "-w", "1080", "-o", str(PASTA / f"{nome}.png"), str(arq_svg)], check=True)
    arq_svg.unlink()
    print("ok", nome)


LETRAS = list("ABCDEFGHIJLMNOPRSTUVWXYZ#")


def trilho(destaque=None, op=0.35, x=398, y0=250, y1=780, onda=False):
    """O trilho de letras na borda direita (da para usar sem abrir a gaveta)."""
    s = ""
    passo = (y1 - y0) / (len(LETRAS) - 1)
    i_d = LETRAS.index(destaque) if destaque else -99
    for i, l in enumerate(LETRAS):
        y = y0 + i * passo
        d = abs(i - i_d)
        if l == destaque:
            continue
        dx = max(0, 3 - d) * 5 if onda else 0
        s += t(x - dx, y + 4, l, 10 + (max(0, 3 - d) * 1.5 if onda else 0), 600, TEXTO, op + (0.15 * max(0, 3 - d) if onda else 0), "middle")
    return s


# ---------------------------------------------------------------- 1. tela inicial
def tela_inicio():
    c = barra_status(mostrar_hora=False)
    c += t(28, 150, "14:32", 84, 200)
    c += t(30, 184, "Segunda, 23 de novembro", 17, 600, LARANJA)
    # a linha do dia: clima, proximo compromisso, bateria (tocar abre o app de cada um)
    x = 30
    for txt in ["☁  24°", "◷  Reunião às 15:00", "▮  82%"]:
        w = 24 + largura(txt, 13, 500)
        c += ret(x, 202, w, 28, 14, "#fff", 0.07, borda="#ffffff22") + t(x + 12, 221, txt, 13, 500, TEXTO, 0.85)
        x += w + 8

    y = 430
    favs = [
        ("WhatsApp", "Ana: chego em 10 min", 3),
        ("Telefone", "1 chamada perdida · Mãe", 1),
        ("Spotify", None, 0),
        ("Câmera", None, 0),
        ("Chrome", None, 0),
        ("Mensagens", None, 0),
    ]
    for nome, previa, n in favs:
        c += t(28, y, nome, 30, 500)
        if n:
            c += selo(28 + largura(nome, 30, 500) + 12, y - 12, n)
        if previa:
            c += t(30, y + 22, previa, 13.5, 400, LARANJA, 0.9)
            y += 22
        if nome == "Spotify":
            # mini player embutido no nome do app
            c += t(30, y + 22, "▶  Uivo da Noite — Lobos do Sul", 13.5, 400, TEXTO, 0.7)
            c += t(300, y + 22, "⏮   ⏸   ⏭", 13, 400, TEXTO, 0.8)
            y += 22
        y += 58

    c += trilho()
    c += t(L / 2, 862, "︿  deslize para cima · ou segure a borda direita", 11.5, 400, TEXTO, 0.45, "middle")
    tela("01-inicio", c)


# ---------------------------------------------------------------- 2. trilho em acao
def tela_trilho():
    c = barra_status(mostrar_hora=False)
    c += t(28, 150, "14:32", 84, 200, op=0.25)
    c += t(30, 184, "Segunda, 23 de novembro", 17, 600, LARANJA, 0.25)
    c += ret(0, 0, L, A, 0, FUNDO, 0.55)
    # a letra escolhida cresce ao lado do dedo; as vizinhas abrem caminho (onda)
    c += trilho("S", op=0.4, onda=True)
    passo = (780 - 250) / (len(LETRAS) - 1)
    ys = 250 + LETRAS.index("S") * passo
    c += circ(318, ys, 30, LARANJA) + t(318, ys + 13, "S", 36, 700, FUNDO, ancora="middle")
    # dedo
    c += circ(404, ys + 8, 22, "#fff", 0.10) + circ(404, ys + 8, 12, "#fff", 0.18)
    # cartao com os apps da letra: deslizar o dedo para a esquerda escolhe, soltar abre
    apps = [("Shopee", "#EE4D2D"), ("Signal", "#3A76F0"), ("Spotify", "#1DB954"), ("Steam", "#2A475E"), ("Strava", "#FC4C02")]
    cy = ys - 20 - len(apps) * 52 / 2
    c += ret(24, cy, 256, len(apps) * 52 + 24, 24, SUP, 0.96, borda="#ffffff14")
    for i, (nome, cor) in enumerate(apps):
        yy = cy + 12 + i * 52
        if nome == "Spotify":
            c += ret(34, yy + 2, 236, 48, 16, LARANJA, 0.16)
        c += icone(72, yy + 26, 16, cor, nome[0]) + t(100, yy + 32, nome, 18, 500)
    c += t(152, cy + len(apps) * 52 + 50, "arraste até o app e solte para abrir", 12, 400, TEXTO, 0.55, "middle")
    tela("02-trilho-de-letras", c, 0.35)


# ---------------------------------------------------------------- 3. gaveta
def tela_gaveta():
    c = barra_status()
    c += ret(20, 52, 372, 50, 25, "#fff", 0.07, borda=LARANJA)
    c += t(46, 83, "⌕", 18, 400, LARANJA) + t(72, 83, "Buscar apps, contatos, ajustes…", 15, 400, TEXTO, 0.5)
    c += t(24, 130, "RECENTES", 11, 700, TEXTO, 0.45, extra='letter-spacing="1.5"')
    rec = [("Maps", "#34A853"), ("Banco", "#8A05BE"), ("Gmail", "#EA4335"), ("Fotos", "#FBBC04"), ("Uber", "#222")]
    for i, (n, cor) in enumerate(rec):
        cx = 52 + i * 74
        c += icone(cx, 168, 22, cor, n[0]) + t(cx, 208, n, 11.5, 500, TEXTO, 0.8, "middle")
    secoes = [
        ("A", [("Agenda", "#4285F4", 0), ("Ajustes", "#5F6368", 0), ("Amazon", "#FF9900", 0)]),
        ("B", [("Banco", "#8A05BE", 2), ("Bloco de notas", "#F6BF26", 0)]),
        ("C", [("Calculadora", "#3C4043", 0), ("Câmera", "#5F6368", 0), ("Chrome", "#1A73E8", 0), ("Contatos", "#4285F4", 0)]),
        ("D", [("Discord", "#5865F2", 5), ("Drive", "#0F9D58", 0)]),
    ]
    y = 250
    for letra, apps in secoes:
        c += t(24, y, letra, 14, 700, LARANJA)
        y += 14
        for nome, cor, n in apps:
            c += icone(46, y + 22, 18, cor, nome[0]) + t(78, y + 29, nome, 18, 500)
            if n:
                c += selo(78 + largura(nome, 18, 500) + 10, y + 28, n)
            y += 50
        y += 14
    c += t(24, y, "E", 14, 700, LARANJA)
    c += f'<rect x="0" y="{A-120}" width="{L}" height="120" fill="url(#sombra)"/>'
    c += trilho("C", op=0.55, y0=250, y1=820)
    passo = (820 - 250) / (len(LETRAS) - 1)
    c += t(398, 250 + LETRAS.index("C") * passo + 5, "C", 13, 800, LARANJA, ancora="middle")
    tela("03-gaveta", c, 0.88)


# ---------------------------------------------------------------- 4. segurar um app
def tela_menu():
    c = barra_status(mostrar_hora=False)
    c += t(28, 150, "14:32", 84, 200, op=0.3)
    for i, n in enumerate(["WhatsApp", "Telefone", "Spotify", "Câmera"]):
        c += t(28, 470 + i * 58, n, 30, 500, TEXTO, 0.3)
    c += ret(0, 0, L, A, 0, "#000", 0.45)
    top = 430
    c += ret(8, top, L - 16, A - top + 30, 30, SUP, 0.98, borda="#ffffff14")
    c += ret(L / 2 - 20, top + 10, 40, 4, 2, TEXTO, 0.3)
    c += icone(52, top + 56, 24, "#25D366", "W") + t(88, top + 52, "WhatsApp", 20, 600) + t(88, top + 72, "3 notificações · usado há 5 min", 12.5, 400, TEXTO, 0.55)
    # atalhos do proprio app (ShortcutManager)
    c += t(28, top + 118, "ATALHOS", 11, 700, TEXTO, 0.45, extra='letter-spacing="1.5"')
    for i, (txt, ic) in enumerate([("Nova conversa", "✎"), ("Câmera", "◉"), ("Ana", "☺")]):
        yy = top + 132 + i * 46
        c += ret(20, yy, L - 40, 40, 14, "#fff", 0.05) + t(40, yy + 26, ic, 15, 400, LARANJA) + t(66, yy + 26, txt, 15.5, 500)
    # acoes do launcher em pilulas
    yy = top + 286
    c += t(28, yy, "NO LAUNCHER", 11, 700, TEXTO, 0.45, extra='letter-spacing="1.5"')
    x, yy = 20, yy + 14
    for txt, dest in [("★ Tirar do início", True), ("✎ Renomear", False), ("◌ Esconder", False), ("⇅ Mudar de lugar", False), ("ⓘ Informações", False), ("✕ Desinstalar", False)]:
        w = 28 + largura(txt, 13.5, 500)
        if x + w > L - 20:
            x, yy = 20, yy + 46
        c += ret(x, yy, w, 36, 18, LARANJA if dest else "#fff", 0.16 if dest else 0.06, borda=LARANJA if dest else "#ffffff18")
        c += t(x + w / 2, yy + 23, txt, 13.5, 500, LARANJA if dest else TEXTO, ancora="middle")
        x += w + 8
    tela("04-segurar-app", c, 0.35)


# ---------------------------------------------------------------- 5. busca universal
def tela_busca():
    c = barra_status()
    c += ret(20, 52, 372, 50, 25, "#fff", 0.08, borda=LARANJA)
    c += t(46, 83, "⌕", 18, 400, LARANJA) + t(72, 83, "ca", 16, 500) + ret(92, 69, 1.5, 20, 0, LARANJA)
    c += t(368, 83, "✕", 15, 400, TEXTO, 0.5, "middle")

    def grupo(y, titulo):
        return t(24, y, titulo, 11, 700, TEXTO, 0.45, extra='letter-spacing="1.5"')

    y = 136
    c += grupo(y, "APPS")
    # o primeiro resultado fica marcado: Enter abre ele
    c += ret(14, y + 10, L - 28, 50, 16, LARANJA, 0.14, borda="#F5A43255")
    for i, (n, cor) in enumerate([("Calculadora", "#3C4043"), ("Câmera", "#5F6368"), ("Calendário", "#4285F4")]):
        yy = y + 10 + i * 50
        c += icone(46, yy + 25, 17, cor, n[0]) + t(76, yy + 31, n, 17, 500)
        if i == 0:
            c += t(380, yy + 31, "↵", 15, 600, LARANJA, ancora="end")
    y += 180
    c += grupo(y, "CONTATOS")
    for i, (n, sub) in enumerate([("Carla Souza", "celular · (51) 9 9876-0000"), ("Caio (trabalho)", "e-mail")]):
        yy = y + 12 + i * 56
        c += circ(46, yy + 26, 17, SUP2) + t(46, yy + 32, n[0], 15, 700, LARANJA, ancora="middle")
        c += t(76, yy + 24, n, 16.5, 500) + t(76, yy + 42, sub, 12, 400, TEXTO, 0.5)
        c += t(330, yy + 32, "✆", 17, 400, LARANJA, ancora="middle") + t(370, yy + 32, "✉", 16, 400, LARANJA, ancora="middle")
    y += 140
    c += grupo(y, "AJUSTES DO ANDROID")
    for i, n in enumerate(["Transmitir tela (Cast)", "Permissões de câmera"]):
        yy = y + 12 + i * 44
        c += t(30, yy + 28, "⚙", 15, 400, TEXTO, 0.6) + t(58, yy + 28, n, 15.5, 400)
    y += 116
    c += grupo(y, "MAIS")
    c += ret(20, y + 12, L - 40, 44, 14, "#fff", 0.05) + t(40, y + 40, "⌕  Buscar “ca” na web", 15, 500)
    # teclado (resumido)
    c += ret(0, A - 250, L, 250, 0, "#1a1c22") + t(L / 2, A - 125, "teclado", 14, 400, TEXTO, 0.25, "middle")
    tela("05-busca", c, 0.9)


# ---------------------------------------------------------------- 6. notificacoes no proprio app
def tela_notificacoes():
    c = barra_status(mostrar_hora=False)
    c += t(28, 150, "14:32", 84, 200)
    c += t(30, 184, "Segunda, 23 de novembro", 17, 600, LARANJA)
    y = 300
    c += t(28, y, "WhatsApp", 30, 500) + selo(28 + largura("WhatsApp", 30, 500) + 12, y - 12, 3)
    # tocar no selo abre as notificacoes ali mesmo, sem descer a cortina
    y += 18
    nots = [("Ana", "chego em 10 min", "agora"), ("Grupo Matilha", "Leo: bora sábado?", "8 min"), ("Pedro", "Foto", "21 min")]
    c += ret(20, y, L - 40, len(nots) * 64 + 52, 22, SUP, 0.92, borda="#ffffff14")
    for i, (de, msg, quando) in enumerate(nots):
        yy = y + 12 + i * 64
        if i == 1:
            # deslizando para dispensar
            c += ret(28, yy, L - 56, 56, 16, "#E5484D", 0.25) + t(52, yy + 34, "dispensar", 12, 600, "#E5484D")
            c += f'<g transform="translate(110 0)" opacity="0.85">'
        c += ret(28, yy, L - 56, 56, 16, SUP2, 1)
        c += circ(56, yy + 28, 15, "#25D366", 0.25) + t(56, yy + 33, de[0], 13, 700, "#25D366", ancora="middle")
        c += t(82, yy + 24, de, 14.5, 600) + t(82, yy + 43, msg, 13.5, 400, TEXTO, 0.7)
        c += t(L - 40, yy + 24, quando, 11, 400, TEXTO, 0.45, "end")
        if i == 1:
            c += "</g>"
    yy = y + len(nots) * 64 + 14
    c += t(40, yy + 22, "Responder", 13.5, 600, LARANJA) + t(140, yy + 22, "Marcar como lida", 13.5, 600, LARANJA) + t(L - 40, yy + 22, "Limpar tudo", 13.5, 600, TEXTO, 0.55, "end")
    y = yy + 80
    for n in ["Telefone", "Spotify", "Câmera", "Chrome"]:
        c += t(28, y, n, 30, 500, TEXTO, 0.55)
        y += 58
    c += trilho()
    tela("06-notificacoes", c)


# ---------------------------------------------------------------- 7. ajustes
def tela_ajustes():
    c = barra_status()
    c += t(24, 92, "‹", 26, 300) + t(52, 92, "Ajustes do WolfLauncher", 21, 600)

    def grupo(y, titulo):
        return t(24, y, titulo, 11, 700, LARANJA, extra='letter-spacing="1.5"')

    def chave(y, titulo, sub, ligado):
        s = t(24, y, titulo, 16, 500) + t(24, y + 19, sub, 12.5, 400, TEXTO, 0.5)
        s += ret(338, y - 18, 50, 28, 14, LARANJA if ligado else "#ffffff", 1 if ligado else 0.15)
        s += circ(374 if ligado else 352, y - 4, 10, FUNDO if ligado else TEXTO)
        return s

    y = 140
    c += grupo(y, "VISUAL")
    c += t(24, y + 34, "Cor de destaque", 16, 500)
    for i, (cor, rot) in enumerate([("wall", "Do wallpaper"), (LARANJA, "Laranja"), (AZUL, "Azul"), ("#3DD68C", "Verde"), ("#B98CFF", "Lilás"), ("#FF6B81", "Rosa")]):
        cx = 44 + i * 62
        if cor == "wall":
            c += f'<defs><linearGradient id="gw"><stop offset="0" stop-color="{LARANJA}"/><stop offset="1" stop-color="{AZUL}"/></linearGradient></defs>'
            c += f'<circle cx="{cx}" cy="{y+72}" r="18" fill="url(#gw)"/>' + circ(cx, y + 72, 23, "none", extra=f'stroke="{TEXTO}" stroke-width="2"')
        else:
            c += circ(cx, y + 72, 18, cor)
        c += t(cx, y + 108, rot, 10.5, 500, TEXTO, 0.6, "middle")
    c += t(24, y + 146, "Tamanho da letra", 16, 500) + t(388, y + 146, "Grande", 13, 500, LARANJA, ancora="end")
    c += ret(24, y + 162, 364, 4, 2, "#fff", 0.15) + ret(24, y + 162, 250, 4, 2, LARANJA) + circ(274, y + 164, 9, LARANJA)
    y += 220
    c += chave(y, "Ícones na tela inicial", "só texto fica mais limpo", False)
    y += 58
    c += chave(y, "Prévia das notificações", "mostra a última mensagem sob o app", True)
    y += 58
    c += chave(y, "Linha do dia", "clima, próximo compromisso e bateria", True)
    y += 70
    c += grupo(y, "GESTOS")
    for i, (g, a) in enumerate([("Deslizar para cima", "Gaveta de apps"), ("Deslizar para baixo", "Cortina de notificações"), ("Tocar duas vezes", "Bloquear a tela"), ("Deslizar para a direita", "Painel (widgets)")]):
        yy = y + 34 + i * 52
        c += t(24, yy, g, 16, 500) + t(24, yy + 19, a, 12.5, 400, LARANJA, 0.9) + t(388, yy + 8, "›", 20, 300, TEXTO, 0.4, "end")
    y += 236
    c += grupo(y, "APPS")
    c += t(24, y + 34, "Apps escondidos", 16, 500) + t(388, y + 34, "2  ›", 14, 500, TEXTO, 0.5, "end")
    c += t(24, y + 74, "Pacote de ícones", 16, 500) + t(388, y + 74, "Do sistema  ›", 14, 500, TEXTO, 0.5, "end")
    tela("07-ajustes", c, 0.92)


if __name__ == "__main__":
    tela_inicio()
    tela_trilho()
    tela_gaveta()
    tela_menu()
    tela_busca()
    tela_notificacoes()
    tela_ajustes()
