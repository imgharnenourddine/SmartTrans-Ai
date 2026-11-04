#!/usr/bin/env python3
"""Deux notes personnelles (PDF) : JWT et assistant IA — sans logo école."""

from pathlib import Path

from reportlab.lib.colors import HexColor, white
from reportlab.lib.enums import TA_JUSTIFY, TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import cm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (
    ListFlowable,
    ListItem,
    PageBreak,
    Paragraph,
    Preformatted,
    SimpleDocTemplate,
    Spacer,
    Table,
    TableStyle,
)

OUT_DIR = Path(__file__).resolve().parent
FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
FONT_B = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
FONT_M = "/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf"

INK = HexColor("#1c2330")
MUTED = HexColor("#5a6570")
BLUE = HexColor("#1f4e79")
SOFT = HexColor("#eef3f8")
LINE = HexColor("#c5d0da")
ORANGE = HexColor("#c47a1a")

pdfmetrics.registerFont(TTFont("DejaVu", FONT))
pdfmetrics.registerFont(TTFont("DejaVu-Bold", FONT_B))
pdfmetrics.registerFont(TTFont("DejaVuMono", FONT_M))


def styles():
    s = getSampleStyleSheet()
    s.add(ParagraphStyle("CoverKicker", fontName="DejaVu", fontSize=10, textColor=ORANGE, spaceAfter=6))
    s.add(ParagraphStyle("CoverTitle", fontName="DejaVu-Bold", fontSize=22, textColor=BLUE, leading=28, spaceAfter=8))
    s.add(ParagraphStyle("CoverSub", fontName="DejaVu", fontSize=11, textColor=MUTED, leading=16, spaceAfter=14))
    s.add(ParagraphStyle("H1", fontName="DejaVu-Bold", fontSize=14, textColor=BLUE, spaceBefore=14, spaceAfter=8))
    s.add(ParagraphStyle("H2", fontName="DejaVu-Bold", fontSize=12, textColor=INK, spaceBefore=10, spaceAfter=6))
    s.add(ParagraphStyle("P", fontName="DejaVu", fontSize=10, textColor=INK, leading=15, alignment=TA_JUSTIFY, spaceAfter=7))
    s.add(ParagraphStyle("B", fontName="DejaVu", fontSize=10, textColor=INK, leading=14.5, leftIndent=8, spaceAfter=3))
    s.add(ParagraphStyle("Cap", fontName="DejaVu", fontSize=8.5, textColor=MUTED, alignment=TA_LEFT, spaceBefore=8))
    s.add(ParagraphStyle("CodeBlock", fontName="DejaVuMono", fontSize=8, textColor=INK, leading=11.5, backColor=SOFT, leftIndent=6, rightIndent=6, spaceBefore=4, spaceAfter=8))
    s.add(ParagraphStyle("Cell", fontName="DejaVu", fontSize=8.5, textColor=INK, leading=12))
    s.add(ParagraphStyle("CellH", fontName="DejaVu-Bold", fontSize=8.5, textColor=white, leading=12))
    s.add(ParagraphStyle("Footer", fontName="DejaVu", fontSize=8, textColor=MUTED))
    return s


def footer(canvas, doc):
    canvas.saveState()
    canvas.setStrokeColor(LINE)
    canvas.setLineWidth(0.6)
    canvas.line(2 * cm, 1.5 * cm, A4[0] - 2 * cm, 1.5 * cm)
    canvas.setFont("DejaVu", 8)
    canvas.setFillColor(MUTED)
    canvas.drawString(2 * cm, 1.05 * cm, "Note personnelle — SmartTrans-AI  ·  sans usage officiel")
    canvas.drawRightString(A4[0] - 2 * cm, 1.05 * cm, str(doc.page))
    canvas.restoreState()


def bullets(items, st):
    flow = []
    for item in items:
        flow.append(ListItem(Paragraph(item, st["B"]), leftIndent=12, bulletColor=BLUE, value="•"))
    return ListFlowable(flow, bulletType="bullet", start="•", leftIndent=14, spaceAfter=8)


def table(headers, rows, st, col_widths):
    head = [Paragraph(h, st["CellH"]) for h in headers]
    body = [[Paragraph(c, st["Cell"]) for c in row] for row in rows]
    t = Table([head] + body, colWidths=col_widths, repeatRows=1)
    t.setStyle(
        TableStyle(
            [
                ("BACKGROUND", (0, 0), (-1, 0), BLUE),
                ("BACKGROUND", (0, 1), (-1, -1), SOFT),
                ("TEXTCOLOR", (0, 0), (-1, 0), white),
                ("VALIGN", (0, 0), (-1, -1), "TOP"),
                ("LEFTPADDING", (0, 0), (-1, -1), 7),
                ("RIGHTPADDING", (0, 0), (-1, -1), 7),
                ("TOPPADDING", (0, 0), (-1, -1), 5),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
                ("GRID", (0, 0), (-1, -1), 0.3, LINE),
                ("ROWBACKGROUNDS", (0, 1), (-1, -1), [white, SOFT]),
            ]
        )
    )
    return t


def code_block(text, st):
    return Preformatted(text.strip("\n"), st["CodeBlock"])


def make_doc(path: Path, title: str):
    path.parent.mkdir(parents=True, exist_ok=True)
    doc = SimpleDocTemplate(
        str(path),
        pagesize=A4,
        leftMargin=2 * cm,
        rightMargin=2 * cm,
        topMargin=1.8 * cm,
        bottomMargin=2.1 * cm,
        title=title,
        author="Note personnelle SmartTrans-AI",
    )
    return doc


def jwt_story(st):
    w = A4[0] - 4 * cm
    story = [
        Paragraph("NOTE PERSONNELLE  ·  TECHNIQUE", st["CoverKicker"]),
        Paragraph("Comment fonctionne JWT dans SmartTrans-AI", st["CoverTitle"]),
        Paragraph(
            "Explication fonctionnelle et technique, calée sur le code réel "
            "(AuthService, JwtService, JwtAuthenticationFilter, React).",
            st["CoverSub"],
        ),
        Paragraph("1. Vue fonctionnelle (ce que voit l’utilisateur)", st["H1"]),
        Paragraph(
            "JWT (JSON Web Token) sert à <b>rester connecté sans session serveur</b>. "
            "Après un login réussi, l’API donne un jeton. Le navigateur le range et le "
            "renvoie à chaque appel. L’API n’a pas besoin de mémoriser « qui est en ligne » "
            "dans une session : elle relit le jeton.",
            st["P"],
        ),
        Paragraph("Parcours côté métier", st["H2"]),
        bullets(
            [
                "L’utilisateur saisit e-mail et mot de passe sur <b>/login</b>.",
                "Le serveur vérifie le compte (hachage BCrypt, compte activé).",
                "Il émet un jeton <b>Bearer</b> + le profil (id, nom, rôles).",
                "React stocke le jeton dans <b>localStorage</b> (<font face='DejaVuMono'>smarttrans.accessToken</font>).",
                "Chaque requête API (sauf le login) part avec <font face='DejaVuMono'>Authorization: Bearer …</font>.",
                "Si le jeton est absent, expiré ou falsifié : 401. Si le rôle ne suffit pas : 403.",
            ],
            st,
        ),
        Paragraph(
            "Les rôles voyagent dans le jeton (<b>ADMIN</b>, <b>TRANSPORT_MANAGER</b>, <b>DRIVER</b>). "
            "L’interface s’en sert pour les menus ; l’API s’en sert pour vraiment bloquer les actions.",
            st["P"],
        ),
        Paragraph("2. Qu’est-ce qu’un JWT ?", st["H1"]),
        Paragraph(
            "Un JWT est une chaîne en trois parties séparées par des points : "
            "<b>header.payload.signature</b>. Header et payload sont en Base64URL (lisibles, pas chiffrés). "
            "La signature HMAC-SHA empêche de modifier le contenu sans connaître le secret.",
            st["P"],
        ),
        table(
            ["Partie", "Rôle"],
            [
                ["Header", "Algorithme (ici HMAC-SHA à partir du secret) et type JWT."],
                ["Payload (claims)", "Données : sujet, e-mail, rôles, dates iat / exp."],
                ["Signature", "Empreinte du header+payload avec le secret serveur. Vérifie l’intégrité."],
            ],
            st,
            [4.2 * cm, w - 4.2 * cm],
        ),
        Spacer(1, 8),
        Paragraph("Claims utilisés dans SmartTrans-AI", st["H2"]),
        table(
            ["Claim", "Contenu"],
            [
                ["sub", "UUID de l’utilisateur (principal.getId())."],
                ["email", "Adresse de connexion (username Spring)."],
                ["roles", "Liste sans préfixe ROLE_ : ADMIN, TRANSPORT_MANAGER, DRIVER."],
                ["iat / exp", "Émis maintenant ; expire après app.jwt.expiration-ms (défaut 86 400 000 ms = 24 h)."],
            ],
            st,
            [3.2 * cm, w - 3.2 * cm],
        ),
        Paragraph("3. Chaîne technique dans le projet", st["H1"]),
        Paragraph("A. Login — émission", st["H2"]),
        Paragraph(
            "<font face='DejaVuMono'>POST /api/v1/auth/login</font> est public (pas de Bearer). "
            "<b>AuthServiceImpl</b> appelle l’AuthenticationManager (e-mail + mot de passe). "
            "Si OK, <b>JwtService.generateToken</b> construit le JWT avec JJWT "
            "(<font face='DejaVuMono'>signWith(secretKey)</font>, clé HMAC dérivée de <font face='DejaVuMono'>app.jwt.secret</font>). "
            "La réponse JSON contient <font face='DejaVuMono'>accessToken</font>, "
            "<font face='DejaVuMono'>tokenType: Bearer</font>, <font face='DejaVuMono'>expiresInMs</font> et le profil. "
            "Un événement d’audit LOGIN est enregistré.",
            st["P"],
        ),
        Paragraph("B. Appels suivants — filtre", st["H2"]),
        Paragraph(
            "<b>JwtAuthenticationFilter</b> (OncePerRequestFilter) s’exécute avant le filtre username/password. "
            "S’il n’y a pas d’en-tête Bearer, ou si l’URL commence par <font face='DejaVuMono'>/api/v1/auth/</font>, "
            "il laisse passer sans poser d’authentification. Sinon il coupe le préfixe Bearer, "
            "appelle <font face='DejaVuMono'>jwtService.isValid</font> (parse + vérif signature + expiration). "
            "Si valide : extraire l’e-mail, recharger l’utilisateur (<b>CustomUserDetailsService</b>), "
            "poser un <font face='DejaVuMono'>UsernamePasswordAuthenticationToken</font> dans le SecurityContext. "
            "Les autorités Spring sont alors disponibles pour <font face='DejaVuMono'>@PreAuthorize</font>.",
            st["P"],
        ),
        Paragraph("C. Politique de sécurité", st["H2"]),
        bullets(
            [
                "Session <b>STATELESS</b> : pas de cookie de session JSESSIONID pour l’API.",
                "CSRF désactivé (API jeton, pas de formulaire cookie classique).",
                "Mot de passe : <b>BCryptPasswordEncoder</b>.",
                "Routes ouvertes : <font face='DejaVuMono'>/api/v1/auth/**</font>, health, swagger, H2.",
                "Tout le reste : <b>authenticated()</b>, puis rôles au niveau des contrôleurs.",
                "401 : pas / plus authentifié (JwtAuthenticationEntryPoint). 403 : authentifié mais rôle insuffisant.",
            ],
            st,
        ),
        Paragraph("D. Front React", st["H2"]),
        Paragraph(
            "Le client HTTP Axios (<font face='DejaVuMono'>api/client.ts</font>) attache le Bearer si un jeton existe. "
            "Le login ne doit <b>pas</b> envoyer un ancien jeton (d’où l’exclusion des routes <font face='DejaVuMono'>/auth/</font> dans le filtre). "
            "Déconnexion = suppression du localStorage.",
            st["P"],
        ),
        Paragraph("4. Schéma du flux", st["H1"]),
        code_block(
            """
Navigateur                         API Spring Boot                    PostgreSQL
    |                                    |                                 |
    |  POST /auth/login                  |                                 |
    |  { email, password }               |  vérif BCrypt + user enabled    |
    | ---------------------------------> | ------------------------------> |
    |  200 { accessToken, roles, … }     |  JWT signé (secret serveur)     |
    | <--------------------------------- |                                 |
    |  stocke le jeton (localStorage)    |                                 |
    |                                    |                                 |
    |  GET /missions                     |                                 |
    |  Authorization: Bearer <jwt>       |  filtre : signature + exp       |
    | ---------------------------------> |  charge UserDetails + rôles     |
    |                                    |  @PreAuthorize selon le rôle    |
    |  200 liste / 401 / 403             | ------------------------------> |
    | <--------------------------------- |                                 |
""",
            st,
        ),
        Paragraph("5. Points à retenir (soutenance / debug)", st["H1"]),
        bullets(
            [
                "Le payload est <b>lisible</b> : ne jamais y mettre le mot de passe.",
                "La confiance vient de la <b>signature + secret</b>, pas du fait que le JSON soit « caché ».",
                "Le secret doit être assez long (HMAC-SHA, ≥ 256 bits) et hors Git en prod (<font face='DejaVuMono'>JWT_SECRET</font>).",
                "Durée de vie 24 h par défaut : après expiration, il faut se reconnecter (pas de refresh token dans ce livrable).",
                "Les rôles dans le jeton sont une photo à l’instant du login : un changement de rôle en base n’est pris en compte qu’au prochain login (le filtre recharge toutefois l’utilisateur par e-mail pour les autorités actuelles).",
            ],
            st,
        ),
        Paragraph(
            "Classes clés : <font face='DejaVuMono'>JwtService</font>, <font face='DejaVuMono'>JwtAuthenticationFilter</font>, "
            "<font face='DejaVuMono'>AuthServiceImpl</font>, <font face='DejaVuMono'>SecurityConfig</font>, "
            "<font face='DejaVuMono'>tokenStorage.ts</font>.",
            st["P"],
        ),
    ]
    return story


def assistant_story(st):
    w = A4[0] - 4 * cm
    story = [
        Paragraph("NOTE PERSONNELLE  ·  TECHNIQUE", st["CoverKicker"]),
        Paragraph("Assistant IA de SmartTrans-AI", st["CoverTitle"]),
        Paragraph(
            "Fonctionnement métier et technique : ancrage sur PostgreSQL, mode GROUNDED, mode LLM optionnel.",
            st["CoverSub"],
        ),
        Paragraph("1. Vue fonctionnelle", st["H1"]),
        Paragraph(
            "L’assistant est un <b>chat d’exploitation</b>, pas un planificateur automatique de tournées. "
            "L’admin ou le responsable pose une question en français. L’outil répond à partir des "
            "<b>missions, chauffeurs, véhicules et clients</b> déjà en base. Il peut aussi signaler "
            "des échéances (permis, assurance, visite technique) et suggérer une ressource disponible.",
            st["P"],
        ),
        Paragraph("Qui a le droit ?", st["H2"]),
        Paragraph(
            "Uniquement <b>ADMIN</b> et <b>TRANSPORT_MANAGER</b> "
            "(<font face='DejaVuMono'>@PreAuthorize</font> sur <font face='DejaVuMono'>AssistantController</font>). "
            "Le chauffeur n’a pas le menu ni l’API : un appel donne 403.",
            st["P"],
        ),
        Paragraph("Ce qu’on peut demander", st["H2"]),
        table(
            ["Intention", "Exemples", "Comportement attendu"],
            [
                [
                    "Effectif",
                    "« Combien de missions ? »",
                    "Réponse courte : un nombre + le mot (ex. « 3 missions »), sans roman.",
                ],
                [
                    "Recherche",
                    "Nom, immat, référence",
                    "Extraits des fiches trouvées + citations cliquables vers l’écran métier.",
                ],
                [
                    "Situation",
                    "résumé, dashboard, risque",
                    "Vue d’ensemble : totaux, permis / documents à 30 jours.",
                ],
                [
                    "Suggestion",
                    "qui est dispo, affecter…",
                    "Chauffeur AVAILABLE (permis OK) + véhicule AVAILABLE (assurance / CT OK).",
                ],
            ],
            st,
            [3.2 * cm, 4.2 * cm, w - 7.4 * cm],
        ),
        Spacer(1, 8),
        Paragraph(
            "Règle métier importante : <b>ne pas inventer</b> un chauffeur ou une immatriculation absents du contexte. "
            "Si rien n’est trouvé, l’assistant le dit clairement.",
            st["P"],
        ),
        Paragraph("2. Architecture technique", st["H1"]),
        Paragraph(
            "Front : <font face='DejaVuMono'>AssistantPage</font> → <font face='DejaVuMono'>POST /api/v1/assistant/ask</font> "
            "<font face='DejaVuMono'>{ message }</font> (JWT obligatoire). "
            "Back : <b>AssistantServiceImpl.ask</b> en lecture seule (transaction readOnly).",
            st["P"],
        ),
        code_block(
            """
Utilisateur (admin / responsable)
        │  question en français
        ▼
React  POST /assistant/ask  + Bearer JWT
        │
        ▼
AssistantServiceImpl
        │  1. sanitize (enlève % et _)
        │  2. collect()  → AssistantGrounding  (requêtes SQL)
        │  3. si clé OpenAI  → OpenAiChatClient.complete(system, contexte)
        │  4. sinon / échec  → GroundedAnswerComposer.extractive()
        ▼
Réponse { answer, mode: "LLM" | "GROUNDED", citations[] }
""",
            st,
        ),
        Paragraph("3. L’ancrage (grounding) — le cœur du dispositif", st["H1"]),
        Paragraph(
            "Sans ancrage, un modèle de langage hallucine des effectifs. Ici, <b>collect()</b> construit un objet "
            "<font face='DejaVuMono'>AssistantGrounding</font> à partir de PostgreSQL avant toute génération.",
            st["P"],
        ),
        Paragraph("Découpage de la question", st["H2"]),
        bullets(
            [
                "<b>needles</b> : tokens ≥ 3 lettres, hors mots vides (le, la, quel…). Si la phrase est courte, la phrase entière aussi.",
                "Pour chaque needle : search missions, chauffeurs, véhicules (immat), clients (nom), max 8 par type.",
                "<b>overview</b> si la question parle de résumé, dashboard, alerte, risque, état…",
                "<b>suggest</b> si elle parle de disponible, recommander, affecter, quel chauffeur / véhicule…",
                "Si overview ou suggest : charger aussi permis / documents expirant sous 30 jours, ressources AVAILABLE, missions planifiées incomplètes.",
            ],
            st,
        ),
        Paragraph("4. Deux modes de réponse", st["H1"]),
        table(
            ["Mode", "Quand", "Mécanisme"],
            [
                [
                    "GROUNDED",
                    "Pas de clé API, ou appel LLM en échec / vide",
                    "Règles Java dans GroundedAnswerComposer : décompte, suggestion, synthèse, ou « je n’ai pas trouvé ».",
                ],
                [
                    "LLM",
                    "app.assistant.openai-api-key renseignée et HTTP OK",
                    "Contexte textuel (contextBlock) + prompt système envoyés à /chat/completions. Température 0.2. Modèle par défaut gpt-4o-mini.",
                ],
            ],
            st,
            [2.8 * cm, 5.2 * cm, w - 8 * cm],
        ),
        Spacer(1, 8),
        Paragraph("Prompt système (contrainte)", st["H2"]),
        Paragraph(
            "Le modèle doit répondre <b>uniquement</b> à partir du contexte : décompte = nombre + mot ; "
            "recommandation = chauffeur AVAILABLE permis valide + véhicule AVAILABLE documents valides, une phrase de justification ; "
            "français, concis ; rien inventer.",
            st["P"],
        ),
        Paragraph(
            "Même en mode LLM, les <b>citations</b> (MISSION, DRIVER, VEHICLE, CLIENT) sont calculées côté serveur "
            "à partir du grounding, pas par le modèle. Le front s’en sert pour ouvrir la fiche.",
            st["P"],
        ),
        Paragraph("5. Composer extractif (sans LLM)", st["H1"]),
        bullets(
            [
                "Si la question est un décompte → <font face='DejaVuMono'>countOnlyAnswer</font> (missions totales / planifiées / en cours).",
                "Si suggest → bloc chauffeur + véhicule disponibles, sinon message d’indisponibilité.",
                "Si overview ou pas de hits mais des totaux / alertes → phrase d’état opérationnel + échéances 30 jours.",
                "Sinon liste courte des objets trouvés ; si vraiment rien : phrase d’échec honnête.",
            ],
            st,
        ),
        Paragraph("6. Configuration et limites", st["H1"]),
        table(
            ["Élément", "Détail"],
            [
                ["Clé / modèle / URL", "AssistantProperties : openaiApiKey, openaiModel, openaiBaseUrl (défaut api.openai.com/v1)."],
                ["Activation LLM", "enabled() = clé non vide. Sinon GROUNDED, zéro appel réseau."],
                ["Robustesse", "Exception HTTP → log warn, Optional.empty() → repli GROUNDED."],
                ["Périmètre", "Pas d’optimisation de tournées, pas d’écriture en base via le chat."],
                ["Qualité", "Liée au contexte injecté (plafond 8 fiches, 5 dispos). Une question trop vague = peu de needles."],
            ],
            st,
            [3.6 * cm, w - 3.6 * cm],
        ),
        Spacer(1, 8),
        Paragraph("Fichiers à relire", st["H2"]),
        Paragraph(
            "<font face='DejaVuMono'>AssistantServiceImpl</font>, <font face='DejaVuMono'>GroundedAnswerComposer</font>, "
            "<font face='DejaVuMono'>OpenAiChatClient</font>, <font face='DejaVuMono'>AssistantController</font>, "
            "<font face='DejaVuMono'>AssistantPage.tsx</font>.",
            st["P"],
        ),
        Paragraph(
            "En une phrase pour un jury : <i>l’IA n’est pas branchée à vide ; elle ne parle que de ce que PostgreSQL vient de lui donner, "
            "et si le prestataire LLM est absent, des règles déterministes répondent quand même.</i>",
            st["P"],
        ),
    ]
    return story


def main():
    st = styles()
    p1 = OUT_DIR / "01-jwt-smarttrans.pdf"
    p2 = OUT_DIR / "02-assistant-ia-smarttrans.pdf"
    d1 = make_doc(p1, "JWT — SmartTrans-AI")
    d1.build(jwt_story(st), onFirstPage=footer, onLaterPages=footer)
    d2 = make_doc(p2, "Assistant IA — SmartTrans-AI")
    d2.build(assistant_story(st), onFirstPage=footer, onLaterPages=footer)
    print(f"Écrit : {p1}")
    print(f"Écrit : {p2}")


if __name__ == "__main__":
    main()
