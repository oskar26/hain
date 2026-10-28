import soundfile as sf, numpy as np
from pocket_tts import TTSModel
T={'english':"Hi, I'm Pim! I live right here on your phone and help you out, no internet needed. What shall we do today?",
'german':"Hallo, ich bin Pim! Ich wohne in deinem Handy und helfe dir, ganz ohne Internet. Was machen wir heute?",
'french':"Salut, je suis Pim ! J'habite dans ton téléphone et je t'aide, même sans internet. On fait quoi aujourd'hui ?",
'spanish':"¡Hola, soy Pim! Vivo en tu móvil y te ayudo, sin necesidad de internet. ¿Qué hacemos hoy?",
'italian':"Ciao, sono Pim! Vivo nel tuo telefono e ti aiuto, anche senza internet. Cosa facciamo oggi?",
'portuguese':"Olá, eu sou o Pim! Moro no teu telemóvel e ajudo-te, mesmo sem internet. O que vamos fazer hoje?",
'dutch':"Hoi, ik ben Pim! Ik woon in je telefoon en help je, helemaal zonder internet. Wat gaan we vandaag doen?"}
import sys
V=sys.argv[1]; L=sys.argv[2].split(',')
for lang,t in [(l,T[l]) for l in L]:
    m=TTSModel.load_model(language=lang); st=m.get_state_for_audio_prompt(V)
    a=np.asarray(m.generate_audio(st,t)).squeeze(); sf.write(f'pocket/{V}_{lang}.wav',a,m.sample_rate); print(lang, round(len(a)/24000,2), flush=True)
