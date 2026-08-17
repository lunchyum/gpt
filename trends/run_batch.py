from pathlib import Path
import pandas as pd
from trendspy import Trends

terms = [x.strip() for x in Path('trends/batch-01.txt').read_text(encoding='utf-8').splitlines() if x.strip()]
anchor = '유튜브'
tr = Trends()

df = tr.interest_over_time(terms + [anchor], timeframe='all', geo='KR')
if 'isPartial' in df.columns:
    df = df.drop(columns=['isPartial'])

out = Path('trends/results')
out.mkdir(parents=True, exist_ok=True)
df.to_csv(out/'batch-01-timeseries.csv', encoding='utf-8-sig')

rows=[]
for term in terms:
    if term not in df.columns:
        rows.append({'keyword':term,'mean_anchor_relative':None,'max_anchor_relative':None,'max_date':None,'observations':0})
        continue
    s = pd.to_numeric(df[term], errors='coerce')
    a = pd.to_numeric(df[anchor], errors='coerce').replace(0, pd.NA)
    ratio = (s/a).dropna()
    if ratio.empty:
        rows.append({'keyword':term,'mean_anchor_relative':None,'max_anchor_relative':None,'max_date':None,'observations':0})
    else:
        mx=ratio.max()
        rows.append({'keyword':term,'mean_anchor_relative':float(ratio.mean()),'max_anchor_relative':float(mx),'max_date':ratio.idxmax().strftime('%Y-%m-%d'),'observations':int(ratio.count())})

summary=pd.DataFrame(rows).sort_values('mean_anchor_relative', ascending=False)
summary.to_csv(out/'batch-01-summary.csv', index=False, encoding='utf-8-sig')
summary.to_excel(out/'batch-01-summary.xlsx', index=False)
