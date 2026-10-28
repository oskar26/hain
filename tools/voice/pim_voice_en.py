import numpy as np, soundfile as sf, torch
from kokoro import KPipeline
for lang,voice in [('a','af_heart'),('b','bf_emma')]:
    p=KPipeline(lang_code=lang, repo_id="hexgrad/Kokoro-82M")
    for k,t in {'hello':"Hi, I'm Pim! I live right here on your phone and help you, no internet needed. What shall we do today?",
                'thinking':"Hmm, let me think for a second. Okay, got it! Tomorrow looks sunny, so maybe bring your sunglasses."}.items():
        a=np.concatenate([np.asarray(au) for _,_,au in p(t,voice=voice,speed=1.05)])
        sf.write(f'kokoro_out/pim_en_{k}_{voice}_raw.wav',a,24000); print(voice,k,len(a)/24000)
