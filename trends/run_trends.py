import os, random, time, logging
from pathlib import Path
import pandas as pd
import numpy as np
from pytrends.request import TrendReq

GEO='KR'; HL='ko'; TZ=540; ANCHOR='유튜브'; BATCH_SIZE=4
START='2004-01-01'; END=(pd.Timestamp.now(tz='Asia/Seoul').normalize()-pd.Timedelta(days=2)).strftime('%Y-%m-%d')
TIMEFRAME=f'{START} {END}'
RAW=Path('trends/raw'); RAW.mkdir(parents=True,exist_ok=True)
logging.basicConfig(level=logging.INFO,format='%(asctime)s %(levelname)s %(message)s')
log=logging.getLogger('trends')

text=Path('trends/keywords.txt').read_text(encoding='utf-8')
keywords=[]; seen=set()
for x in text.split(','):
    x=' '.join(x.strip().split())
    if x and x.casefold() not in seen:
        seen.add(x.casefold()); keywords.append(x)

# Avoid duplicating anchor as a target.
keywords=[x for x in keywords if x.casefold()!=ANCHOR.casefold()]

py=TrendReq(hl=HL,tz=TZ,timeout=(15,60),retries=0,backoff_factor=0)
all_norm=[]; failed=[]

for i in range(0,len(keywords),BATCH_SIZE):
    batch=keywords[i:i+BATCH_SIZE]
    q=batch+[ANCHOR]
    ok=False
    for attempt in range(6):
        try:
            log.info('batch %d/%d: %s', i//BATCH_SIZE+1, (len(keywords)+BATCH_SIZE-1)//BATCH_SIZE, q)
            py.build_payload(q,cat=0,timeframe=TIMEFRAME,geo=GEO,gprop='')
            df=py.interest_over_time()
            if df is None or df.empty: raise RuntimeError('empty response')
            if 'isPartial' in df: df=df.drop(columns=['isPartial'])
            safe='_'.join(str(x)[:24].replace('/','_') for x in q)
            df.to_csv(RAW/f'{i:04d}_{safe}.csv',encoding='utf-8-sig')
            a=pd.to_numeric(df[ANCHOR],errors='coerce').replace(0,np.nan)
            norm=pd.DataFrame(index=df.index)
            for k in batch:
                if k in df:
                    norm[k]=pd.to_numeric(df[k],errors='coerce')/a
                else:
                    norm[k]=np.nan
            all_norm.append(norm); ok=True; break
        except Exception as e:
            log.warning('attempt %d failed: %r',attempt+1,e)
            time.sleep(min(180,10*(2**attempt)+random.uniform(2,8)))
    if not ok:
        failed += batch
    time.sleep(random.uniform(8,15))

if not all_norm:
    raise SystemExit('No successful Google Trends batches')

ts=pd.concat(all_norm,axis=1)
ts=ts.loc[:,~ts.columns.duplicated()]
ts.index.name='date'
rows=[]
for k in keywords:
    s=pd.to_numeric(ts[k],errors='coerce').dropna() if k in ts else pd.Series(dtype=float)
    if s.empty:
        rows.append({'keyword':k,'mean_anchor_relative':np.nan,'peak_anchor_relative':np.nan,'peak_date':None,'observations':0})
    else:
        m=float(s.mean()); p=float(s.max()); d=s.index[s==p].min()
        rows.append({'keyword':k,'mean_anchor_relative':m,'peak_anchor_relative':p,'peak_date':pd.Timestamp(d).strftime('%Y-%m-%d'),'observations':int(s.count())})
summary=pd.DataFrame(rows)
mx=summary['mean_anchor_relative'].max(); summary['index_0_100']=summary['mean_anchor_relative']/mx*100 if pd.notna(mx) and mx>0 else np.nan
summary['rank']=summary['index_0_100'].rank(method='min',ascending=False,na_option='bottom').astype('Int64')
summary=summary.sort_values(['index_0_100','keyword'],ascending=[False,True])
summary.to_csv('trends_summary.csv',index=False,encoding='utf-8-sig')
ts.to_csv('trends_timeseries.csv',encoding='utf-8-sig')
if failed: pd.DataFrame({'keyword':failed}).to_csv('failed_keywords.csv',index=False,encoding='utf-8-sig')
with pd.ExcelWriter('trends_summary.xlsx',engine='openpyxl') as w:
    summary.to_excel(w,index=False,sheet_name='summary')
    ts.to_excel(w,sheet_name='timeseries')
print(f'DONE keywords={len(keywords)} success={len(summary)-len(failed)} failed={len(failed)} timeframe={TIMEFRAME}')
