from pathlib import Path
from datetime import date, timedelta
import time, random
import pandas as pd
from pytrends.request import TrendReq

ROOT = Path(__file__).parent
keywords = [x.strip() for x in (ROOT/'keywords.txt').read_text(encoding='utf-8').splitlines() if x.strip()]
# Common high-volume anchor keeps separate 5-term requests on a shared scale.
ANCHOR = '유튜브'
BATCH = 4
END = date.today() - timedelta(days=2)
TIMEFRAME = f'2004-01-01 {END.isoformat()}'

out = ROOT/'raw'
out.mkdir(exist_ok=True)

pt = TrendReq(hl='ko', tz=540, timeout=(10,30), retries=0, backoff_factor=0)
frames=[]
failed=[]

for i in range(0, len(keywords), BATCH):
    targets = keywords[i:i+BATCH]
    terms = targets + [ANCHOR]
    ok=False
    for attempt in range(1,7):
        try:
            pt.build_payload(terms, cat=0, timeframe=TIMEFRAME, geo='KR', gprop='websearch')
            df = pt.interest_over_time()
            if df is None or df.empty:
                raise RuntimeError('empty response')
            if 'isPartial' in df.columns:
                df=df.drop(columns=['isPartial'])
            df.to_csv(out/f'batch_{i//BATCH+1:03d}.csv', encoding='utf-8-sig')
            frames.append(df)
            ok=True
            break
        except Exception as e:
            if attempt == 6:
                failed.extend((k, repr(e)) for k in targets)
            time.sleep(min(180, 5*(2**attempt)+random.uniform(0,10)))
    time.sleep(random.uniform(5,10))

if not frames:
    raise SystemExit('No successful Google Trends requests')

# Convert each target to target/anchor. This produces a common cross-batch index.
series=[]
for df in frames:
    a=df[ANCHOR].replace(0, pd.NA)
    r=df.drop(columns=[ANCHOR]).div(a, axis=0)
    series.append(r)
ts=pd.concat(series, axis=1)
ts=ts.loc[:, ~ts.columns.duplicated()]

def one(kw):
    s=pd.to_numeric(ts[kw], errors='coerce').dropna()
    if s.empty: return {'keyword':kw,'mean_anchor_relative':None,'peak_anchor_relative':None,'peak_date':None,'observations':0}
    m=s.mean(); p=s.max(); d=s.index[s==p].min()
    return {'keyword':kw,'mean_anchor_relative':float(m),'peak_anchor_relative':float(p),'peak_date':d.strftime('%Y-%m-%d'),'observations':int(s.size)}

summary=pd.DataFrame(one(k) for k in keywords)
mx=summary.mean_anchor_relative.max()
summary['index_0_100'] = summary.mean_anchor_relative/mx*100 if pd.notna(mx) and mx>0 else pd.NA
summary['rank'] = summary.index_0_100.rank(method='min', ascending=False).astype('Int64')
summary=summary.sort_values(['index_0_100','keyword'], ascending=[False,True])
summary.to_csv(ROOT/'summary.csv', index=False, encoding='utf-8-sig')
ts.to_csv(ROOT/'timeseries.csv', encoding='utf-8-sig')
if failed:
    pd.DataFrame(failed, columns=['keyword','error']).to_csv(ROOT/'failed.csv', index=False, encoding='utf-8-sig')
print(summary.to_string(index=False))
