"""O WolfLauncher final: Matilha (verbos + pessoas) + Uivo (letra e comando) + Rio (o dia).

Uso: python3 design/gerar_final.py   (os PNG saem em design/mockups/final/)
"""
import math
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
import gerar_conceitos as gc  # noqa: E402
import gerar_mockups as g  # noqa: E402
from gerar_conceitos import pessoa, rotulo  # noqa: E402
from gerar_mockups import (AZUL, FUNDO, L, LARANJA, SUP, SUP2, TEXTO, A,  # noqa: E402
                           barra_status, circ, icone, largura, ret, t)

g.PASTA = Path(__file__).parent / "mockups" / "final"
LILAS = "#B98CFF"

MATILHA = [("A", "#25D366", True), ("M", LARANJA, True), ("L", AZUL, False), ("P", LILAS, False), ("J", "#FF6B81", False)]


def com_nome(nome, fn):
    """Roda um desenho antigo gravando com outro nome."""
    orig = g.tela
    g.tela = lambda _n, c, e=0.35: orig(nome, c, e)
    try:
        fn()
    finally:
        g.tela = orig


def cabecalho(hora, cor=LARANJA, data="seg, 23 nov · ☁ 24°", op=1.0):
    c = t(26, 112, hora, 56, 200, TEXTO, op)
    x = 26 + largura(hora, 56, 300) + 4
    c += t(x, 90, data.split(" · ")[0], 15, 600, cor, op) + t(x, 110, " · ".join(data.split(" · ")[1:]), 13, 500, TEXTO, 0.55 * op)
    return c


def faixa_agora(y, icone_txt, texto, botao, cor=LARANJA, op=1.0):
    """A linha do 'agora' (do Rio): o proximo compromisso, com a acao na mao."""
    c = ret(16, y, L - 32, 44, 22, cor, 0.10 * op, borda=cor + "55")
    c += t(36, y + 28, icone_txt, 14, 600, cor, op) + t(58, y + 28, texto, 14, 500, TEXTO, 0.9 * op)
    w = 22 + largura(botao, 13, 700)
    c += ret(L - 22 - w, y + 7, w, 30, 15, cor, op) + t(L - 22 - w / 2, y + 27, botao, 13, 700, FUNDO, ancora="middle")
    return c


def verbos(y, lista, cor=LARANJA, op_base=1.0):
    """Os verbos da Matilha, com a previa ao vivo embaixo de cada um."""
    c = ""
    for v, previa, aviso, extra in lista:
        peso = 500 if aviso else 300
        c += t(24, y, v, 34, peso, TEXTO, (1 if (aviso or previa) else 0.7) * op_base)
        if aviso:
            c += circ(24 + largura(v, 34, 500) + 12, y - 24, 4.5, cor)
        if extra:
            c += t(L - 26, y - 4, extra, 11, 600, cor, 0.7, "end")
        if previa:
            c += t(26, y + 20, previa, 13, 400, cor if aviso else TEXTO, (0.9 if aviso else 0.5) * op_base)
            y += 18
        y += 56
    return c


def matilha(y, lista=MATILHA, cor=LARANJA, op=1.0, legenda="MATILHA"):
    c = rotulo(24, y, legenda, op=0.45 * op)
    for i, (l, cc, av) in enumerate(lista):
        x = 46 + i * 64
        c += pessoa(x, y + 34, 22, l, cc, av, op)
        if av:
            c += circ(x, y + 34, 26, "none", extra=f'stroke="{cor}" stroke-width="2.5" stroke-opacity="{op}"')
    x = 46 + len(lista) * 64
    c += circ(x, y + 34, 22, "#fff", 0.06) + t(x, y + 41, "+", 20, 300, TEXTO, 0.6, "middle")
    return c


def barra_uivo(y, texto="Buscar ou mandar: “ligar mãe”", cor=LARANJA, digitado=False):
    """A barra de comando (do Uivo), sempre na altura do polegar."""
    c = ret(14, y, L - 28, 54, 27, SUP, 0.96, borda=cor)
    c += circ(42, y + 27, 14, cor) + t(42, y + 33, "◗", 13, 700, FUNDO, ancora="middle")
    if digitado:
        c += t(66, y + 33, texto, 16, 500) + ret(66 + largura(texto, 16, 500) + 2, y + 17, 1.5, 21, 0, cor)
        c += t(L - 40, y + 33, "✕", 15, 400, TEXTO, 0.5, "middle")
    else:
        c += t(66, y + 33, texto, 14.5, 400, TEXTO, 0.45) + t(L - 40, y + 34, "🎙", 16, 400, TEXTO, 0.7, "middle")
    return c


# ------------------------------------------------------------------ 1. tela inicial (tarde)
def inicio():
    c = barra_status(mostrar_hora=False)
    c += cabecalho("14:32")
    c += faixa_agora(138, "◷", "Reunião de projeto · em 28 min", "Entrar")
    # de tarde o "Trabalhar" sobe sozinho (ritmo do dia)
    c += verbos(250, [
        ("Conversar", "Ana: chego em 10 min · +2", True, None),
        ("Trabalhar", "2 e-mails novos", True, "↑ de tarde"),
        ("Ouvir", "▶ Lobos do Sul · continuar", False, None),
        ("Ir", "casa · 18 min", False, None),
        ("Ver", None, False, None),
        ("Criar", None, False, None),
    ])
    c += matilha(690)
    c += barra_uivo(782)
    c += t(L / 2, 860, "desenhe uma letra · ↑ todos os apps · → seu dia", 11.5, 400, TEXTO, 0.45, "middle")
    g.tela("01-inicio", c, 0.45)


# ------------------------------------------------------------------ 2. desenhar uma letra
def letra():
    c = barra_status(mostrar_hora=False)
    c += cabecalho("14:32", op=0.15)
    c += verbos(250, [("Conversar", None, False, None), ("Trabalhar", None, False, None), ("Ouvir", None, False, None), ("Ir", None, False, None)], op_base=0.15)
    c += ret(0, 0, L, A, 0, FUNDO, 0.5)
    rastro = "M262 300 C 200 250, 130 290, 160 350 S 290 420, 250 480 S 140 520, 120 470"
    c += '<g transform="translate(20 -110)">'
    c += f'<path d="{rastro}" stroke="{LARANJA}" stroke-opacity="0.25" stroke-width="22" fill="none" stroke-linecap="round"/>'
    c += f'<path d="{rastro}" stroke="{LARANJA}" stroke-width="5" fill="none" stroke-linecap="round"/></g>'
    cx, cy = 206, 610
    c += circ(cx, cy, 30, LARANJA) + t(cx, cy + 12, "S", 34, 700, FUNDO, ancora="middle")
    # leque: apps, pessoas e ajustes com S (o mais usado perto do polegar)
    itens = [("Spotify", "#1DB954", "app"), ("Sara", LILAS, "pessoa"), ("Signal", "#3A76F0", "app"), ("Steam", "#2A475E", "app"), ("Shopee", "#EE4D2D", "app"), ("Som", "#5F6368", "ajuste")]
    for i, (n, cor, tipo) in enumerate(itens):
        ang = math.radians(186 + i * 33.6)
        x, y = cx + 128 * math.cos(ang), cy + 128 * math.sin(ang)
        r = 26 if i == 0 else 22
        if i == 0:
            c += circ(x, y, r + 7, LARANJA, 0.25)
        c += pessoa(x, y, r, n[0], cor) if tipo == "pessoa" else icone(x, y, r, cor, n[0])
        c += t(x, y + r + 16, n, 12, 600, TEXTO, 1 if i == 0 else 0.8, "middle")
    c += t(L / 2, 800, "solte em cima para abrir · escreva mais letras para afinar", 12.5, 500, TEXTO, 0.6, "middle")
    g.tela("02-desenhar-letra", c, 0.45)


# ------------------------------------------------------------------ 3. comando (resultados sobem do polegar)
def comando():
    c = barra_status()
    teclado = 250
    by = A - teclado - 70
    # resultados de baixo para cima: o melhor fica colado na barra (perto do polegar)
    linhas = [
        ("APPS", None),
        ("Maps", ("app", "#34A853", "M")),
        ("Mensagens", ("app", "#1A73E8", "M")),
        ("PESSOAS", None),
        ("Ligar para Marcos", ("pessoa", AZUL, "M", "celular")),
        ("Mensagem para Mãe", ("pessoa", LARANJA, "M", "WhatsApp")),
        ("Ligar para Mãe", ("top", LARANJA, "M", "celular · (51) 9 9123-0000")),
    ]
    y = by - 16
    for txt, info in reversed(linhas):
        if info is None:
            c += rotulo(26, y - 8, txt)
            y -= 34
            continue
        tipo = info[0]
        h = 56
        if tipo == "top":
            c += ret(14, y - h, L - 28, h - 4, 18, LARANJA, 0.15, borda="#F5A43266")
            c += t(L - 30, y - 22, "↵", 16, 700, LARANJA, ancora="end")
        if tipo == "app":
            c += icone(44, y - 28, 16, info[1], info[2])
        else:
            c += pessoa(44, y - 28, 17, info[2], info[1])
        c += t(74, y - 30 if tipo != "app" else y - 22, txt, 16, 600 if tipo == "top" else 500)
        if tipo != "app":
            c += t(74, y - 12, info[3], 12, 400, TEXTO, 0.5)
        y -= h
    # atalhos de comando
    y -= 40
    c += rotulo(26, y - 14, "TAMBÉM DÁ PARA ESCREVER")
    x = 20
    for ch in ["timer 5 min", "12 × 3", "wi-fi", "lanterna"]:
        w = 24 + largura(ch, 13, 500)
        c += ret(x, y - 4, w, 30, 15, "#fff", 0.06, borda="#ffffff18") + t(x + w / 2, y + 16, ch, 13, 500, TEXTO, 0.8, "middle")
        x += w + 8
    c += barra_uivo(by, "ligar m", digitado=True)
    c += ret(0, A - teclado, L, teclado, 0, "#1a1c22") + t(L / 2, A - teclado / 2, "teclado", 14, 400, TEXTO, 0.25, "middle")
    g.tela("03-comando", c, 0.9)


# ------------------------------------------------------------------ 6. noite
def noite():
    c = barra_status(mostrar_hora=False)
    c += ret(0, 0, L, A, 0, "#000", 0.3)
    c += cabecalho("23:10", LILAS, "seg, 23 nov · modo noite")
    c += faixa_agora(138, "⏰", "Despertador 06:30 · 7 h 20 de sono", "Mudar", LILAS)
    # a noite troca os verbos: menos coisa, mais calma
    c += verbos(260, [
        ("Ouvir", "▶ sons de chuva · 30 min", False, None),
        ("Ler", "Kindle · capítulo 12", False, None),
        ("Conversar", "em silêncio até 07:00", False, None),
    ], LILAS, 0.85)
    c += rotulo(24, 520, "HOJE", op=0.3) + t(24, 544, "6 h de tela · 2 reuniões · 4 fotos", 13.5, 400, TEXTO, 0.4)
    c += matilha(690, [("A", "#25D366", False), ("M", LARANJA, False)], LILAS, 0.8, "SÓ ESSAS PESSOAS TOCAM À NOITE")
    c += barra_uivo(782, cor=LILAS)
    c += t(L / 2, 860, "o resto dos apps continua a um gesto", 11.5, 400, TEXTO, 0.4, "middle")
    g.tela("06-noite", c, 0.75)


# ------------------------------------------------------------------ 8. segurar um verbo: editar
def editar_verbo():
    c = barra_status(mostrar_hora=False)
    c += cabecalho("14:32", op=0.25)
    c += verbos(250, [("Conversar", None, False, None), ("Trabalhar", None, False, None), ("Ouvir", None, False, None)], op_base=0.3)
    c += ret(0, 0, L, A, 0, "#000", 0.45)
    top = 300
    c += ret(8, top, L - 16, A - top + 30, 30, SUP, 0.98, borda="#ffffff14") + ret(L / 2 - 20, top + 10, 40, 4, 2, TEXTO, 0.3)
    c += t(28, top + 56, "Ouvir", 28, 500) + t(L - 30, top + 54, "Renomear", 13, 600, LARANJA, ancora="end")
    c += rotulo(28, top + 96, "APPS NESTE VERBO")
    x, y = 24, top + 110
    for n, cor in [("Spotify", "#1DB954"), ("YT Music", "#FF0000"), ("Podcasts", "#8E44AD"), ("Rádio", "#5F6368")]:
        w = 50 + largura(n, 13.5, 500)
        c += ret(x, y, w, 38, 19, "#fff", 0.07, borda="#ffffff18") + icone(x + 19, y + 19, 11, cor, n[0]) + t(x + 36, y + 24, n, 13.5, 500)
        x += w + 8
        if x > L - 110:
            x, y = 24, y + 46
    c += ret(x, y, 96, 38, 19, LARANJA, 0.12, borda=LARANJA) + t(x + 48, y + 24, "+ Adicionar", 13, 600, LARANJA, ancora="middle")
    y += 80
    c += rotulo(28, y, "SOBE PARA O TOPO QUANDO")
    for i, (txt, on) in enumerate([("conectar o fone ou o carro", True), ("de manhã (6h – 9h)", True), ("estiver em casa à noite", False)]):
        yy = y + 36 + i * 46
        c += t(28, yy, txt, 15.5, 500, TEXTO, 0.9)
        c += ret(338, yy - 18, 46, 26, 13, LARANJA if on else "#fff", 1 if on else 0.15) + circ(371 if on else 351, yy - 5, 9, FUNDO if on else TEXTO)
    y += 190
    c += rotulo(28, y, "PRÉVIA")
    c += t(28, y + 30, "Mostrar o que está tocando", 15.5, 500, TEXTO, 0.9) + t(388, y + 30, "Spotify  ›", 13.5, 500, TEXTO, 0.5, "end")
    g.tela("08-editar-verbo", c, 0.45)


# ------------------------------------------------------------------ 9. ajustes
def ajustes():
    c = barra_status()
    c += t(24, 92, "‹", 26, 300) + t(52, 92, "Ajustes do WolfLauncher", 21, 600)
    # aviso de atualizacao (chega sozinho, pelo GitHub)
    c += ret(16, 112, L - 32, 70, 20, LARANJA, 0.12, borda="#F5A43266")
    c += t(34, 140, "⬆  WolfLauncher 0.2.0 disponível", 15, 600, LARANJA) + t(34, 162, "novo: modo noite e desenhar letras", 12.5, 400, TEXTO, 0.65)
    c += ret(L - 122, 132, 92, 30, 15, LARANJA) + t(L - 76, 152, "Atualizar", 13, 700, FUNDO, ancora="middle")

    def chave(y, titulo, sub, on):
        s = t(24, y, titulo, 15.5, 500) + t(24, y + 18, sub, 12, 400, TEXTO, 0.5)
        return s + ret(338, y - 17, 46, 26, 13, LARANJA if on else "#fff", 1 if on else 0.15) + circ(371 if on else 351, y - 4, 9, FUNDO if on else TEXTO)

    def grupo(y, txt):
        return t(24, y, txt, 11, 700, LARANJA, extra='letter-spacing="1.6"')

    y = 220
    c += grupo(y, "TELA INICIAL")
    c += chave(y + 34, "Verbos", "Conversar, Ouvir, Ir… (segure um para editar)", True)
    c += chave(y + 84, "Matilha", "as pessoas no polegar", True)
    c += chave(y + 134, "Ícones ao lado dos verbos", "só texto fica mais limpo", False)
    c += chave(y + 184, "Trilho de letras na borda", "além de desenhar a letra", False)
    y += 236
    c += grupo(y, "RITMO DO DIA")
    c += chave(y + 34, "Verbos mudam com a hora", "trabalho de tarde, música no fone", True)
    c += t(24, y + 84, "Modo noite", 15.5, 500) + t(24, y + 102, "calmo e em silêncio", 12, 400, TEXTO, 0.5) + t(388, y + 88, "22:30 – 07:00  ›", 13.5, 500, LILAS, ancora="end")
    y += 136
    c += grupo(y, "VISUAL")
    for i, cor in enumerate(["wall", LARANJA, AZUL, "#3DD68C", LILAS, "#FF6B81"]):
        cx = 40 + i * 54
        if cor == "wall":
            c += f'<defs><linearGradient id="gw"><stop offset="0" stop-color="{LARANJA}"/><stop offset="1" stop-color="{AZUL}"/></linearGradient></defs>'
            c += f'<circle cx="{cx}" cy="{y+34}" r="15" fill="url(#gw)"/>' + circ(cx, y + 34, 20, "none", extra=f'stroke="{TEXTO}" stroke-width="2"')
        else:
            c += circ(cx, y + 34, 15, cor)
    y += 74
    c += grupo(y, "ATUALIZAÇÕES")
    c += chave(y + 34, "Instalar sozinho", "baixa e instala quando o celular estiver parado", True)
    g.tela("09-ajustes", c, 0.92)


# ------------------------------------------------------------------ 10. mapa dos gestos
def gestos():
    c = barra_status(mostrar_hora=False)
    c += t(L / 2, 92, "Como se mexe", 24, 600, TEXTO, ancora="middle")
    c += t(L / 2, 116, "tudo parte da tela inicial", 13, 400, TEXTO, 0.5, "middle")
    # a tela inicial em miniatura no meio
    mx, my, mw, mh = 131, 330, 150, 260
    c += ret(mx, my, mw, mh, 18, SUP, 0.95, borda=LARANJA)
    c += t(mx + 14, my + 34, "14:32", 20, 300)
    for i, v in enumerate(["Conversar", "Trabalhar", "Ouvir", "Ir"]):
        c += t(mx + 14, my + 72 + i * 26, v, 14, 500 if i < 2 else 300, TEXTO, 0.9)
    for i in range(4):
        c += circ(mx + 22 + i * 24, my + 198, 8, [LARANJA, AZUL, LILAS, "#25D366"][i], 0.5)
    c += ret(mx + 10, my + 222, mw - 20, 24, 12, "#fff", 0.08, borda=LARANJA)

    def seta(x1, y1, x2, y2):
        return (f'<line x1="{x1}" y1="{y1}" x2="{x2}" y2="{y2}" stroke="{LARANJA}" stroke-width="2.5" '
                f'stroke-linecap="round" marker-end="url(#ponta)"/>')

    c += (f'<defs><marker id="ponta" viewBox="0 0 10 10" refX="5" refY="5" markerWidth="5" markerHeight="5" orient="auto">'
          f'<path d="M0 0 L10 5 L0 10 z" fill="{LARANJA}"/></marker></defs>')

    def caixa(x, y, titulo, sub, ancora="middle"):
        return t(x, y, titulo, 15, 700, TEXTO, ancora=ancora) + t(x, y + 18, sub, 12, 400, TEXTO, 0.55, ancora)

    c += seta(206, 318, 206, 258) + caixa(206, 214, "↑ Todos os apps", "a gaveta, por letra")
    c += seta(206, 602, 206, 650) + caixa(206, 676, "↓ Notificações", "a cortina do Android")
    c += seta(292, 460, 340, 460) + caixa(388, 430, "→ Seu dia", "linha do tempo", "end")
    c += seta(120, 460, 72, 460) + caixa(24, 430, "← Widgets", "o painel livre", "start")
    linhas = [
        ("✎", "Desenhar uma letra", "o leque de apps"),
        ("●", "Tocar num verbo", "pessoas e apps dele"),
        ("◉", "Segurar", "editar, mover, esconder"),
        ("◗", "Tocar duas vezes", "bloqueia a tela"),
    ]
    for i, (ic, a, b) in enumerate(linhas):
        x, y = 24 + (i % 2) * 190, 740 + (i // 2) * 56
        c += t(x, y, ic, 13, 700, LARANJA) + t(x + 20, y, a, 13.5, 600) + t(x + 20, y + 19, b, 11.5, 400, TEXTO, 0.55)
    g.tela("10-gestos", c, 0.9)


if __name__ == "__main__":
    inicio()
    letra()
    comando()
    com_nome("04-verbo-conversar", gc.c2_conversar)
    com_nome("05-seu-dia", gc.b1_rio)
    noite()
    com_nome("07-todos-os-apps", g.tela_gaveta)
    editar_verbo()
    ajustes()
    gestos()
