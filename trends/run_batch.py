from pathlib import Path
import pandas as pd
from pytrends.request import TrendReq

terms = [x.strip() for x in Path('trends/batch-01.txt').read_text(encoding='utf-8').splitlines() if x.strip()]
anchor = '유튜브'
pytrends = TrendReq(hl='ko', tz=540, timeout=(10,30), retries=0, backoff_factor=0)
# pytrends uses an empty gprop to indicate standard web search.
pytrends.build_payload(terms + [anchor], cat=0, timeframe='2004-01-01 2026-08-15', geo='KR', gprop='')
df = pytrends.interest_over_time().drop(columns=['isPartial'], errors='ignore')

out = Path('trends/results')
out.mkdir(parents=True, exist_ok=True)
df.to_csv(out/'batch-01-timeseries.csv', encoding='utf-8-sig')

rows=[]
for term in terms:
    s = df[term].astype(float)
    a = df[anchor].astype(float).replace(0, pd.NA)
    ratio = (s/a).dropna()
    if ratio.empty:
        rows.append({'keyword':term,'mean_anchor_relative':None,'max_anchor_relative':None,'max_date':None,'observations':0})
    else:
        mx=ratio.max()
        rows.append({'keyword':term,'mean_anchor_relative':float(ratio.mean()),'max_anchor_relative':float(mx),'max_date':ratio.idxmax().strftime('%Y-%m-%d'),'observations':int(ratio.count())})

summary=pd.DataFrame(rows).sort_values('mean_anchor_relative', ascending=False)
summary.to_csv(out/'batch-01-summary.csv', index=False, encoding='utf-8-sig')
summary.to_excel(out/'batch-01-summary.xlsx', index=False)
