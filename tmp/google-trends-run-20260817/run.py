#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import random
import time
from pathlib import Path

import numpy as np
import pandas as pd
from pytrends.request import TrendReq

BASE = Path(__file__).resolve().parent
KEYWORDS_FILE = BASE / 'keywords.txt'
OUT = BASE / 'out'
OUT.mkdir(parents=True, exist_ok=True)

GEO = 'KR'
TZ = 540
HL = 'ko'
ANCHOR = '유튜브'
BATCH_TARGETS = 4
START = '2004-01-01'
END = (pd.Timestamp.now(tz='Asia/Seoul').normalize() - pd.Timedelta(days=2)).strftime('%Y-%m-%d')
TIMEFRAME = f'{START} {END}'
MAX_RETRIES = 7


def load_keywords():
    txt = KEYWORDS_FILE.read_text(encoding='utf-8')
    raw = txt.split(',')
    out = []
    seen = set()
    for x in raw:
        x = ' '.join(str(x).strip().split())
        if not x:
            continue
        k = x.casefold()
        if k in seen:
            continue
        seen.add(k)
        out.append(x)
    return out


def make_client():
    return TrendReq(hl=HL, tz=TZ, timeout=(15, 60), retries=0, backoff_factor=0)


def fetch(client, terms):
    last = None
    for attempt in range(1, MAX_RETRIES + 1):
        try:
            client.build_payload(
                kw_list=terms,
                cat=0,
                timeframe=TIMEFRAME,
                geo=GEO,
                gprop='websearch',
            )
            df = client.interest_over_time()
            if df is None or df.empty:
                raise RuntimeError('empty response')
            if 'isPartial' in df.columns:
                df = df.drop(columns=['isPartial'])
            return df
        except Exception as e:
            last = e
            wait = min(180, 5 * (2 ** (attempt - 1)) + random.uniform(1, 7))
            print(f'  attempt {attempt}/{MAX_RETRIES} failed: {e}; sleep {wait:.1f}s', flush=True)
            time.sleep(wait)
    raise RuntimeError(f'failed terms={terms}: {last}')


def main():
    kws = load_keywords()
    print(f'keywords={len(kws)} timeframe={TIMEFRAME}', flush=True)

    batches = [kws[i:i+BATCH_TARGETS] for i in range(0, len(kws), BATCH_TARGETS)]
    client = make_client()
    all_ratio = []
    failures = []

    for bi, batch in enumerate(batches, 1):
        terms = [x for x in batch if x.casefold() != ANCHOR.casefold()] + [ANCHOR]
        print(f'BATCH {bi}/{len(batches)}: {terms}', flush=True)
        try:
            df = fetch(client, terms)
            safe = f'{bi:04d}'
            df.to_csv(OUT / f'raw_{safe}.csv', encoding='utf-8-sig')
            anchor = pd.to_numeric(df[ANCHOR], errors='coerce').replace(0, np.nan)
            ratio = pd.DataFrame(index=df.index)
            for kw in batch:
                if kw in df.columns:
                    ratio[kw] = pd.to_numeric(df[kw], errors='coerce') / anchor
            ratio.to_csv(OUT / f'ratio_{safe}.csv', encoding='utf-8-sig')
            all_ratio.append(ratio)
        except Exception as e:
            print(f'  FAILED: {e}', flush=True)
            for kw in batch:
                failures.append({'keyword': kw, 'error': repr(e)})
        if bi < len(batches):
            time.sleep(random.uniform(3, 7))

    if not all_ratio:
        raise SystemExit('No successful batches')

    ts = pd.concat(all_ratio, axis=1)
    ts = ts.loc[:, ~ts.columns.duplicated()]
    ts.index.name = 'date'
    ts.to_csv(OUT / 'timeseries_anchor_relative.csv', encoding='utf-8-sig')

    rows = []
    for kw in ts.columns:
        s = pd.to_numeric(ts[kw], errors='coerce').replace([np.inf, -np.inf], np.nan).dropna()
        if len(s) == 0:
            rows.append({'keyword': kw, 'mean_anchor_relative': np.nan, 'peak_anchor_relative': np.nan, 'peak_date': '', 'observations': 0})
            continue
        mx = float(s.max())
        peak_date = s.index[s == mx].min().strftime('%Y-%m-%d')
        rows.append({'keyword': kw, 'mean_anchor_relative': float(s.mean()), 'peak_anchor_relative': mx, 'peak_date': peak_date, 'observations': int(s.size)})

    summary = pd.DataFrame(rows)
    maxmean = summary['mean_anchor_relative'].max()
    maxpeak = summary['peak_anchor_relative'].max()
    summary['global_mean_index_0_100'] = summary['mean_anchor_relative'] / maxmean * 100 if pd.notna(maxmean) and maxmean > 0 else np.nan
    summary['global_peak_index_0_100'] = summary['peak_anchor_relative'] / maxpeak * 100 if pd.notna(maxpeak) and maxpeak > 0 else np.nan
    summary['rank'] = summary['global_mean_index_0_100'].rank(method='min', ascending=False, na_option='bottom').astype('Int64')
    summary = summary.sort_values(['global_mean_index_0_100', 'keyword'], ascending=[False, True])
    summary.to_csv(OUT / 'trends_summary.csv', index=False, encoding='utf-8-sig')
    try:
        with pd.ExcelWriter(OUT / 'trends_summary.xlsx', engine='openpyxl') as w:
            summary.to_excel(w, sheet_name='summary', index=False)
            ts.to_excel(w, sheet_name='timeseries')
            pd.DataFrame({'setting':['geo','timeframe','anchor','batch_targets'],'value':[GEO,TIMEFRAME,ANCHOR,BATCH_TARGETS]}).to_excel(w, sheet_name='settings', index=False)
    except Exception as e:
        print('xlsx write failed:', e, flush=True)
    if failures:
        pd.DataFrame(failures).to_csv(OUT / 'failed_keywords.csv', index=False, encoding='utf-8-sig')

    print('DONE', flush=True)
    print(summary.head(20).to_string(index=False), flush=True)

if __name__ == '__main__':
    main()
