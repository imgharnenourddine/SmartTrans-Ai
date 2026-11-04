#!/usr/bin/env python3
"""Diagramme de séquence — question à l'assistant IA (SmartTrans-AI).

Canevas étroit et police large pour rester lisible à la largeur d'une page A4.
"""

import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent
OUT_REPORT = ROOT.parent / "rapport-stage" / "figures" / "diagramme-sequence.png"
OUT_DECK = ROOT / "assets" / "diagramme-sequence.png"

W, H = 1560, 1740

BG = (255, 255, 255, 255)
INK = (32, 38, 48, 255)
MUTED = (90, 100, 112, 255)
LIFE = (176, 186, 198, 255)
HEAD_FILL = (255, 255, 255, 255)
HEAD_LINE = (46, 58, 74, 255)
ACT_FILL = (232, 244, 248, 255)
ACT_LINE = (46, 124, 148, 255)
ARROW = (32, 38, 48, 255)
ALT_FILL = (247, 249, 252, 255)
ALT_LINE = (46, 124, 148, 255)
ALT_TAB = (46, 124, 148, 255)
GUARD = (46, 90, 110, 255)

FONT_REG = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
FONT_BOLD = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
FONT_OBL = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Oblique.ttf"


def fnt(size, kind="reg"):
    path = {"reg": FONT_REG, "bold": FONT_BOLD, "obl": FONT_OBL}[kind]
    return ImageFont.truetype(path, size)


def text_size(draw, text, font):
    bb = draw.textbbox((0, 0), text, font=font)
    return bb[2] - bb[0], bb[3] - bb[1]


def centered(draw, cx, cy, text, font, fill):
    tw, th = text_size(draw, text, font)
    draw.text((cx - tw / 2, cy - th / 2), text, font=font, fill=fill)


def dashed(draw, x1, y1, x2, y2, fill, width=2, dash=10, gap=7):
    dx, dy = x2 - x1, y2 - y1
    length = math.hypot(dx, dy)
    if length < 1:
        return
    ux, uy = dx / length, dy / length
    d = 0.0
    while d < length:
        d2 = min(d + dash, length)
        draw.line(
            [(x1 + ux * d, y1 + uy * d), (x1 + ux * d2, y1 + uy * d2)],
            fill=fill,
            width=width,
        )
        d += dash + gap


def arrow(draw, x1, x2, y, fill=ARROW, dashed_line=False, width=3):
    direction = 1 if x2 >= x1 else -1
    head = 14
    tip = x2
    shaft = x2 - direction * head
    if dashed_line:
        dashed(draw, x1, y, shaft, y, fill, width=width, dash=12, gap=7)
    else:
        draw.line([(x1, y), (shaft, y)], fill=fill, width=width)
    draw.polygon(
        [(tip, y), (tip - direction * head, y - 6), (tip - direction * head, y + 6)],
        fill=fill,
    )


def main():
    img = Image.new("RGBA", (W, H), BG)
    draw = ImageDraw.Draw(img)

    name_f = fnt(22, "bold")
    stereo_f = fnt(15, "reg")
    msg_f = fnt(22, "reg")
    guard_f = fnt(18, "obl")
    tab_f = fnt(18, "bold")

    names = [
        ("Gestionnaire", "acteur"),
        ("React", "interface"),
        ("Filtre JWT", "sécurité"),
        ("Service", "assistant"),
        ("PostgreSQL", "données"),
        ("OpenAI", "externe"),
    ]
    box_w, box_h = 210, 64
    gap = (W - 36 - len(names) * box_w) / (len(names) - 1)
    lefts = [18 + i * (box_w + gap) for i in range(len(names))]
    cx = [x + box_w / 2 for x in lefts]
    head_y = 16

    outer = (14, 430, W - 14, 1688)
    inner = (cx[3] - 36, 1030, W - 22, 1420)
    draw.rounded_rectangle(outer, radius=10, fill=ALT_FILL, outline=ALT_LINE, width=2)
    draw.rounded_rectangle(inner, radius=8, fill=(255, 255, 255, 255), outline=ALT_LINE, width=2)

    life_top = head_y + box_h
    for x in cx:
        dashed(draw, x, life_top, x, H - 18, LIFE, width=2, dash=7, gap=6)

    def activation(index, y1, y2):
        x = cx[index]
        draw.rectangle((x - 7, y1, x + 7, y2), fill=ACT_FILL, outline=ACT_LINE, width=2)

    activation(1, 168, 1628)
    activation(2, 248, 1560)
    activation(3, 788, 1560)
    activation(4, 878, 968)
    activation(5, 1128, 1210)

    for x, (name, stereo) in zip(lefts, names):
        draw.rounded_rectangle(
            (x, head_y, x + box_w, head_y + box_h),
            radius=8,
            fill=HEAD_FILL,
            outline=HEAD_LINE,
            width=3,
        )
        centered(draw, x + box_w / 2, head_y + 22, f"« {stereo} »", stereo_f, MUTED)
        centered(draw, x + box_w / 2, head_y + 44, name, name_f, INK)

    def msg(a, b, y, text, ret=False):
        x1, x2 = cx[a], cx[b]
        if x2 > x1:
            x1 += 8
            x2 -= 8
        else:
            x1 -= 8
            x2 += 8
        arrow(draw, x1, x2, y, dashed_line=ret)
        tw, th = text_size(draw, text, msg_f)
        draw.text(((cx[a] + cx[b]) / 2 - tw / 2, y - th - 8), text, font=msg_f, fill=INK)

    def self_msg(index, y, text):
        x = cx[index] + 8
        w, h = 46, 32
        draw.line([(x, y), (x + w, y)], fill=ARROW, width=3)
        draw.line([(x + w, y), (x + w, y + h)], fill=ARROW, width=3)
        arrow(draw, x + w, x, y + h)
        tw, th = text_size(draw, text, msg_f)
        draw.text((x + w + 10, y + (h - th) / 2), text, font=msg_f, fill=INK)

    msg(0, 1, 188, "Pose une question")
    msg(1, 2, 268, "POST /assistant/ask")
    self_msg(2, 348, "Valider le JWT")

    dashed(draw, outer[0] + 12, 700, outer[2] - 12, 700, ALT_LINE, width=2, dash=9, gap=7)
    draw.text((outer[0] + 18, 454), "[ jeton invalide ou expiré ]", font=guard_f, fill=GUARD)
    msg(2, 1, 560, "401 Unauthorized", ret=True)
    msg(1, 0, 640, "Retour à la connexion", ret=True)

    draw.text((outer[0] + 18, 718), "[ jeton valide ]", font=guard_f, fill=GUARD)
    msg(2, 3, 808, "ask(message)")
    msg(3, 4, 888, "Lire le contexte")
    msg(4, 3, 968, "Données métier", ret=True)

    dashed(draw, inner[0] + 12, 1280, inner[2] - 12, 1280, ALT_LINE, width=2, dash=9, gap=7)
    draw.text((inner[0] + 16, 1050), "[ clé OpenAI présente ]", font=guard_f, fill=GUARD)
    msg(3, 5, 1138, "POST /chat/completions")
    msg(5, 3, 1220, "Réponse LLM", ret=True)

    draw.text((inner[0] + 16, 1300), "[ clé absente ou échec ]", font=guard_f, fill=GUARD)
    self_msg(3, 1360, "Mode GROUNDED")

    msg(3, 1, 1520, "answer, mode, citations", ret=True)
    msg(1, 0, 1610, "Afficher la réponse", ret=True)

    def tab(box):
        x, y = box[0], box[1]
        draw.rounded_rectangle((x, y - 16, x + 58, y + 18), radius=5, fill=ALT_TAB)
        centered(draw, x + 29, y + 1, "alt", tab_f, (255, 255, 255, 255))

    tab(outer)
    tab(inner)

    OUT_REPORT.parent.mkdir(parents=True, exist_ok=True)
    OUT_DECK.parent.mkdir(parents=True, exist_ok=True)
    img.save(OUT_REPORT, "PNG")
    img.save(OUT_DECK, "PNG")
    print(OUT_REPORT)


if __name__ == "__main__":
    main()
