#!/usr/bin/env python3
"""Schéma entité-association SmartTrans-AI — PNG transparent."""

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

OUT = Path(__file__).resolve().parent / "assets" / "modele-donnees.png"

W, H = 2280, 1180
HEADER_H = 46
ROW_H = 30
RADIUS = 16
LINE = (200, 206, 214, 255)
HEADER_BG = (92, 102, 114, 255)
HEADER_FG = (255, 255, 255, 255)
ROW_BG = (255, 255, 255, 255)
ROW_ALT = (244, 246, 248, 255)
NAME_FG = (40, 46, 54, 255)
TYPE_FG = (118, 126, 136, 255)
REL = (186, 192, 200, 255)

FONT_REG = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
FONT_BOLD = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"


def fnt(size, bold=False):
    return ImageFont.truetype(FONT_BOLD if bold else FONT_REG, size)


def table_h(n):
    return HEADER_H + n * ROW_H + 6


def draw_table(draw, x, y, width, title, fields, fonts):
    n = len(fields)
    th = table_h(n)
    draw.rounded_rectangle((x, y, x + width, y + th), radius=RADIUS, fill=ROW_BG, outline=LINE, width=2)
    draw.rounded_rectangle((x, y, x + width, y + HEADER_H), radius=RADIUS, fill=HEADER_BG)
    draw.rectangle((x, y + HEADER_H - 14, x + width, y + HEADER_H), fill=HEADER_BG)
    bb = fonts["title"].getbbox(title)
    tw = bb[2] - bb[0]
    draw.text((x + (width - tw) / 2, y + 11), title, font=fonts["title"], fill=HEADER_FG)
    split = int(width * 0.58)
    for i, (name, typ) in enumerate(fields):
        top = y + HEADER_H + i * ROW_H
        bg = ROW_ALT if i % 2 else ROW_BG
        draw.rectangle((x + 2, top, x + width - 2, min(top + ROW_H, y + th - 2)), fill=bg)
        draw.line((x + split, top, x + split, top + ROW_H), fill=LINE, width=1)
        draw.text((x + 14, top + 6), name, font=fonts["field"], fill=NAME_FG)
        draw.text((x + split + 10, top + 6), typ, font=fonts["type"], fill=TYPE_FG)
    draw.line((x + 2, y + HEADER_H, x + width - 2, y + HEADER_H), fill=LINE, width=1)
    return {"x": x, "y": y, "w": width, "h": th}


def pt(box, side):
    x, y, w, h = box["x"], box["y"], box["w"], box["h"]
    return {
        "L": (x, y + h / 2),
        "R": (x + w, y + h / 2),
        "T": (x + w / 2, y),
        "B": (x + w / 2, y + h),
        "TL": (x + w * 0.28, y),
        "TR": (x + w * 0.72, y),
        "BL": (x + w * 0.28, y + h),
        "BR": (x + w * 0.72, y + h),
        "LT": (x, y + h * 0.35),
        "LB": (x, y + h * 0.7),
        "RT": (x + w, y + h * 0.35),
        "RB": (x + w, y + h * 0.7),
    }[side]


def tick_one(draw, x, y, axis):
    if axis == "v":
        draw.line([(x, y - 9), (x, y + 9)], fill=REL, width=3)
    else:
        draw.line([(x - 9, y), (x + 9, y)], fill=REL, width=3)


def tick_many(draw, x, y, toward):
    s = 12
    if toward == "R":
        draw.line([(x, y), (x + s, y - s)], fill=REL, width=2)
        draw.line([(x, y), (x + s, y + s)], fill=REL, width=2)
        draw.line([(x, y - s), (x, y + s)], fill=REL, width=2)
    elif toward == "L":
        draw.line([(x, y), (x - s, y - s)], fill=REL, width=2)
        draw.line([(x, y), (x - s, y + s)], fill=REL, width=2)
        draw.line([(x, y - s), (x, y + s)], fill=REL, width=2)
    elif toward == "D":
        draw.line([(x, y), (x - s, y + s)], fill=REL, width=2)
        draw.line([(x, y), (x + s, y + s)], fill=REL, width=2)
        draw.line([(x - s, y), (x + s, y)], fill=REL, width=2)
    else:
        draw.line([(x, y), (x - s, y - s)], fill=REL, width=2)
        draw.line([(x, y), (x + s, y - s)], fill=REL, width=2)
        draw.line([(x - s, y), (x + s, y)], fill=REL, width=2)


def line(draw, points):
    pts = [(int(p[0]), int(p[1])) for p in points]
    draw.line(pts, fill=REL, width=2)


def build():
    OUT.parent.mkdir(parents=True, exist_ok=True)
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    fonts = {
        "title": fnt(20, True),
        "field": fnt(16),
        "type": fnt(15),
        "cap": fnt(15),
    }

    users = draw_table(draw, 70, 80, 310, "users", [
        ("id", "UUID"),
        ("email", "Varchar"),
        ("first_name", "Varchar"),
        ("enabled", "Booléen"),
    ], fonts)
    roles = draw_table(draw, 70, 430, 310, "roles", [
        ("id", "UUID"),
        ("code", "Varchar"),
        ("label", "Varchar"),
    ], fonts)
    user_roles = draw_table(draw, 70, 780, 310, "user_roles", [
        ("user_id", "UUID"),
        ("role_id", "UUID"),
    ], fonts)

    clients = draw_table(draw, 520, 80, 320, "clients", [
        ("id", "UUID"),
        ("company_name", "Varchar"),
        ("phone", "Varchar"),
        ("email", "Varchar"),
    ], fonts)
    drivers = draw_table(draw, 520, 430, 320, "drivers", [
        ("id", "UUID"),
        ("user_id", "UUID"),
        ("license_number", "Varchar"),
        ("status", "Varchar"),
    ], fonts)

    vehicles = draw_table(draw, 980, 80, 360, "vehicles", [
        ("id", "UUID"),
        ("registration_number", "Varchar"),
        ("type", "Varchar"),
        ("status", "Varchar"),
        ("insurance_expires_at", "Date"),
    ], fonts)
    missions = draw_table(draw, 980, 430, 360, "missions", [
        ("id", "UUID"),
        ("reference", "Varchar"),
        ("client_id", "UUID"),
        ("driver_id", "UUID"),
        ("vehicle_id", "UUID"),
        ("status", "Varchar"),
    ], fonts)

    activities = draw_table(draw, 1520, 430, 340, "activities", [
        ("id", "UUID"),
        ("actor_user_id", "UUID"),
        ("action", "Varchar"),
        ("entity_type", "Varchar"),
        ("created_at", "DateHeure"),
    ], fonts)

    # users 1 — n user_roles  (bus à gauche, sans traverser roles)
    bus = 42
    pu_l, pur_l = pt(users, "L"), pt(user_roles, "L")
    line(draw, [pu_l, (bus, pu_l[1]), (bus, pur_l[1]), pur_l])
    tick_one(draw, pu_l[0] - 2, pu_l[1], "v")
    tick_many(draw, pur_l[0], pur_l[1], "L")

    pr_l = pt(roles, "L")
    line(draw, [pr_l, (bus, pr_l[1])])
    tick_one(draw, pr_l[0] - 2, pr_l[1], "v")

    # users 0..1 — 0..1 drivers
    pu, pd = pt(users, "RT"), pt(drivers, "LT")
    line(draw, [pu, pd])
    tick_one(draw, pu[0] + 2, pu[1], "v")
    tick_one(draw, pd[0] - 2, pd[1], "v")
    draw.text(((pu[0] + pd[0]) / 2 - 18, pu[1] - 24), "0..1", font=fonts["cap"], fill=TYPE_FG)

    # clients 1 — n missions (couloir entre colonnes)
    pc, pm = pt(clients, "R"), pt(missions, "TL")
    gap_x = 900
    line(draw, [pc, (gap_x, pc[1]), (gap_x, pm[1] - 24), (pm[0], pm[1] - 24), pm])
    tick_one(draw, pc[0] + 2, pc[1], "v")
    tick_many(draw, pm[0], pm[1], "U")

    # vehicles 1 — n missions (même colonne)
    pv, pm2 = pt(vehicles, "B"), pt(missions, "T")
    line(draw, [pv, pm2])
    tick_one(draw, pv[0], pv[1] + 2, "h")
    tick_many(draw, pm2[0], pm2[1], "U")

    # drivers 1 — n missions
    pdr, pmm = pt(drivers, "R"), pt(missions, "L")
    line(draw, [pdr, pmm])
    tick_one(draw, pdr[0] + 2, pdr[1], "v")
    tick_many(draw, pmm[0], pmm[1], "L")

    # users 1 — n activities (actor), couloir haut
    pa_t = pt(activities, "T")
    pu_t = pt(users, "T")
    y_top = 28
    line(draw, [pu_t, (pu_t[0], y_top), (pa_t[0], y_top), pa_t])
    tick_one(draw, pu_t[0], pu_t[1] - 2, "h")
    tick_many(draw, pa_t[0], pa_t[1], "U")
    draw.text((pa_t[0] - 28, y_top + 6), "actor", font=fonts["cap"], fill=TYPE_FG)

    img.save(OUT, "PNG")
    print(f"Écrit : {OUT} ({W}x{H}, transparent)")


if __name__ == "__main__":
    build()
