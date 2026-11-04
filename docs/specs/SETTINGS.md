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
| | haptics | off/subtle/normal/strong | subtle (T-096, docs/design/HAPTICS.md) | 2 |
| | app_icon | three_d/flat (launcher icon "Pim im Wald", activity-alias; kept on reset) | three_d (T-103, docs/design/ICON.md) | 1 |
| | start_screen | chat/home | chat | 2 |
| Model | brain_model_id | string | lfm3-1.7b-qad-q4_0 | 1 |
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
| Privacy | app_lock | bool (T-056: BiometricPrompt, BIOMETRIC_WEAK or DEVICE_CREDENTIAL; switching on asks once; mirrored in SharedPreferences `hain_start` so a cold start is locked before DataStore is read) | false | 1 |
| | app_lock_delay | IMMEDIATE/ONE_MINUTE/FIVE_MINUTES (time in the background before it locks again; file picker / share sheet opened from Hain do not lock unless away > 5 min) | IMMEDIATE | 1 |
| | block_screenshots | bool (FLAG_SECURE; recents preview blank, also with app_lock on Android 13+) | false | 1 |
| | private_by_default | bool | false | 1 |
| | auto_delete_days | 0(off)/7/30/90 | 0 | 1 |
| Energy | battery_saver_threshold | 0/15/30 % | 15 | 2 |
| | show_energy_per_answer | bool | false | 2 |
| Updates (T-212) | update_check | off/daily (once a day at start, via NetworkGate purpose UPDATE) | off (asked once after the 3rd chat) | 1 (About Hain) |
| | update_channel | stable/nightly | stable | 1 (About Hain) |
| | update_asked | bool (one-time card answered) | false | – |
| | update_last_check | ms | 0 | – |
| Developer (T-211, hidden: tap version 7× in About Hain; only used while dev_enabled) | dev_enabled | bool | false | 3 |
| | dev_temperature, dev_top_k, dev_top_p, dev_min_p, dev_repeat_penalty | numbers | 0.1, 50, 1.0, 0.0, 1.1 | 3 |
| | dev_context | 512–8192 (a change reloads the model) | 8192 | 3 |
| | dev_max_tokens, dev_seed | int | 1024, -1 (random) | 3 |
| | dev_thinking, dev_thinking_budget, dev_raw_thinking | auto/on/off, int, bool | auto, 4096, false | 3 |
| | dev_system_prompt | text (empty = Hain's prompt) | "" | 3 |
| | dev_stats | bool (tokens/s + time to first token, copy debug info) | false | 3 |
| | dev_model_path, dev_model_name | own GGUF on the phone (empty = Hain brain) | "" | 3 |

Actions (not keys): export all, delete all, reset settings, manage packs, device check, view system prompt, licences.
News has **no** per-source settings (ADR-14) — only mode, languages, interval, Wi-Fi, own feed list.
