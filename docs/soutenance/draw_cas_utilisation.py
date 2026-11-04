#!/usr/bin/env python3
"""Diagramme de cas d'utilisation UML — SmartTrans-AI."""

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent
OUT_REPORT = ROOT.parent / "rapport-stage" / "figures" / "diagramme-cas-utilisation.png"
OUT_DECK = ROOT / "assets" / "diagramme-cas-utilisation.png"

W, H = 2600, 1720

# Rapport (papier blanc)
PALETTE_REPORT = {
    "BG": (255, 255, 255, 255),
    "INK": (32, 38, 48, 255),
    "MUTED": (90, 100, 112, 255),
    "LINE": (120, 132, 148, 255),
    "SYS": (46, 58, 74, 255),
    "SYS_FILL": (255, 255, 255, 255),
    "PKG_FILL": (246, 248, 251, 255),
    "PKG_LINE": (176, 186, 198, 255),
    "TAB_FILL": (255, 255, 255, 255),
    "UC_FILL": (255, 255, 255, 255),
    "UC_LINE": (46, 124, 148, 255),
    "TITLE": (22, 28, 38, 255),
}

# Soutenance : fond transparent + verts SmartTrans (lisible sur diapo sombre)
PALETTE_DECK = {
    "BG": (0, 0, 0, 0),
    "INK": (236, 244, 242, 255),
    "MUTED": (168, 196, 188, 255),
    "LINE": (15, 138, 106, 200),
    "SYS": (15, 138, 106, 255),
    "SYS_FILL": (13, 36, 48, 50),
    "PKG_FILL": (13, 36, 48, 95),
    "PKG_LINE": (15, 138, 106, 210),
    "TAB_FILL": (13, 36, 48, 200),
    "UC_FILL": (13, 36, 48, 155),
    "UC_LINE": (15, 138, 106, 255),
    "TITLE": (255, 255, 255, 255),
}

BG = INK = MUTED = LINE = SYS = SYS_FILL = PKG_FILL = PKG_LINE = TAB_FILL = UC_FILL = UC_LINE = TITLE = None


def apply_palette(p):
    g = globals()
    for k, v in p.items():
        g[k] = v

FONT_REG = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
FONT_BOLD = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"


def fnt(size, bold=False):
    return ImageFont.truetype(FONT_BOLD if bold else FONT_REG, size)


def text_size(draw, text, font):
    bb = draw.textbbox((0, 0), text, font=font)
    return bb[2] - bb[0], bb[3] - bb[1]


def centered(draw, cx, cy, text, font, fill):
    tw, th = text_size(draw, text, font)
    draw.text((cx - tw / 2, cy - th / 2), text, font=font, fill=fill)


def multiline_centered(draw, cx, cy, lines, font, fill, gap=4):
    sizes = [text_size(draw, ln, font) for ln in lines]
    total_h = sum(h for _, h in sizes) + gap * (len(lines) - 1)
    y = cy - total_h / 2
    for (tw, th), ln in zip(sizes, lines):
        draw.text((cx - tw / 2, y), ln, font=font, fill=fill)
        y += th + gap


def draw_actor(draw, cx, y_head, label_lines, fonts, scale=1.0):
    r = int(26 * scale)
    body = int(78 * scale)
    arm_y = y_head + r + int(22 * scale)
    arm_w = int(38 * scale)
    leg_h = int(46 * scale)
    leg_w = int(28 * scale)
    body_top = y_head + 2 * r
    body_bot = body_top + body
    draw.ellipse((cx - r, y_head, cx + r, y_head + 2 * r), outline=INK, width=3)
    draw.line((cx, body_top, cx, body_bot), fill=INK, width=3)
    draw.line((cx - arm_w, arm_y, cx + arm_w, arm_y), fill=INK, width=3)
    draw.line((cx, body_bot, cx - leg_w, body_bot + leg_h), fill=INK, width=3)
    draw.line((cx, body_bot, cx + leg_w, body_bot + leg_h), fill=INK, width=3)
    multiline_centered(draw, cx, body_bot + leg_h + 32, label_lines, fonts["actor"], INK, gap=2)
    return {"hand_r": (cx + arm_w, arm_y), "hand_l": (cx - arm_w, arm_y)}


def draw_package(draw, box, title, fonts):
    x0, y0, x1, y1 = box
    draw.rounded_rectangle((x0, y0, x1, y1), radius=18, fill=PKG_FILL, outline=PKG_LINE, width=2)
    tw, th = text_size(draw, title, fonts["pkg"])
    pad_x, pad_y = 14, 8
    tab = (x0 + 18, y0 - th - pad_y * 2 + 6, x0 + 18 + tw + pad_x * 2, y0 + 8)
    draw.rounded_rectangle(tab, radius=8, fill=TAB_FILL, outline=PKG_LINE, width=2)
    draw.text((tab[0] + pad_x, tab[1] + pad_y - 2), title, font=fonts["pkg"], fill=MUTED)


def draw_usecase(draw, uc, fonts):
    cx, cy, w, h = uc["cx"], uc["cy"], uc["w"], uc["h"]
    x0, y0, x1, y1 = cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2
    draw.ellipse((x0, y0, x1, y1), fill=UC_FILL, outline=UC_LINE, width=3)
    multiline_centered(draw, cx, cy, uc["lines"], fonts["uc"], INK, gap=2)


def polyline(draw, points, width=2):
    draw.line([(int(p[0]), int(p[1])) for p in points], fill=LINE, width=width)


def uc_geom(cx, cy, w, h, lines):
    return {
        "cx": cx,
        "cy": cy,
        "w": w,
        "h": h,
        "lines": lines,
        "l": (cx - w / 2, cy),
        "r": (cx + w / 2, cy),
    }


def from_left(draw, actor, uc, rail_x, via=None):
    ax, ay = actor["hand_r"]
    if via:
        pts = [(ax, ay), (rail_x, ay), (rail_x, via[0][1]), *via, uc["l"]]
    else:
        pts = [(ax, ay), (rail_x, ay), (rail_x, uc["cy"]), uc["l"]]
    polyline(draw, pts)


def from_right(draw, actor, uc, rail_x, via=None):
    ax, ay = actor["hand_l"]
    if via:
        pts = [(ax, ay), (rail_x, ay), (rail_x, via[0][1]), *via, uc["r"]]
    else:
        pts = [(ax, ay), (rail_x, ay), (rail_x, uc["cy"]), uc["r"]]
    polyline(draw, pts)


def render(palette):
    apply_palette(palette)
    img = Image.new("RGBA", (W, H), BG)
    draw = ImageDraw.Draw(img)
    fonts = {
        "title": fnt(34, True),
        "sub": fnt(16),
        "sys": fnt(20, True),
        "pkg": fnt(15, True),
        "uc": fnt(16),
        "actor": fnt(17, True),
        "note": fnt(14),
    }

    centered(draw, W / 2, 48, "SmartTrans-AI — Diagramme de cas d'utilisation", fonts["title"], TITLE)
    centered(
        draw,
        W / 2,
        88,
        "UML 2  ·  Acteurs : Administrateur (ADMIN), Responsable transport (TRANSPORT_MANAGER), Chauffeur (DRIVER)",
        fonts["sub"],
        MUTED,
    )

    sys = (430, 150, 2170, 1595)
    draw.rounded_rectangle(sys, radius=22, fill=SYS_FILL, outline=SYS, width=3)
    centered(draw, (sys[0] + sys[2]) / 2, sys[1] + 28, "SmartTrans-AI", fonts["sys"], SYS)

    admin = draw_actor(draw, 170, 280, ["Administrateur"], fonts, 1.05)
    manager = draw_actor(draw, 170, 1080, ["Responsable", "transport"], fonts, 1.05)
    driver = draw_actor(draw, 2430, 900, ["Chauffeur"], fonts, 1.05)

    commun = (500, 235, 2100, 475)
    metier = (500, 535, 2100, 1015)
    admin_pkg = (500, 1095, 1100, 1535)
    driver_pkg = (1180, 1095, 2100, 1535)

    draw_package(draw, commun, "Commun", fonts)
    draw_package(draw, metier, "Métier (admin et responsable)", fonts)
    draw_package(draw, admin_pkg, "Administration", fonts)
    draw_package(draw, driver_pkg, "Espace chauffeur", fonts)

    uc_login = uc_geom(780, 365, 400, 145, ["S'authentifier (JWT)"])
    uc_dash = uc_geom(1640, 365, 440, 145, ["Tableau de bord", "et alertes d'expiration"])
    uc_clients = uc_geom(760, 690, 340, 125, ["Gérer les clients"])
    uc_drivers = uc_geom(1300, 690, 360, 125, ["Gérer les chauffeurs"])
    uc_vehicles = uc_geom(1840, 690, 360, 125, ["Gérer les véhicules"])
    uc_missions = uc_geom(760, 900, 380, 135, ["Planifier et suivre", "les missions"])
    uc_act = uc_geom(1300, 900, 380, 135, ["Journal d'activités", "(lecture seule)"])
    uc_ai = uc_geom(1840, 900, 360, 135, ["Assistant IA"])
    uc_users = uc_geom(800, 1325, 400, 145, ["Gérer les utilisateurs", "et les rôles"])
    uc_mym = uc_geom(1680, 1210, 340, 120, ["Consulter mes missions"])
    uc_stat = uc_geom(1680, 1350, 360, 120, ["Démarrer / terminer", "une mission"])
    uc_prof = uc_geom(1680, 1485, 320, 110, ["Mon profil"])

    left_rail, right_rail = 360, 2240
    shelf_c, gutter_c = 258, 1180
    shelf_m, gutter_m1, gutter_m2 = 560, 1035, 1575

    def left_direct(actor, uc):
        from_left(draw, actor, uc, left_rail)

    def left_via(actor, uc, gutter, shelf):
        from_left(draw, actor, uc, left_rail, via=[(gutter, shelf), (gutter, uc["cy"])])

    def right_direct(actor, uc):
        from_right(draw, actor, uc, right_rail)

    def right_via(actor, uc, gutter, shelf):
        from_right(draw, actor, uc, right_rail, via=[(gutter, shelf), (gutter, uc["cy"])])

    for who in (admin, manager):
        left_direct(who, uc_login)
        left_via(who, uc_dash, gutter_c, shelf_c)
        left_direct(who, uc_clients)
        left_direct(who, uc_missions)
        left_via(who, uc_drivers, gutter_m1, shelf_m)
        left_via(who, uc_act, gutter_m1, shelf_m)
        left_via(who, uc_vehicles, gutter_m2, shelf_m)
        left_via(who, uc_ai, gutter_m2, shelf_m)
    from_left(draw, admin, uc_users, 455)

    right_via(driver, uc_login, gutter_c, shelf_c)
    right_direct(driver, uc_dash)
    right_direct(driver, uc_mym)
    right_direct(driver, uc_stat)
    right_direct(driver, uc_prof)

    for uc in (
        uc_login,
        uc_dash,
        uc_clients,
        uc_drivers,
        uc_vehicles,
        uc_missions,
        uc_act,
        uc_ai,
        uc_users,
        uc_mym,
        uc_stat,
        uc_prof,
    ):
        draw_usecase(draw, uc, fonts)

    note = "Le chauffeur ne gère pas le référentiel. Le journal est alimenté automatiquement (pas de CRUD)."
    centered(draw, W / 2, H - 28, note, fonts["note"], MUTED)

    return img


def build():
    report = render(PALETTE_REPORT)
    OUT_REPORT.parent.mkdir(parents=True, exist_ok=True)
    report.save(OUT_REPORT, "PNG")
    print(f"Écrit : {OUT_REPORT} (fond blanc, rapport)")

    deck = render(PALETTE_DECK)
    OUT_DECK.parent.mkdir(parents=True, exist_ok=True)
    deck.save(OUT_DECK, "PNG")
    print(f"Écrit : {OUT_DECK} (fond transparent, soutenance)")


if __name__ == "__main__":
    build()
