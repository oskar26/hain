# SETTINGS.md — all settings (DataStore keys)

Level 1 = visible directly; level 2 = under "Erweitert / Advanced".

| Group | key | type | default | level |
|---|---|---|---|---|
| General | app_language | system/de/en | system | 1 |
| | answer_language | auto/de/en | auto | 1 |
| | theme | system/light/dark | system | 1 |
| | font_scale_extra | 1.0–1.4 | 1.0 | 1 |
| | readable_font | bool | false | 1 |
| | show_pim | bool | true | 1 |
| | pim_avatar_in_chat | bool | true | 2 |
| | reduce_motion | bool | false | 2 |
| | haptics | bool | true | 2 |
| | start_screen | chat/home | chat | 2 |
| Model | brain_model_id | string | lfm3-1.7b-q4km | 1 |
| | answer_mode | fast/thorough/auto | auto | 1 |
| | prompt_variant | full/short | full | 2 |
| | temperature, top_k, top_p, min_p, repeat_penalty | numbers | LLM.md §6 | 2 |
| | max_tokens | int | 1024 | 2 |
| | context_size | 4096…32768 | 8192 (4096 if RAM<6) | 2 |
| | threads | int | auto (big cores) | 2 |
| | gpu_offload | bool | false | 2 |
| | custom_system_prompt | text? | null | 2 |
| Personal | custom_about_me | text | "" | 1 |
| | custom_style | text | "" | 1 |
| Memory | memory_enabled | bool | true | 1 |
| | use_chat_history | bool | true | 1 |
| | profile_in_private_chats | bool | false | 2 |
| Internet | offline_mode | bool | false | 1 |
| | web_search | on/ask/off | on | 1 |
| | searxng_url | url? | null | 2 |
| | ddg_enabled | bool | true (marked experimental) | 2 |
| | search_region | de-de/us-en/auto | auto | 2 |
| | read_full_pages | bool | true | 2 |
| | network_log_detail | bool | false | 2 |
| News | news_mode | standard/standard_plus_own/own_only | standard | 1 |
| | news_languages | both/app_language_only | both | 1 |
| | news_refresh_minutes | 15/30/60 | 30 | 2 |
| | news_wifi_only | bool | false | 2 |
| Voice | tts_voice_de, tts_voice_en | model id/system | piper defaults | 1 |
| | tts_speed | 0.8–1.5 | 1.0 | 1 |
| | auto_read_aloud | bool | false | 1 |
| | stt_model | id | moonshine | 2 |
| | vad_silence_ms | int | 1200 | 2 |
| Images | image_edit_enabled | bool | true | 1 |
| | image_steps | 4–8 | 4 | 2 |
| Documents | ocr_quality | fast/accurate | fast | 1 |
| Phone | confirm_actions | bool | true | 1 |
| | screen_context | bool | false | 1 |
| Storage | downloads_wifi_only | bool | true | 1 |
| Privacy | app_lock | bool | false | 1 |
| | private_by_default | bool | false | 1 |
| | auto_delete_days | 0(off)/7/30/90 | 0 | 1 |
| Energy | battery_saver_threshold | 0/15/30 % | 15 | 2 |
| | show_energy_per_answer | bool | false | 2 |

Actions (not keys): export all, delete all, reset settings, manage packs, device check, view system prompt, licences.
News has **no** per-source settings (ADR-14) — only mode, languages, interval, Wi-Fi, own feed list.
