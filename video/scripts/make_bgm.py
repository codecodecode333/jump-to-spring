"""
배경음악 합성: 저작권 걱정 없이 FFmpeg 사인파로 잔잔한 패드 음을 직접 만든다.

  화음 4개(각 6초, Cmaj7 → Am7 → Fmaj7 → G6)를 부드럽게 겹쳐 24초 반복 단위를 만들고,
  영상 전체 길이만큼 반복한 뒤 저역 통과 필터로 날카로운 소리를 깎는다.
  → audio/bgm.mp3  (build.py 가 있으면 루트에 data-volume 0.12 ≈ -18dB 로 깐다)

사용법 (video 폴더): .venv/Scripts/python scripts/make_bgm.py <초>
"""
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "audio" / "bgm.mp3"
TMP = ROOT / "build" / "bgm"

CHORDS = {  # 주파수(Hz): 낮은 음역에서 부드럽게
    "Cmaj7": [130.81, 164.81, 196.00, 246.94],
    "Am7": [110.00, 130.81, 164.81, 196.00],
    "Fmaj7": [87.31, 130.81, 174.61, 220.00],
    "G6": [98.00, 146.83, 196.00, 246.94],
}
CHORD_SEC = 6


def run(args):
    subprocess.run(["ffmpeg", "-v", "error", "-y", *args], check=True)


def main(total: float):
    TMP.mkdir(parents=True, exist_ok=True)
    parts = []
    for i, (name, freqs) in enumerate(CHORDS.items()):
        # 음마다 사인파 + 아주 느린 떨림(0.1~0.2Hz)으로 살아 있는 느낌
        expr = " + ".join(
            f"0.18*sin(2*PI*{f}*t)*(0.8+0.2*sin(2*PI*{0.1 + 0.03 * k}*t))" for k, f in enumerate(freqs))
        wav = TMP / f"{i}-{name}.wav"
        run(["-f", "lavfi", "-i", f"aevalsrc='{expr}':s=44100:d={CHORD_SEC + 2}",
             "-af", f"afade=t=in:d=2,afade=t=out:st={CHORD_SEC}:d=2", str(wav)])
        parts.append(wav)

    # 화음 사이 2초씩 겹치기(acrossfade) → 24초 반복 단위
    unit = TMP / "unit.wav"
    inputs = sum([["-i", str(p)] for p in parts], [])
    chain = "[0][1]acrossfade=d=2[a];[a][2]acrossfade=d=2[b];[b][3]acrossfade=d=2"
    run([*inputs, "-filter_complex", chain, str(unit)])

    # 영상 길이만큼 반복 + 저역 통과 + 앞뒤 페이드 + 음량 정규화
    OUT.parent.mkdir(exist_ok=True)
    run(["-stream_loop", "-1", "-i", str(unit), "-t", str(total),
         "-af", f"lowpass=f=1200,afade=t=in:d=3,afade=t=out:st={max(total - 4, 0)}:d=4,loudnorm=I=-20:TP=-2",
         "-ar", "44100", "-ac", "2", "-b:a", "128k", str(OUT)])
    print(f"bgm: {OUT} ({total:.1f}s)")


if __name__ == "__main__":
    main(float(sys.argv[1]))
