#!/usr/bin/env python3
"""Genereert Android strings.xml (12 talen) uit de iOS String Catalog van Dicom Viewer.
Gebruik: android_strings.py <ios Localizable.xcstrings> <android res-map>
"""
import json, os, re, sys, html

CAT, RES = sys.argv[1], sys.argv[2]
cat = json.load(open(CAT))["strings"]

# android-naam -> Nederlandse sleutel in de iOS-catalogus
M = {
 "tab_scans": "Scans", "tab_learn": "Leren", "tab_about": "Over",
 "n_sample_scans": "%lld voorbeeldscans", "one_own_scan": "1 eigen scan", "n_own_scans": "%lld eigen scans",
 "tip_jar": "Fooienpot", "import_scan": "Scan importeren", "open_own_scan": "Eigen scan openen",
 "choose_dicom": "Kies een DICOM-bestand", "recently_opened": "Laatst geopend", "my_scans": "Mijn scans",
 "sample_scans": "Voorbeeldscans", "sample_scans_sub": "Hartkatheterisatie: de kransslagaders in beeld",
 "show": "Toon", "hide": "Verberg",
 "demo_before": "Vóór de ingreep", "demo_during": "Tijdens de ingreep", "demo_after": "Na de ingreep",
 "learn_tip": "Wat doet Window/Level met een scan? **Lees het in Leren**",
 "loading_scan": "Scan laden…", "choose_scan": "Kies een scan", "choose_scan_sub": "Tik links op een scan om hem hier te bekijken.",
 "delete_file_q": "Bestand verwijderen?", "delete_file_msg": "%@ wordt permanent verwijderd.",
 "delete_series_q": "Serie verwijderen?", "delete_series_msg": "%@ — %lld bestanden worden permanent verwijderd.",
 "delete": "Verwijder", "cancel": "Annuleer", "delete_series_n": "Verwijder serie (%lld bestanden)", "share": "Deel",
 "file_unsupported": "Bestand niet ondersteund", "ok": "OK",
 "err_no_images_series": "Geen leesbare beelden in deze serie.", "err_format": "Dit bestandsformaat wordt niet ondersteund.",
 "err_not_dicom": "Dit is geen geldig DICOM-bestand.", "err_no_pixels": "Geen beelddata gevonden in dit bestand.",
 "err_dims": "De afmetingen van het beeld kloppen niet.",
 "series_meta": "Serie · %lld slices · %@", "cine_frames": "Cine · %lld beelden", "n_frames": "%lld beelden",
 "close": "Sluiten", "scan_details": "Scangegevens en exporteren", "inverted": "Omgekeerd",
 "measure_start": "Tik op het beginpunt", "measure_end": "Tik op het eindpunt",
 "measure_no_spacing": "Geen pixelafstand in dit bestand, dus afstand in pixels",
 "measure_detector": "Gemeten op de detector; in het lichaam is het iets kleiner",
 "measure_file": "Afstand volgens de pixelafstand in het bestand",
 "clear": "Wis", "invert_contrast": "Contrast omkeren", "measure_distance": "Afstand meten", "stop_measuring": "Meten uitzetten",
 "play": "Afspelen", "pause": "Pauzeren", "frame_x_of_y": "Beeld %lld van %lld", "fps_n": "%lld bps",
 "fps_menu": "%lld beelden per seconde", "speed": "Snelheid",
 "pinch_hint": "Knijp om te zoomen · dubbeltik om terug te zetten", "no_image_data": "Geen beelddata in dit bestand",
 "preset_auto": "Auto", "preset_abdomen": "Abdomen", "preset_lung": "Long", "preset_bone": "Bot", "preset_brain": "Hersenen",
 "patient": "Patiënt", "modality": "Modaliteit", "size": "Afmeting", "frames": "Beelden", "wl": "Window / Level",
 "pixel_spacing": "Pixelafstand", "pixel_spacing_detector": "Pixelafstand (detector)", "export": "Exporteren",
 "export_png": "Dit beeld exporteren als PNG", "export_jpeg": "Dit beeld exporteren als JPEG",
 "export_footer": "Het beeld wordt geëxporteerd met het gekozen venster (zonder omkeren).", "done": "Klaar",
 "scan_fallback": "Scan", "window_n": "Window %lld", "level_n": "Level %lld", "medical_image": "Medisch beeld",
 "learn_sub": "Korte uitleg over medische beelden", "featured": "UITGELICHT", "wl_title": "Window/Level in één minuut",
 "wl_short": "**Window** bepaalt het contrast: hoe breed het grijsbereik is. **Level** bepaalt de helderheid: waar het midden ligt.",
 "wl_long": "Een medisch beeld bevat veel meer grijstinten dan een scherm kan tonen. Window/Level kiest welk stukje je ziet. Een smal venster geeft veel contrast, een breed venster meer overzicht. Bij CT gebruikt de app vaste presets, zoals Long (L −600, W 1500) en Hersenen (L 40, W 80). Die kies je in de viewer onderin.",
 "read_more": "Lees verder", "less": "Minder", "dark": "donker", "window_word": "venster", "light": "licht",
 "scan_types": "Soorten scans",
 "mod_ct": "CT-scan", "mod_ct_short": "Doorsnedes met röntgen",
 "mod_ct_long": "Een CT-scanner maakt met röntgenstraling dunne doorsnedes van het lichaam. De grijswaarden zijn Hounsfield-eenheden (HU): lucht is −1000, water 0 en bot enkele honderden tot boven de 1000. Daarom werken de Window/Level-presets in deze app met HU.",
 "mod_mr": "MRI", "mod_mr_short": "Magneetveld, veel detail",
 "mod_mr_long": "MRI gebruikt een sterk magneetveld en radiogolven, zonder röntgenstraling. Het toont weke delen zoals hersenen, spieren en gewrichten in veel detail. De grijswaarden hebben geen vaste schaal zoals bij CT.",
 "mod_xa": "Angiografie", "mod_xa_short": "Bloedvaten, als film",
 "mod_xa_long": "Bij angiografie worden bloedvaten zichtbaar gemaakt met contrastvloeistof, terwijl er röntgenbeelden worden gemaakt. Vaak als korte film (cine). De voorbeeldscans in deze app zijn opnames van de kransslagaders.",
 "mod_us": "Echo", "mod_us_short": "Geluidsgolven, geen straling",
 "mod_us_long": "Echografie maakt beelden met geluidsgolven, zonder straling. Het beeld is live; in een DICOM-bestand staan losse beelden of korte filmpjes.",
 "mod_cr": "Röntgenfoto", "mod_cr_short": "Eén projectiebeeld",
 "mod_cr_long": "Een gewone röntgenfoto (CR of DX) is één projectiebeeld, bijvoorbeeld van botten of longen. Alles wat de straling tegenhoudt, zoals bot, wordt licht.",
 "mod_mg": "Mammografie", "mod_mg_short": "Röntgen van de borst",
 "mod_mg_long": "Een mammografie is een röntgenfoto van de borst met een lage dosis straling. De beelden zijn groot en zeer gedetailleerd.",
 "viewer_tips": "Handig in de viewer", "tip_pinch": "Knijp om in en uit te zoomen", "tip_drag": "Sleep om het beeld te verplaatsen",
 "tip_double": "Dubbeltik om terug te zetten", "tip_tap": "Tik één keer om de knoppen te verbergen",
 "not_diagnosis": "Bedoeld om te leren en te bekijken, niet om een diagnose te stellen.",
 "fact_no_account": "Geen account nodig", "fact_no_account_sub": "Openen en meteen kijken",
 "fact_local": "Alles blijft op je toestel", "fact_local_sub": "Geen upload, geen tracking",
 "fact_offline": "Werkt offline", "fact_offline_sub": "Ook zonder internet",
 "support_maker": "Steun de maker", "tip_free": "De app blijft gratis en zonder reclame. Een fooi helpt.",
 "thanks": "Dank je wel! Dit betekent veel.", "restore": "Aankopen herstellen",
 "help_contact": "Hulp en contact", "privacy_policy": "Privacybeleid", "rate_app": "Beoordeel de app",
 "disclaimer": "Dicom Viewer is bedoeld om te leren en te bekijken. Het is geen medisch hulpmiddel en niet geschikt om een diagnose te stellen.",
 "by_version": "by The IT Crowd · versie %@",
 "coffee": "Koffie", "lunch": "Lunch", "dinner": "Diner", "rate_dicom": "Beoordeel Dicom Viewer",
 "dont_show_again": "Niet meer vanzelf tonen", "thank_trust": "Bedankt voor je vertrouwen",
 "tipjar_intro": "Deze app is gratis en zonder advertenties.\nEen kleine bijdrage helpt enorm.",
 "tip_small_desc": "Een klein bedankje voor de ontwikkelaar", "tip_medium_desc": "Houd de ontwikkelaar goed gevoed",
 "tip_large_desc": "Een uitgebreid diner voor de ontwikkelaar",
 "coach_close": "of tik ergens om te sluiten", "coach_small": "Kleine bijdrage, grote glimlach",
 "coach_large": "Echt waardevol — hartstikke bedankt!",
}

LANGS = {  # iOS-taal -> Android values-map
 "en": "values", "nl": "values-nl", "de": "values-de", "fr": "values-fr", "es": "values-es", "it": "values-it",
 "pt": "values-pt", "ja": "values-ja", "ko": "values-ko", "zh-Hans": "values-b+zh+Hans", "ru": "values-ru", "hi": "values-hi",
}

# Android-specifieke teksten (niet in de iOS-catalogus)
EXTRA = {
 "open_own_scan_sub": {
  "en": "From your files, email or another app. Stays on your device only.",
  "nl": "Uit je bestanden, mail of een andere app. Blijft alleen op je toestel.",
  "de": "Aus deinen Dateien, E-Mails oder einer anderen App. Bleibt nur auf deinem Gerät.",
  "fr": "Depuis vos fichiers, un e-mail ou une autre app. Reste uniquement sur votre appareil.",
  "es": "Desde tus archivos, el correo u otra app. Solo se queda en tu dispositivo.",
  "it": "Dai tuoi file, dalla posta o da un’altra app. Resta solo sul tuo dispositivo.",
  "pt": "Dos seus arquivos, e-mail ou outro app. Fica só no seu dispositivo.",
  "ja": "ファイル、メール、他のアプリから。端末内にのみ保存されます。",
  "ko": "파일, 메일 또는 다른 앱에서. 기기에만 저장됩니다.",
  "zh-Hans": "来自你的文件、邮件或其他应用。只保存在你的设备上。",
  "ru": "Из ваших файлов, почты или другого приложения. Хранится только на устройстве.",
  "hi": "आपकी फ़ाइलों, ईमेल या किसी दूसरे ऐप से। सिर्फ़ आपके डिवाइस पर रहता है।"},
 "also_ios": {
  "en": "Also on iPhone and iPad", "nl": "Ook op iPhone en iPad", "de": "Auch für iPhone und iPad",
  "fr": "Aussi sur iPhone et iPad", "es": "También en iPhone y iPad", "it": "Anche su iPhone e iPad",
  "pt": "Também no iPhone e iPad", "ja": "iPhone・iPad 版もあります", "ko": "iPhone과 iPad에서도 사용 가능",
  "zh-Hans": "也支持 iPhone 和 iPad", "ru": "Также для iPhone и iPad", "hi": "iPhone और iPad पर भी"},
}


def conv(v):
    """iOS-formaat -> Android-formaat en XML-escapes."""
    v = v.replace("**", "")
    # tel argumenten om posities te bepalen
    n = len(re.findall(r"%(?:\d\$)?(?:lld|@)", v))
    if n > 1:
        i = [0]
        def pos(m):
            if m.group(1):
                idx = m.group(1)
            else:
                i[0] += 1; idx = f"{i[0]}$"
            return "%" + idx + ("d" if m.group(2) == "lld" else "s")
        v = re.sub(r"%(\d\$)?(lld|@)", pos, v)
    else:
        v = re.sub(r"%(\d\$)?lld", lambda m: "%" + (m.group(1) or "") + "d", v)
        v = re.sub(r"%(\d\$)?@", lambda m: "%" + (m.group(1) or "") + "s", v)
    v = html.escape(v, quote=False)
    v = v.replace("'", "\\'").replace('"', '\\"').replace("’", "’")
    v = v.replace("\n", "\\n")
    if v.startswith("@") or v.startswith("?"):
        v = "\\" + v
    return v


missing = []
for lang, folder in LANGS.items():
    lines = ['<?xml version="1.0" encoding="utf-8"?>', "<resources>"]
    if lang == "en":
        lines.append('    <string name="app_name" translatable="false">Dicom Viewer</string>')
    for name, key in M.items():
        e = cat.get(key)
        if e is None:
            missing.append(key); continue
        if lang == "nl":
            val = key
        else:
            val = e["localizations"][lang]["stringUnit"]["value"]
        fmt = ' formatted="false"' if "%" in val and "%" not in re.sub(r"%(\d\$)?(lld|@|d|s)", "", val) and False else ""
        lines.append(f'    <string name="{name}"{fmt}>{conv(val)}</string>')
    for name, tr in EXTRA.items():
        lines.append(f'    <string name="{name}">{conv(tr[lang])}</string>')
    lines.append("</resources>")
    os.makedirs(os.path.join(RES, folder), exist_ok=True)
    open(os.path.join(RES, folder, "strings.xml"), "w").write("\n".join(lines) + "\n")
print("missing:", sorted(set(missing)))
print("strings per taal:", len(M) + len(EXTRA))
