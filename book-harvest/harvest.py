#!/usr/bin/env python3
"""Collect actual public-domain full text only, from Gutenberg harvest and Google Books.
No book metadata, previews, or descriptions are saved as content.
Uses Gutenberg's explicitly permitted /robot/harvest entry point.
"""
from __future__ import annotations
import csv
import html.parser
import io
import json
import os
from pathlib import Path
import re
import subprocess
import time
from urllib.parse import urljoin, urlencode, urlparse, parse_qs
from urllib.request import Request, urlopen
import zipfile

OUT=Path("book-harvest-results")
OUT.mkdir(exist_ok=True)
CHUNK=4000
UA="PublicDomainFullTextResearch/0.1 (Gutenberg sanctioned harvest; Google Books API)"
PAT_START=re.compile(r"\*{3}\s*START OF (?:THE|THIS) PROJECT GUTENBERG (?:EBOOK|E-TEXT)[^\n]*\*{3}", re.I)
PAT_END=re.compile(r"\*{3}\s*END OF (?:THE|THIS) PROJECT GUTENBERG (?:EBOOK|E-TEXT)[^\n]*\*{3}", re.I)
STATS={}
def get(url, limit=20_000_000):
    req=Request(url,headers={"User-Agent":UA})
    with urlopen(req,timeout=40) as rsp:
        if int(rsp.headers.get("Content-Length","0"))>limit:raise ValueError("file too large")
        raw=rsp.read(limit+1)
        if len(raw)>limit:raise ValueError("file too large")
        return raw
def decode(data):
    for enc in ("utf-8-sig","utf-8","cp1252","latin-1"):
        try:return data.decode(enc)
        except UnicodeDecodeError:pass
    return ""
def body(raw):
    text=decode(raw).replace("\r\n","\n").replace("\r","\n").replace("\ufeff","").replace("\x00","")
    a=PAT_START.search(text)
    if not a:return ""
    text=text[a.end():]
    b=PAT_END.search(text)
    if b:text=text[:b.start()]
    text=text.strip()
    return text if len(text)>=500 else ""
def save_csv(path, books):
    with path.open("w",encoding="utf-8-sig",newline="") as f:
        w=csv.writer(f)
        w.writerow(("book_id","chunk_index","content"))
        n=0
        for book_id,text in books.items():
            for idx,i in enumerate(range(0,len(text),CHUNK)):
                w.writerow((book_id,idx,text[i:i+CHUNK]))
                n+=1
    print(path, "books",len(books),"rows",n,"bytes",path.stat().st_size,flush=True)
    return {"books":len(books),"rows":n,"bytes":path.stat().st_size}
class Links(html.parser.HTMLParser):
    def __init__(self):
        super().__init__()
        self.links=[]
    def handle_starttag(self, tag, attrs):
        if tag=="a":
            link=dict(attrs).get("href","")
            if link:self.links.append(link)
def pg_id(name):
    n=Path(name).name.lower()
    m=re.match(r"(?:pg)?(\d{1,7})(?:[-_.][^/]*)?\.(?:txt|utf8)$",n)
    return m.group(1) if m else None
def gutenberg(lang, want=20, pages=4):
    books={}
    url="https://www.gutenberg.org/robot/harvest?"+urlencode({"filetypes[]":"txt","langs[]":lang})
    seen_pages=set()
    seen_zips=set()
    errors=[]
    for page in range(pages):
        if url in seen_pages or len(books)>=want:break
        seen_pages.add(url)
        try:
            parser=Links()
            parser.feed(decode(get(url,limit=2_000_000)))
        except Exception as e:
            errors.append(f"harvest index: {str(e)[:150]}")
            break
        ziplinks=[]
        nextlink=None
        for link in parser.links:
            absolute=urljoin(url,link)
            sp=urlparse(absolute)
            if sp.path.lower().endswith(".zip") and absolute not in seen_zips:
                ziplinks.append(absolute)
            elif "/robot/harvest" in sp.path and (parse_qs(sp.query).get("offset") or parse_qs(sp.query).get("start")) and absolute not in seen_pages:
                nextlink=absolute
        print(f"gutenberg {lang}: page {page+1}, {len(ziplinks)} zip links",flush=True)
        for link in ziplinks:
            if len(books)>=want:break
            seen_zips.add(link)
            try:
                data=get(link)
                with zipfile.ZipFile(io.BytesIO(data)) as z:
                    for name in z.namelist():
                        bid=pg_id(name)
                        if not bid or bid in books or not name.lower().endswith((".txt",".utf8")):continue
                        if z.getinfo(name).file_size>15_000_000:continue
                        text=body(z.read(name))
                        if text:books[bid]=text
                        if len(books)>=want:break
            except Exception as e:
                if len(errors)<10:errors.append(f"zip: {str(e)[:130]}")
            time.sleep(2)
        if not nextlink:break
        url=nextlink
        time.sleep(2)
    STATS[f"gutenberg_{lang}"]=save_csv(OUT/f"gutenberg_{lang}_fulltext.csv",books)
    STATS[f"gutenberg_{lang}"]["errors"]=errors
def epubtext(data):
    class Text(html.parser.HTMLParser):
        def __init__(self):super().__init__();self.parts=[];self.skip=0
        def handle_starttag(self,tag,attrs):
            if tag in ("style","script"):self.skip+=1
            if tag in ("p","div","h1","h2","br","li"):self.parts.append("\n")
        def handle_endtag(self,tag):
            if tag in ("style","script") and self.skip:self.skip-=1
            if tag in ("p","div","h1","h2","li"):self.parts.append("\n")
        def handle_data(self,s):
            if not self.skip:self.parts.append(s)
    sections=[]
    with zipfile.ZipFile(io.BytesIO(data)) as z:
        for name in sorted(z.namelist()):
            if not name.lower().endswith((".html",".xhtml",".htm")):continue
            if any(x in name.lower() for x in ("nav","toc","cover")):continue
            parser=Text()
            parser.feed(decode(z.read(name)))
            sections.append(" ".join("".join(parser.parts).split()))
    return "\n\n".join(x for x in sections if x)
def google(want=5):
    books={}
    errs=[]
    queries={"en":["subject:fiction","subject:philosophy","subject:history"],"ko":["한국 고전","고전 소설"]}
    for lang,qlist in queries.items():
        for q in qlist:
            if len(books)>=want:break
            params={"q":q,"filter":"full","langRestrict":lang,"maxResults":40,"printType":"books"}
            key=os.getenv("GOOGLE_BOOKS_API_KEY")
            if key:params["key"]=key
            try:
                info=json.loads(get("https://www.googleapis.com/books/v1/volumes?"+urlencode(params),limit=4_000_000))
            except Exception as e:
                errs.append(f"API {lang} {q}: {str(e)[:140]}")
                continue
            for item in info.get("items",[]):
                if len(books)>=want:break
                bid=item.get("id")
                access=item.get("accessInfo") or {}
                if not bid or bid in books or access.get("publicDomain") is not True:continue
                for fmt in ("epub","pdf"):
                    f=access.get(fmt) or {}
                    if not f.get("isAvailable") or not f.get("downloadLink"):continue
                    try:
                        raw=get(f["downloadLink"])
                        if fmt=="epub" and zipfile.is_zipfile(io.BytesIO(raw)):
                            text=epubtext(raw)
                        elif fmt=="pdf" and raw.startswith(b"%PDF"):
                            p=subprocess.run(["pdftotext","-layout","-","-"],input=raw,stdout=subprocess.PIPE,stderr=subprocess.PIPE,timeout=90)
                            text=decode(p.stdout) if p.returncode==0 else ""
                        else:continue
                        if len(text.strip())>=500:books[bid]=text.strip();break
                    except Exception as e:
                        if len(errs)<10:errs.append(f"download {bid}: {str(e)[:120]}")
                    finally:time.sleep(1)
            time.sleep(1)
    STATS["google_books"]=save_csv(OUT/"google_books_fulltext.csv",books)
    STATS["google_books"]["errors"]=errs
if __name__=="__main__":
    gutenberg("en",int(os.getenv("MAX_GUTENBERG_EN","20")))
    gutenberg("ko",int(os.getenv("MAX_GUTENBERG_KO","10")))
    google(int(os.getenv("MAX_GOOGLE_BOOKS","5")))
    (OUT/"stats.json").write_text(json.dumps(STATS,ensure_ascii=False,indent=2),encoding="utf-8")
    print(json.dumps(STATS,ensure_ascii=False),flush=True)
