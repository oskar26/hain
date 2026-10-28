import sys, torch, numpy as np, soundfile as sf
sys.path.insert(0,'/workspace/work/voice/kikiri-tts/scripts')
from pathlib import Path
from test_inference import convert_checkpoint
from kokoro import KModel, KPipeline
W='/workspace/work/voice/kerstin_kokoro.pth'
if not Path(W).exists(): convert_checkpoint('/workspace/work/voice/kerstin_stage2.pth', W)
cfg='/workspace/work/voice/kikiri-tts/training/config.json'
m=KModel(repo_id="hexgrad/Kokoro-82M", config=cfg if Path(cfg).exists() else None, model=W).eval()
p=KPipeline(lang_code="d", repo_id="hexgrad/Kokoro-82M", model=m)
v=torch.load('/workspace/work/voice/df_kerstin.pt', map_location='cpu', weights_only=True)
lines={'pim_de_hallo':"Hallo, ich bin Pim! Ich wohne in deinem Handy und helfe dir, ganz ohne Internet. Was machen wir heute?",
'pim_de_denken':"Hmm, lass mich kurz nachdenken. Okay, ich hab's! Morgen wird es sonnig, also nimm lieber die Sonnenbrille mit.",
'pim_de_fehler':"Oh nein, das hat leider nicht geklappt. Sollen wir es noch einmal versuchen?"}
Path('/workspace/work/voice/kokoro_out').mkdir(exist_ok=True)
for k,t in lines.items():
    a=np.concatenate([np.asarray(au) for _,_,au in p(t,voice=v,speed=1.05)])
    sf.write(f'/workspace/work/voice/kokoro_out/{k}_raw.wav',a,24000); print(k,len(a)/24000)
