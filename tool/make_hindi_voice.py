#!/usr/bin/env python3
"""Records the Hindi warning clips in assets/voice/ from the Hindi strings in lib/l10n/app_hi.arb.

The clips say exactly what the warning screen says: the headline (with Mummy/Daddy/Grandma, or
none for the neutral title) and the body. Uses eSpeak NG (offline, free to redistribute the
audio) and ffmpeg. Re-run after changing the Hindi warning strings:

    sudo apt-get install espeak-ng ffmpeg
    python3 tool/make_hindi_voice.py
"""
import json
import pathlib
import subprocess
import tempfile

ROOT = pathlib.Path(__file__).resolve().parent.parent
HI = json.loads((ROOT / "lib/l10n/app_hi.arb").read_text(encoding="utf-8"))
OUT = ROOT / "assets/voice"

TITLES = {"mummy": HI["titleMummy"], "daddy": HI["titleDaddy"], "grandma": HI["titleGrandma"], "neutral": None}
FAMILY = HI["yourFamily"]  # the clip can't know the family member's name


def sentences(level: str, title: str | None) -> str:
    if level == "scam":
        head = HI["alertScamHeadlineNamed"].replace("{arg1}", title) if title else HI["alertScamHeadline"]
        body = HI["alertScamBody"]
    elif level == "careful":
        head = HI["alertCarefulHeadlineNamed"].replace("{arg1}", title) if title else HI["alertCarefulHeadline"]
        body = HI["alertCarefulBody"].replace("{arg1}", FAMILY)
    else:
        head, body = HI["alertOtpHeadline"], HI["alertOtpBody"]
    return f"{head} {body}"


def record(text: str, dest: pathlib.Path) -> None:
    with tempfile.TemporaryDirectory() as tmp:
        wav = pathlib.Path(tmp) / "clip.wav"
        # Hindi voice, a little slower than normal for older listeners
        subprocess.run(["espeak-ng", "-v", "hi", "-s", "135", "-p", "45", "-a", "180", "-g", "4", "-w", str(wav), text], check=True)
        # Mono MP3 like the English clips, loudness-normalised so every clip is equally loud
        subprocess.run(
            ["ffmpeg", "-loglevel", "error", "-y", "-i", str(wav), "-af", "acompressor=threshold=-24dB:ratio=4:attack=5:release=80,loudnorm=I=-10:TP=-1.0:LRA=7", "-ac", "1", "-ar", "22050", "-b:a", "64k", str(dest)],
            check=True,
        )


def main() -> None:
    for level in ("scam", "careful", "otp"):
        for key, title in TITLES.items():
            dest = OUT / f"{level}_hi_{key}.mp3"
            text = sentences(level, title)
            record(text, dest)
            print(f"{dest.name}: {text}")


if __name__ == "__main__":
    main()
