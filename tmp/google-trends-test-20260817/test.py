from pytrends.request import TrendReq
p=TrendReq(hl='ko',tz=540,timeout=(10,30),retries=0,backoff_factor=0)
p.build_payload(['무야호'],cat=0,timeframe='2004-01-01 2026-08-15',geo='KR',gprop='')
print(p.interest_over_time().tail().to_string())
