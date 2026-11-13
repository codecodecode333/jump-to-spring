"""
설명 영상 빌드 스크립트.

  narration/NN-slug.txt  (한 줄 = 자막 한 줄 = TTS 한 덩어리)
      │  edge-tts 로 줄마다 음성 생성 (캐시: build/tts-cache/)
      ▼
  audio/NN-slug.mp3      (장 하나의 내레이션, 줄 사이에 짧은 쉼)
  build/NN-slug.cues.json (줄마다 장 기준 시작/끝 시간)
      │
      ├─ compositions/NN-slug.html 안의 /*CUES*/[...]/*END*/ 를 실제 시작 시간 배열로 교체
      │   → 장면 애니메이션을 "몇 번째 문장이 나올 때"에 맞출 수 있다
      └─ index.html (루트) 생성: 장 배치 + 내레이션 오디오 + 하단 자막 + 배경음악
         build/chapters.txt: 유튜브 설명란용 챕터 시간표

사용법 (video 폴더에서):
  .venv/Scripts/python scripts/build.py            # 음성 생성 + 루트 생성
  .venv/Scripts/python scripts/build.py --no-tts   # 기존 음성으로 루트만 다시 생성
  .venv/Scripts/python scripts/build.py --only=00-intro   # 이 장만 음성 생성
"""
import asyncio
import hashlib
import json
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
NARRATION = ROOT / "narration"
AUDIO = ROOT / "audio"
BUILD = ROOT / "build"
CACHE = BUILD / "tts-cache"
COMPOSITIONS = ROOT / "compositions"

VOICE = "ko-KR-InJoonNeural"
RATE = "+5%"          # 분당 약 300자 목표
GAP = 0.35            # 줄 사이 쉼(초)
CHAPTER_TAIL = 0.8    # 장 끝 여유(초)
PLACEHOLDER_SEC = 4.0  # 원고가 없는 장의 임시 길이

CHAPTERS = [
    ("00-intro", "인트로"),
    ("01-project", "프로젝트 소개"),
    ("02-url", "URL 설계"),
    ("03-entity", "엔티티 설계"),
    ("04-nplus1", "N+1 문제"),
    ("05-security", "보안"),
    ("06-test", "테스트"),
    ("07-form", "폼과 사용성"),
    ("08-package", "패키지 구조"),
    ("09-outro", "마무리"),
]


def read_lines(path: Path) -> list[tuple[str, str]]:
    """(자막, 읽을 말) 목록. "자막 || 읽을 말" 로 쓰면 둘을 다르게 할 수 있다.
    예) `BaseEntity`로 공통 필드를 모읍니다. || 베이스 엔티티로 공통 필드를 모읍니다."""
    lines = []
    for raw in path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if line and not line.startswith("#"):
            display, _, speech = line.partition("||")
            lines.append((display.strip(), (speech or display).strip()))
    return lines


def duration_of(path: Path) -> float:
    out = subprocess.run(
        ["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", str(path)],
        capture_output=True, text=True, check=True,
    ).stdout.strip()
    return float(out)


async def tts(text: str, out: Path) -> None:
    import edge_tts
    await edge_tts.Communicate(text, VOICE, rate=RATE).save(str(out))


def synthesize(slug: str, lines: list[tuple[str, str]]) -> list[dict]:
    """줄마다 음성을 만들고(캐시), 쉼을 넣어 이어 붙인 뒤 줄별 시간을 돌려준다."""
    CACHE.mkdir(parents=True, exist_ok=True)
    AUDIO.mkdir(parents=True, exist_ok=True)
    parts, cues, t = [], [], 0.0
    for display, speech in lines:
        key = hashlib.sha1(f"{VOICE}|{RATE}|{speech}".encode()).hexdigest()[:16]
        mp3 = CACHE / f"{key}.mp3"
        if not mp3.exists():
            asyncio.run(tts(speech, mp3))
        d = duration_of(mp3)
        cues.append({"start": round(t, 3), "end": round(t + d, 3), "text": display})
        parts.append(mp3)
        t += d + GAP

    # ffmpeg concat: 줄 음성 + 쉼(무음)을 순서대로 이어 붙인다
    silence = CACHE / f"silence-{GAP}.mp3"
    if not silence.exists():
        subprocess.run(["ffmpeg", "-v", "error", "-y", "-f", "lavfi", "-i", "anullsrc=r=24000:cl=mono",
                        "-t", str(GAP), "-q:a", "9", "-acodec", "libmp3lame", str(silence)], check=True)
    listfile = BUILD / f"{slug}.concat.txt"
    with listfile.open("w", encoding="utf-8") as f:
        for p in parts:
            f.write(f"file '{p.as_posix()}'\nfile '{silence.as_posix()}'\n")
    subprocess.run(["ffmpeg", "-v", "error", "-y", "-f", "concat", "-safe", "0", "-i", str(listfile),
                    "-ar", "24000", "-ac", "1", "-b:a", "96k", str(AUDIO / f"{slug}.mp3")], check=True)
    return cues


def inject_cues(slug: str, cues: list[dict], duration: float) -> None:
    """장 HTML 의 /*CUES*/[...]/*END*/ 와 /*DUR*/0/*END*/ 를 실제 값으로 바꾼다."""
    path = COMPOSITIONS / f"{slug}.html"
    s = path.read_text(encoding="utf-8")
    starts = json.dumps([c["start"] for c in cues])
    s = re.sub(r"/\*CUES\*/.*?/\*END\*/", f"/*CUES*/{starts}/*END*/", s, flags=re.S)
    s = re.sub(r"/\*DUR\*/.*?/\*END\*/", f"/*DUR*/{duration}/*END*/", s, flags=re.S)
    path.write_text(s, encoding="utf-8", newline="\n")


def esc(text: str) -> str:
    return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def build(no_tts: bool, only: set[str]) -> None:
    BUILD.mkdir(exist_ok=True)
    timeline, t = [], 0.0
    for slug, title in CHAPTERS:
        txt = NARRATION / f"{slug}.txt"
        cues_file = BUILD / f"{slug}.cues.json"
        lines = read_lines(txt) if txt.exists() else []
        # --only 로 고른 장만 음성을 만든다. 나머지는 이미 만든 음성을 쓰거나, 아직 없으면 임시 길이로 둔다.
        make_tts = not no_tts and (not only or slug in only)
        if lines and make_tts:
            cues = synthesize(slug, lines)
            cues_file.write_text(json.dumps(cues, ensure_ascii=False, indent=1), encoding="utf-8")
        elif cues_file.exists() and lines:
            cues = json.loads(cues_file.read_text(encoding="utf-8"))
        else:
            cues = []
        has_audio = bool(cues) and (AUDIO / f"{slug}.mp3").exists()
        # 원고에 "# tail=7" 처럼 쓰면 마지막 문장 뒤에 그만큼 화면을 더 보여 준다 (결과 화면을 읽을 시간)
        m = re.search(r"^#\s*tail\s*=\s*([\d.]+)", txt.read_text(encoding="utf-8"), re.M) if txt.exists() else None
        tail = float(m.group(1)) if m else CHAPTER_TAIL
        duration = round(cues[-1]["end"] + tail, 3) if has_audio else PLACEHOLDER_SEC
        inject_cues(slug, cues, duration)
        timeline.append({"slug": slug, "title": title, "start": round(t, 3), "duration": duration,
                         "cues": cues, "audio": has_audio})
        t += duration

    total = round(t, 3)
    write_index(timeline, total)
    write_chapters(timeline)
    print(f"total {total:.1f}s ({int(total // 60)}m {total % 60:.0f}s)")
    for c in timeline:
        print(f"  {c['slug']:<12} start {c['start']:7.2f}  dur {c['duration']:6.2f}  cues {len(c['cues'])}")


def write_index(timeline: list[dict], total: float) -> None:
    chapters, audios, captions = [], [], []
    for i, c in enumerate(timeline):
        cid = "ch" + c["slug"][:2]
        chapters.append(
            f'      <div id="{cid}-host" data-composition-id="{cid}" data-composition-src="compositions/{c["slug"]}.html"'
            f' data-start="{c["start"]}" data-duration="{c["duration"]}" data-track-index="0"></div>')
        if c["audio"]:
            audio_len = round(duration_of(AUDIO / f"{c['slug']}.mp3"), 3)  # 슬롯 = 실제 음성 길이
            audios.append(
                f'      <audio id="vo-{cid}" class="clip" src="audio/{c["slug"]}.mp3" data-start="{c["start"]}"'
                f' data-duration="{audio_len}" data-track-index="2" data-volume="1"></audio>')
        for j, cue in enumerate(c["cues"]):
            s = round(c["start"] + cue["start"], 3)
            d = round(cue["end"] - cue["start"] + 0.25, 3)
            captions.append(
                f'      <div id="cap-{cid}-{j:02d}" class="clip caption" data-start="{s}" data-duration="{d}"'
                f' data-track-index="3" data-layout-ignore><span>{esc(cue["text"])}</span></div>')
    bgm = ""
    if (ROOT / "audio" / "bgm.mp3").exists():
        bgm = (f'      <audio id="bgm" class="clip" src="audio/bgm.mp3" data-start="0" data-duration="{total}"'
               f' data-track-index="4" data-volume="0.12" data-fade-in="2" data-fade-out="3"></audio>')
    html = f"""<!doctype html>
<!-- 이 파일은 scripts/build.py 가 생성한다. 직접 고치지 말고 compositions/ 와 narration/ 을 고친 뒤 다시 빌드할 것 -->
<html lang="ko" data-resolution="landscape">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=1920, height=1080" />
    <script src="https://cdn.jsdelivr.net/npm/gsap@3.15.0/dist/gsap.min.js"></script>
    <link rel="stylesheet" href="assets/theme.css" />
  </head>
  <body>
    <div id="root" data-composition-id="main" data-start="0" data-duration="{total}" data-width="1920" data-height="1080">
      <!-- 장(서브 컴포지션) -->
{chr(10).join(chapters)}
      <!-- 내레이션 -->
{chr(10).join(audios)}
      <!-- 배경음악 -->
{bgm}
      <!-- 자막 -->
{chr(10).join(captions)}
    </div>
    <script>
      window.__timelines = window.__timelines || {{}};
      const tl = gsap.timeline({{ paused: true }});
      window.__timelines["main"] = tl;
      tl.seek(0);
    </script>
  </body>
</html>
"""
    (ROOT / "index.html").write_text(html, encoding="utf-8", newline="\n")


def write_chapters(timeline: list[dict]) -> None:
    lines = []
    for c in timeline:
        m, s = divmod(int(c["start"]), 60)
        lines.append(f"{m}:{s:02d} {c['title']}")
    (BUILD / "chapters.txt").write_text("\n".join(lines) + "\n", encoding="utf-8")


if __name__ == "__main__":
    # 예) build.py --only 00-intro,01-project
    only_arg = next((a.split("=", 1)[1] for a in sys.argv if a.startswith("--only=")), "")
    build(no_tts="--no-tts" in sys.argv, only={s for s in only_arg.split(",") if s})
