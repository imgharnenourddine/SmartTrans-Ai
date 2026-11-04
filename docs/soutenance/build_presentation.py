#!/usr/bin/env python3
"""Soutenance SmartTrans-AI — textes courts, couleurs du produit."""

from __future__ import annotations

import colorsys
import io
import re
import shutil
import zipfile
from pathlib import Path

from pptx import Presentation
from pptx.oxml.ns import qn
from pptx.util import Inches

ROOT = Path(__file__).resolve().parents[2]
FIG = ROOT / "docs" / "rapport-stage" / "figures"
THEME = Path("/home/flow/AI Editor Software Pitch Deck by Slidesgo.pptx")
OUT = Path(__file__).resolve().parent / "SmartTrans-AI-soutenance.pptx"


def delete_all_slides(prs: Presentation) -> None:
    sld_id_lst = prs.slides._sldIdLst
    for sld_id in list(sld_id_lst):
        r_id = sld_id.get(qn("r:id"))
        prs.part.drop_rel(r_id)
        sld_id_lst.remove(sld_id)


def set_text(shape, text: str) -> None:
    tf = shape.text_frame
    tf.word_wrap = True
    shape.text = text


def ph(slide, idx: int):
    return slide.placeholders[idx]


def add(prs: Presentation, layout_name: str):
    layout = next(l for l in prs.slide_layouts if l.name == layout_name)
    return prs.slides.add_slide(layout)


def place_picture_contain(slide, placeholder, path: Path) -> None:
    """Affiche toute l'image dans le cadre (sans couper le haut / le titre)."""
    from PIL import Image

    box_l, box_t = int(placeholder.left), int(placeholder.top)
    box_w, box_h = int(placeholder.width), int(placeholder.height)
    iw, ih = Image.open(path).size
    img_ratio = iw / ih
    box_ratio = box_w / box_h
    if img_ratio > box_ratio:
        w = box_w
        h = int(box_w / img_ratio)
    else:
        h = box_h
        w = int(box_h * img_ratio)
    left = box_l + (box_w - w) // 2
    top = box_t + (box_h - h) // 2
    slide.shapes.add_picture(str(path), left, top, width=w, height=h)
    placeholder.left = Inches(-12)
    placeholder.top = Inches(-12)
    placeholder.width = Inches(0.1)
    placeholder.height = Inches(0.1)


def fit_picture(slide, path: Path, top_in=1.25, max_h=3.85, max_w=8.4) -> None:
    from PIL import Image

    img = Image.open(path)
    w_px, h_px = img.size
    max_w_emu = Inches(max_w)
    max_h_emu = Inches(max_h)
    ratio = w_px / h_px
    width = max_w_emu
    height = int(width / ratio)
    if height > max_h_emu:
        height = max_h_emu
        width = int(height * ratio)
    left = int((Inches(10) - width) / 2)
    slide.shapes.add_picture(str(path), left, Inches(top_in), width=width, height=height)


def _hex_to_rgb(h: str) -> tuple[float, float, float]:
    return int(h[0:2], 16) / 255.0, int(h[2:4], 16) / 255.0, int(h[4:6], 16) / 255.0


def smarttrans_hex(src: str) -> str:
    """Mappe une couleur du deck vers la palette produit (vert #0F8A6A, fonds #EDF3F2)."""
    src = src.upper()
    if src in {"000000", "FFFFFF", "FEFEFE", "191919", "2B2B2B"}:
        return src
    r, g, b = _hex_to_rgb(src)
    h, s, v = colorsys.rgb_to_hsv(r, g, b)
    hue = h * 360
    if s < 0.04:
        return src

    cool = hue >= 90 or hue < 55
    if not cool:
        return src

    if v >= 0.93 and s < 0.35:
        return "EDF3F2"
    if v >= 0.82:
        return "D8F3EA"
    if v >= 0.55:
        return "0F8A6A"
    if v >= 0.32:
        return "0B6B52"
    return "0D2430"


THEME_SCHEMES = {
    "ppt/theme/theme1.xml": (
        '<a:clrScheme name="SmartTrans">'
        '<a:dk1><a:srgbClr val="FFFFFF"/></a:dk1>'
        '<a:lt1><a:srgbClr val="0D2430"/></a:lt1>'
        '<a:dk2><a:srgbClr val="0F8A6A"/></a:dk2>'
        '<a:lt2><a:srgbClr val="D8F3EA"/></a:lt2>'
        '<a:accent1><a:srgbClr val="0F8A6A"/></a:accent1>'
        '<a:accent2><a:srgbClr val="184456"/></a:accent2>'
        '<a:accent3><a:srgbClr val="2B2B2B"/></a:accent3>'
        '<a:accent4><a:srgbClr val="FFFFFF"/></a:accent4>'
        '<a:accent5><a:srgbClr val="FFFFFF"/></a:accent5>'
        '<a:accent6><a:srgbClr val="FFFFFF"/></a:accent6>'
        '<a:hlink><a:srgbClr val="FFFFFF"/></a:hlink>'
        '<a:folHlink><a:srgbClr val="0B6B52"/></a:folHlink>'
        "</a:clrScheme>"
    ),
    "ppt/theme/theme2.xml": (
        '<a:clrScheme name="SmartTrans">'
        '<a:dk1><a:srgbClr val="0D2430"/></a:dk1>'
        '<a:lt1><a:srgbClr val="FFFFFF"/></a:lt1>'
        '<a:dk2><a:srgbClr val="5B6F79"/></a:dk2>'
        '<a:lt2><a:srgbClr val="EDF3F2"/></a:lt2>'
        '<a:accent1><a:srgbClr val="0F8A6A"/></a:accent1>'
        '<a:accent2><a:srgbClr val="D8F3EA"/></a:accent2>'
        '<a:accent3><a:srgbClr val="184456"/></a:accent3>'
        '<a:accent4><a:srgbClr val="FFFFFF"/></a:accent4>'
        '<a:accent5><a:srgbClr val="FFFFFF"/></a:accent5>'
        '<a:accent6><a:srgbClr val="FFFFFF"/></a:accent6>'
        '<a:hlink><a:srgbClr val="0F8A6A"/></a:hlink>'
        '<a:folHlink><a:srgbClr val="0B6B52"/></a:folHlink>'
        "</a:clrScheme>"
    ),
    "ppt/theme/theme3.xml": (
        '<a:clrScheme name="SmartTrans">'
        '<a:dk1><a:srgbClr val="102029"/></a:dk1>'
        '<a:lt1><a:srgbClr val="FFFFFF"/></a:lt1>'
        '<a:dk2><a:srgbClr val="0B6B52"/></a:dk2>'
        '<a:lt2><a:srgbClr val="EDF3F2"/></a:lt2>'
        '<a:accent1><a:srgbClr val="0F8A6A"/></a:accent1>'
        '<a:accent2><a:srgbClr val="184456"/></a:accent2>'
        '<a:accent3><a:srgbClr val="123040"/></a:accent3>'
        '<a:accent4><a:srgbClr val="D8F3EA"/></a:accent4>'
        '<a:accent5><a:srgbClr val="5B6F79"/></a:accent5>'
        '<a:accent6><a:srgbClr val="0B6B52"/></a:accent6>'
        '<a:hlink><a:srgbClr val="0F8A6A"/></a:hlink>'
        '<a:folHlink><a:srgbClr val="0B6B52"/></a:folHlink>'
        "</a:clrScheme>"
    ),
}


def _recolor_decor_image(data: bytes, filename: str) -> bytes:
    """Passe les blobs violets du template en vert SmartTrans. Ne touche pas aux captures."""
    from PIL import Image

    name = Path(filename).name.lower()
    if name in {"image7.jpg", "image8.png", "image9.png", "image10.png", "image11.png", "image12.png"}:
        return data
    if not name.endswith((".png", ".jpg", ".jpeg")):
        return data
    try:
        im = Image.open(io.BytesIO(data))
    except Exception:
        return data
    src_mode = im.mode
    alpha = im.split()[-1] if src_mode in {"RGBA", "LA"} else None
    hsv = im.convert("RGB").convert("HSV")
    h_b, s_b, v_b = hsv.split()
    try:
        import numpy as np

        h = np.array(h_b)
        s = np.array(s_b)
        target = int(164 / 360 * 255)
        h = np.where(s > 24, target, h).astype("uint8")
        h_b = Image.fromarray(h, mode="L")
    except ImportError:
        target = int(164 / 360 * 255)
        h_px = bytearray(h_b.tobytes())
        s_px = s_b.tobytes()
        for i, sat in enumerate(s_px):
            if sat > 24:
                h_px[i] = target
        h_b = Image.frombytes("L", h_b.size, bytes(h_px))
    rgb = Image.merge("HSV", (h_b, s_b, v_b)).convert("RGB")
    buf = io.BytesIO()
    if name.endswith(".png"):
        if alpha is not None:
            rgb.putalpha(alpha)
        rgb.save(buf, format="PNG")
    else:
        rgb.save(buf, format="JPEG", quality=92)
    return buf.getvalue()


def apply_smarttrans_theme(pptx_path: Path) -> None:
    """Recolore le thème, les masques et les images décoratives violettes."""
    srgb = re.compile(r'(<a:srgbClr val=")([0-9A-Fa-f]{6})(")')
    scheme = re.compile(r"<a:clrScheme[^>]*>.*?</a:clrScheme>", re.DOTALL)
    buf = io.BytesIO()
    with zipfile.ZipFile(pptx_path, "r") as zin, zipfile.ZipFile(buf, "w", zipfile.ZIP_DEFLATED) as zout:
        for info in zin.infolist():
            data = zin.read(info.filename)
            if info.filename.startswith("ppt/media/"):
                data = _recolor_decor_image(data, info.filename)
            elif info.filename.endswith(".xml"):
                text = data.decode("utf-8")

                def repl(m: re.Match[str]) -> str:
                    return m.group(1) + smarttrans_hex(m.group(2)) + m.group(3)

                text = srgb.sub(repl, text)
                if info.filename in THEME_SCHEMES:
                    text = scheme.sub(THEME_SCHEMES[info.filename], text, count=1)
                data = text.encode("utf-8")
            zout.writestr(info, data)
    pptx_path.write_bytes(buf.getvalue())


def build() -> None:
    shutil.copy(THEME, OUT)
    prs = Presentation(OUT)
    delete_all_slides(prs)

    # 1 — Titre
    s = add(prs, "TITLE")
    set_text(ph(s, 0), "SmartTrans-AI")
    set_text(ph(s, 1), "Soutenance de stage  ·  Hamza Bendaib  ·  ENSIASD 2025–2026")

    # 2 — Plan
    s = add(prs, "BLANK_1_1_1_1_1_1")
    set_text(ph(s, 0), "Plan")
    set_text(ph(s, 2), "01")
    set_text(ph(s, 4), "02")
    set_text(ph(s, 6), "03")
    set_text(ph(s, 3), "04")
    set_text(ph(s, 5), "05")
    set_text(ph(s, 7), "06")
    set_text(ph(s, 1), "Contexte")
    set_text(ph(s, 8), "Problème")
    set_text(ph(s, 9), "Conception")
    set_text(ph(s, 13), "Produit")
    set_text(ph(s, 14), "Tests")
    set_text(ph(s, 15), "Bilan")

    # 3 — Section contexte
    s = add(prs, "SECTION_HEADER")
    set_text(ph(s, 2), "01")
    set_text(ph(s, 0), "Contexte")

    # 4 — Le stage
    s = add(prs, "TITLE_AND_TWO_COLUMNS")
    set_text(ph(s, 0), "Le stage")
    set_text(ph(s, 3), "Organisme")
    set_text(ph(s, 2), "S. WATRANS\nTransport et BTP\nDépartement informatique")
    set_text(ph(s, 4), "Mission")
    set_text(ph(s, 1), "Une Application web\npour suivre les missions\net aider l’exploitation")

    # 5 — Problème
    s = add(prs, "CUSTOM_5")
    set_text(ph(s, 0), "Le problème")
    set_text(ph(s, 5), "Tableurs")
    set_text(ph(s, 1), "Plusieurs versions,\npas une seule vérité")
    set_text(ph(s, 7), "Oublis")
    set_text(ph(s, 2), "Permis et assurances\nqui expirent")
    set_text(ph(s, 6), "Affectation")
    set_text(ph(s, 3), "Qui est libre ?\nQuel véhicule ?")
    set_text(ph(s, 8), "Traçabilité")
    set_text(ph(s, 4), "Peu d’historique\ndes actions")

    # 6 — Périmètre
    s = add(prs, "CUSTOM_7")
    set_text(ph(s, 0), "Ce que fait l’Application")
    set_text(ph(s, 7), "Connexion")
    set_text(ph(s, 1), "Login JWT\net 3 rôles")
    set_text(ph(s, 8), "Pilotage")
    set_text(ph(s, 2), "Dashboard\net alertes")
    set_text(ph(s, 9), "Référentiels")
    set_text(ph(s, 5), "Clients, chauffeurs,\nvéhicules")
    set_text(ph(s, 13), "Missions")
    set_text(ph(s, 3), "Planifier,\nsuivre, clôturer")
    set_text(ph(s, 14), "Journal")
    set_text(ph(s, 4), "Audit automatique\nen lecture seule")
    set_text(ph(s, 15), "Assistant")
    set_text(ph(s, 6), "Questions métier\nancrées sur la base")

    # 7 — Rôles
    s = add(prs, "CUSTOM_6")
    set_text(ph(s, 0), "Trois rôles")
    set_text(ph(s, 4), "Admin")
    set_text(ph(s, 1), "Comptes et rôles\nTout le métier\nAssistant")
    set_text(ph(s, 5), "Responsable")
    set_text(ph(s, 2), "Flotte et missions\nJournal\nPas les comptes")
    set_text(ph(s, 6), "Chauffeur")
    set_text(ph(s, 3), "Ses courses\nDémarrer / terminer\nPas d’annulation")

    # 8 — Section conception
    s = add(prs, "SECTION_HEADER")
    set_text(ph(s, 2), "02")
    set_text(ph(s, 0), "Conception")

    # 9 — Stack
    s = add(prs, "TITLE_AND_TWO_COLUMNS")
    set_text(ph(s, 0), "Choix techniques")
    set_text(ph(s, 3), "Backend")
    set_text(ph(s, 2), "Spring Boot\nPostgreSQL + Flyway\nJWT")
    set_text(ph(s, 4), "Frontend")
    set_text(ph(s, 1), "React + TypeScript\nVite\nInterface selon le rôle")

    # 10 — Architecture
    s = add(prs, "CUSTOM_6")
    set_text(ph(s, 0), "Architecture")
    set_text(ph(s, 4), "Navigateur")
    set_text(ph(s, 1), "React\nÉcrans par profil")
    set_text(ph(s, 5), "API")
    set_text(ph(s, 2), "Spring Boot\n/api/v1")
    set_text(ph(s, 6), "Base")
    set_text(ph(s, 3), "PostgreSQL\nMissions et audit")

    # 11 — Produit + capture
    s = add(prs, "ONE_COLUMN_TEXT")
    set_text(ph(s, 0), "Le produit")
    set_text(
        ph(s, 1),
        "Une Application web.\nUne API REST.\nDes droits par rôle.",
    )
    hero = Path(__file__).resolve().parent / "assets" / "smarttrans-ai-hero.png"
    if hero.exists():
        place_picture_contain(s, ph(s, 2), hero)

    # 12 — Modèle de données (EA, fond transparent)
    s = add(prs, "TITLE_ONLY")
    set_text(ph(s, 0), "Modèle de données")
    modele = Path(__file__).resolve().parent / "assets" / "modele-donnees.png"
    if modele.exists():
        fit_picture(s, modele, top_in=1.12, max_h=4.25, max_w=9.2)

    # 13 — Section réalisation
    s = add(prs, "SECTION_HEADER")
    set_text(ph(s, 2), "03")
    set_text(ph(s, 0), "Réalisation")

    # 14 — Cycle mission
    s = add(prs, "CUSTOM_5")
    set_text(ph(s, 0), "Cycle d’une mission")
    set_text(ph(s, 5), "1. Créer")
    set_text(ph(s, 1), "Client, trajet, date\nStatut : planifiée")
    set_text(ph(s, 7), "2. Affecter")
    set_text(ph(s, 2), "Chauffeur\net véhicule")
    set_text(ph(s, 6), "3. Démarrer")
    set_text(ph(s, 3), "Le chauffeur passe\nen cours")
    set_text(ph(s, 8), "4. Terminer")
    set_text(ph(s, 4), "Course clôturée\n+ trace d’audit")

    # 15 — Sécurité
    s = add(prs, "CUSTOM_6")
    set_text(ph(s, 0), "Sécurité")
    set_text(ph(s, 4), "Login")
    set_text(ph(s, 1), "E-mail + mot de passe\nhaché")
    set_text(ph(s, 5), "JWT")
    set_text(ph(s, 2), "Jeton Bearer\nsur chaque appel")
    set_text(ph(s, 6), "Droits")
    set_text(ph(s, 3), "Chaque rôle\nvoit seulement son périmètre")

    # 16 — Assistant
    s = add(prs, "CUSTOM_6")
    set_text(ph(s, 0), "Assistant IA")
    set_text(ph(s, 4), "Ancré")
    set_text(ph(s, 1), "Réponses à partir\ndes données réelles")
    set_text(ph(s, 5), "Deux modes")
    set_text(ph(s, 2), "LLM si une clé\nsinon règles locales")
    set_text(ph(s, 6), "Limite")
    set_text(ph(s, 3), "Il n’invente pas\nde missions")

    # 17 — Interfaces
    s = add(prs, "TITLE_ONLY")
    set_text(ph(s, 0), "Quelques écrans")
    shots = [
        FIG / "ui-01-login.png",
        FIG / "ui-13-missions.png",
        FIG / "ui-16-assistant.png",
        FIG / "ui-17-activites.png",
    ]
    # 2x2 grid under title
    box_w, box_h = Inches(4.15), Inches(1.85)
    gap_x, gap_y = Inches(0.22), Inches(0.18)
    origin_x, origin_y = Inches(0.75), Inches(1.22)
    for i, path in enumerate(shots):
        if not path.exists():
            continue
        col, row = i % 2, i // 2
        left = origin_x + col * (box_w + gap_x)
        top = origin_y + row * (box_h + gap_y)
        s.shapes.add_picture(str(path), left, top, width=box_w)

    # 18 — Tests
    s = add(prs, "CUSTOM_8")
    set_text(ph(s, 2), "Maven")
    set_text(ph(s, 3), "Tests API et persistance")
    set_text(ph(s, 0), "Docker")
    set_text(ph(s, 1), "Démo locale ou en stack")
    set_text(ph(s, 4), "Recette")
    set_text(ph(s, 5), "Login, mission, alertes, IA")

    # 19 — Limites
    s = add(prs, "CUSTOM_5")
    set_text(ph(s, 0), "Limites et suite")
    set_text(ph(s, 5), "Pas de GED")
    set_text(ph(s, 1), "Pas de pièces jointes\ndans ce livrable")
    set_text(ph(s, 7), "Pas de push")
    set_text(ph(s, 2), "Pas de notifications\ntemps réel")
    set_text(ph(s, 6), "Plus de tests")
    set_text(ph(s, 3), "Renforcer la recette\nmétier")
    set_text(ph(s, 8), "Production")
    set_text(ph(s, 4), "HTTPS, secrets,\nmots de passe")

    # 20 — Merci
    s = add(prs, "CUSTOM_3_1")
    set_text(ph(s, 0), "Merci")
    set_text(ph(s, 1), "Questions ?\nHamza Bendaib  ·  SmartTrans-AI\nThème : Slidesgo")

    prs.save(OUT)
    apply_smarttrans_theme(OUT)
    print(f"Écrit : {OUT} ({len(list(prs.slides))} diapositives, thème SmartTrans)")


if __name__ == "__main__":
    build()
