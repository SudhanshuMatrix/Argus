#!/usr/bin/env python3
"""Makes the 12 Hindi warning clips in assets/voice/ (scam, careful, otp x mummy, daddy, grandma, neutral).

Two ways:

    # From a recording (what Argus ships: the ElevenLabs voice in tool/voice/). The one
    # recording is used for every Hindi warning, loudness-matched to the English clips.
    python3 tool/make_hindi_voice.py --from tool/voice/hindi_warning_elevenlabs.mp3

    # Synthesised with eSpeak NG from the Hindi strings in lib/l10n/app_hi.arb, so each clip
    # says exactly what its warning screen says (sounds robotic):
    sudo apt-get install espeak-ng ffmpeg
    python3 tool/make_hindi_voice.py
"""
import argparse
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


# Same format and loudness as the English ElevenLabs clips (44.1 kHz mono MP3, 128 kbps)
ENCODE = ["-ac", "1", "-ar", "44100", "-b:a", "128k"]


def from_recording(source: pathlib.Path, dest: pathlib.Path) -> None:
    subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-i", str(source), "-af", "acompressor=threshold=-28dB:ratio=4:attack=5:release=120:makeup=2,loudnorm=I=-9:TP=-0.5:LRA=6,volume=2dB,alimiter=limit=0.95:level=false", *ENCODE, str(dest)], check=True)


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
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--from", dest="source", type=pathlib.Path, help="a Hindi recording to use for every warning")
    args = parser.parse_args()
    for level in ("scam", "careful", "otp"):
        for key, title in TITLES.items():
            dest = OUT / f"{level}_hi_{key}.mp3"
            if args.source:
                from_recording(args.source, dest)
                print(f"{dest.name}: from {args.source.name}")
            else:
                text = sentences(level, title)
                record(text, dest)
                print(f"{dest.name}: {text}")


if __name__ == "__main__":
    main()
