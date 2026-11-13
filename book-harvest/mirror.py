#!/usr/bin/env python3
"""Fallback: Project Gutenberg texts from the public GITenberg GitHub mirror.
Only complete bodies between Project Gutenberg START/END markers enter CSV.
"""
import csv
import json
from pathlib import Path
import re
import time
from urllib.request import Request,urlopen
from urllib.error import HTTPError,URLError

REPOS=[
"Pride-and-Prejudice_1342",
"Moby-Dick--Or-The-Whale_2701",
"The-Communist-Manifesto_61",
"Shakespeare-s-Sonnets_1041",
"Adventures-of-Huckleberry-Finn_76",
"The-Art-of-War_132",
"Around-the-World-in-80-Days_103",
"Frankenstein_84",
"Dubliners_2814",
"Aesop-s-Fables--a-new-translation_11339",
"The-Adventures-of-Sherlock-Holmes_1661",
"The-Importance-of-Being-Earnest--A-Trivial-Comedy-for-Serious-People_844",
"Jane-Eyre_1260",
"Alice-s-Adventures-in-Wonderland_11",
"Tractatus-Logico-Philosophicus_5740",
"Anna-Karenina_1399",
"The-Mysterious-Island_1268",
"The-Time-Machine_35",
"The-Extraordinary-Adventures-of-Arsene-Lupin-Gentleman-Burglar_6133",
"A-Christmas-Carol_46",
"The-Phantom-of-the-Opera_175",
"The-Count-of-Monte-Cristo_1184",
"The-Great-Gatsby_64317",
"Twenty-Thousand-Leagues-under-the-Sea_164",
"Crime-and-Punishment_2554",
"The-King-James-Bible_10900",
"Divine-Comedy-Longfellow-s-Translation-Hell_1001",
"The-First-Six-Books-of-the-Elements-of-Euclid_21076",
]
OUT=Path("book-harvest-results")
OUT.mkdir(exist_ok=True)
ST=re.compile(r"\*{3}\s*START OF (?:THE|THIS) PROJECT GUTENBERG (?:EBOOK|E-TEXT)[^\n]*\*{3}",re.I)
ED=re.compile(r"\*{3}\s*END OF (?:THE|THIS) PROJECT GUTENBERG (?:EBOOK|E-TEXT)[^\n]*\*{3}",re.I)
def fetch(url):
    req=Request(url,headers={"User-Agent":"PublicDomainBookFullTextResearch/1.0"})
    with urlopen(req,timeout=30) as r:
        if int(r.headers.get("Content-Length","0"))>12_000_000:raise ValueError("large")
        result=r.read(12_000_001)
        if len(result)>12_000_000:raise ValueError("large")
        return result
def decode(data):
    for codec in ("utf-8-sig","utf-8","cp1252","latin-1"):
        try:return data.decode(codec)
        except UnicodeDecodeError:continue
    return ""
def clean(raw):
    text=decode(raw).replace("\r\n","\n").replace("\r","\n").replace("\ufeff","").replace("\x00","")
    a=ST.search(text)
    if not a:return ""
    text=text[a.end():]
    b=ED.search(text)
    if b:text=text[:b.start()]
    return text.strip() if len(text.strip())>=500 else ""
def main():
    books={}
    failures=[]
    lim=20
    for repo in REPOS:
        if len(books)>=lim:break
        bid=repo.rsplit("_",1)[1]
        base="https://raw.githubusercontent.com/GITenberg/"+repo+"/master/"
        # Most book repos have one of these original PG text files.
        for fname in [f"{bid}-0.txt",f"{bid}.txt",f"{bid}-8.txt",f"pg{bid}.txt"]:
            try:
                data=fetch(base+fname)
                txt=clean(data)
                if txt:
                    books[bid]=txt
                    print("GITenberg mirrored book",bid,repo,"characters",len(txt),flush=True)
                    break
            except (HTTPError,URLError,TimeoutError,ValueError) as err:
                continue
        if bid not in books:failures.append(repo)
        time.sleep(0.15)
    path=OUT/"gutenberg_en_fulltext.csv"
    rows=0
    with path.open("w",encoding="utf-8-sig",newline="") as f:
        w=csv.writer(f)
        w.writerow(["book_id","chunk_index","content"])
        for bid,txt in books.items():
            for n,i in enumerate(range(0,len(txt),4000)):
                w.writerow([bid,n,txt[i:i+4000]])
                rows+=1
    print("Actual mirrored Gutenberg bodies",len(books),"CSV rows",rows,"size",path.stat().st_size,flush=True)
    statsfile=OUT/"stats.json"
    stats=json.loads(statsfile.read_text(encoding="utf-8")) if statsfile.exists() else {}
    stats["gutenberg_en"]={"books":len(books),"rows":rows,"bytes":path.stat().st_size,"source":"GITenberg GitHub mirror of Project Gutenberg","failed_repos":failures}
    statsfile.write_text(json.dumps(stats,ensure_ascii=False,indent=2),encoding="utf-8")
if __name__=="__main__":main()
