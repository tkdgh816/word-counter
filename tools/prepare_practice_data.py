#!/usr/bin/env python3
"""Instructor-only input preparation. Students use the committed data directly."""

import argparse
import csv
import hashlib
import html
import io
import json
from pathlib import Path


SOURCES = {
    "ynat-train.json": {
        "sha256": "e1ceadbcfc654bfbd241ac8170fe825ceeb6e7c0686f05181befeaef778d0031",
        "url": "https://raw.githubusercontent.com/KLUE-benchmark/KLUE/3efd98708a40ff49251fddde35453f8fbb11f536/klue_benchmark/ynat-v1.1/ynat-v1.1_train.json",
    },
    "klue-license.txt": {
        "sha256": "7abe19ec9bb73b36141b999b861d24ad855e808bafe0f81e84cce28556f6c297",
        "url": "https://raw.githubusercontent.com/KLUE-benchmark/KLUE/3efd98708a40ff49251fddde35453f8fbb11f536/License.md",
    },
    "chatbot.csv": {
        "sha256": "287eb129695b577321c80ad397bb3c2279164d4ca577874d129fd3db5b30afe2",
        "url": "https://raw.githubusercontent.com/songys/Chatbot_data/4cf20d13fc46f5037fd1c531cd566e2dd9f72974/ChatbotData.csv",
    },
    "chatbot-license.txt": {
        "sha256": "1d8084ef9ea897ec462ce6c0e538f97967894699aac65c933804062714d3330a",
        "url": "https://raw.githubusercontent.com/songys/Chatbot_data/4cf20d13fc46f5037fd1c531cd566e2dd9f72974/LICENSE",
    },
}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source-dir", type=Path, required=True,
                        help="Directory containing the four pinned source files")
    parser.add_argument("--output-dir", type=Path,
                        default=Path(__file__).resolve().parents[1] / "data")
    args = parser.parse_args()
    raw = {}
    for name, info in SOURCES.items():
        raw[name] = (args.source_dir / name).read_bytes()
        if hashlib.sha256(raw[name]).hexdigest() != info["sha256"]:
            raise ValueError("Source checksum mismatch: " + name)

    records = json.loads(raw["ynat-train.json"].decode("utf-8"))
    titles = [record["title"] for record in records]
    if len(titles) != 45678 or any(not isinstance(t, str) for t in titles):
        raise ValueError("Unexpected YNAT structure")
    # These titles can also be represented as the assignment's simple TSV/TXT.
    if any(any(c in t for c in "\r\n\t") for t in titles[:1000]):
        raise ValueError("The alternate-format sample requires single-line titles")
    chatbot = list(csv.reader(io.StringIO(raw["chatbot.csv"].decode("utf-8-sig"))))
    if chatbot[0] != ["Q", "A", "label"] or len(chatbot) - 1 != 11823:
        raise ValueError("Unexpected Chatbot Data structure")
    if any(len(row) != 3 for row in chatbot[1:]):
        raise ValueError("Invalid Chatbot Data row")

    out = args.output_dir
    files = []

    def record_file(relative, rows=None, columns=None):
        payload = (out / relative).read_bytes()
        item = {"path": relative, "bytes": len(payload),
                "sha256": hashlib.sha256(payload).hexdigest()}
        if rows is not None:
            item["records"] = rows
        if columns is not None:
            item["text_columns"] = columns
        files.append(item)

    def write_bytes(relative, payload, rows=None, columns=None):
        target = out / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(payload)
        record_file(relative, rows, columns)

    def write_news(relative, values, start=0):
        target = out / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        with target.open("w", encoding="utf-8", newline="") as stream:
            writer = csv.writer(stream, lineterminator="\n")
            writer.writerow(["id", "text"])
            for index, title in enumerate(values, start + 1):
                writer.writerow([index, title])
        record_file(relative, len(values), ["text"])

    def split_news(relative, values):
        # Disjoint contiguous slices; each record occurs once across 16 files.
        for index in range(16):
            start = len(values) * index // 16
            end = len(values) * (index + 1) // 16
            write_news(f"{relative}/news-{index + 1:02d}.csv", values[start:end], start)

    for count in [1000, 10000]:
        write_news(f"klue-ynat/news-{count}.csv", titles[:count])
    write_news("klue-ynat/news-full.csv", titles)
    split_news("klue-ynat/many", titles)

    repeated = titles * 8
    write_news("performance/news-repeat-8.csv", repeated)
    split_news("performance/many", repeated)

    first = titles[:1000]
    write_bytes("klue-ynat/formats/news-1000.txt",
                ("\n".join(first) + "\n").encode("utf-8"), len(first))
    tsv = "id\tdocument\n" + "".join(f"{i}\t{t}\n" for i, t in enumerate(first, 1))
    write_bytes("klue-ynat/formats/news-1000.tsv", tsv.encode("utf-8"), len(first), ["document"])
    markup = '<!doctype html>\n<html lang="ko"><head><meta charset="UTF-8"><title>연습용 뉴스</title></head>\n<body><nav>분석 제외 메뉴</nav><article id="content">\n'
    markup += "".join("<p>" + html.escape(title, quote=True) + "</p>\n" for title in first)
    markup += "</article><footer>분석 제외 꼬리말</footer></body></html>\n"
    write_bytes("klue-ynat/formats/news-1000.html", markup.encode("utf-8"), len(first))
    # Re-encode only; keep all original columns and values.
    buffer = io.StringIO(newline="")
    writer = csv.writer(buffer, lineterminator="\n")
    writer.writerows(chatbot)
    write_bytes("chatbot/chatbot.csv", buffer.getvalue().encode("utf-8"), len(chatbot) - 1, ["Q", "A"])
    write_bytes("licenses/KLUE-CC-BY-SA-4.0.txt", raw["klue-license.txt"])
    write_bytes("licenses/Chatbot-MIT.txt", raw["chatbot-license.txt"])

    manifest = {
        "description": "Fixed inputs for the Java word counter assignment; not student results.",
        "sources": SOURCES,
        "transformations": {
            "klue-ynat": "Training split titles in original order; 1-based id plus text, no title normalization; subsets use first N records; many partitions the full data into 16 disjoint slices.",
            "formats": "First 1000 titles as TXT, simple TSV, and escaped static HTML; original title text retained.",
            "performance": "All 45678 titles repeated exactly 8 times (365424 records), new sequential ids; many partitions that same sequence into 16 disjoint slices. This is repeated load, not additional real examples.",
            "chatbot": "All 11823 Q/A/label records retained; UTF-8 without BOM, LF line endings.",
        },
        "files": files,
    }
    (out / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Prepared {len(files)} files; {sum(f['bytes'] for f in files):,} bytes.")
    for item in files:
        if "/many/" not in item["path"]:
            print(f"{item['path']}: {item['bytes']:,} bytes, {item.get('records', '-')} records")


if __name__ == "__main__":
    main()
