# RP Time

Мод Minecraft Forge 1.20.1 (Java 17): день и ночь своей длины, скорость и пауза времени, дальше — плавный сон,
медленный рост растений, часы на табличках. Автор — faygoLover (Alex). **С автором общаемся по-русски.**

Мод `rptime`, «RP Time», пакет `faygolover.rptime`, MIT, языки ru и en. Часть RP-семейства, **RP Core не требует**;
связь с Core — мягкая (этап 2). Заменяет часы датапака PCD (`S5 Packs/datapacks/PCD`, функции `clock`).

## Состояние
- План и все решения — `docs/00_plan.md` (этапы §11, решения §12).
- **20.10.2026, 0.1.0 — этап 1 (ядро):** ход времени по длинам дня и ночи, `/time info|length|speed|pause|resume`,
  настройки в мире, клиент считает ход сам.
- **20.10.2026, 1.0.0 — этапы 2–5:** сон с ускорением (спят все), связь с RP Core 0.7.2 (пауза = `/rptime`, кто спит —
  `RpSleepCountEvent`, прокрутка ран и голода во сне), растения по игровому времени (пшеница — 7 игровых суток),
  часы на табличках (`{время}`, `{день}`), `RpTimeAPI`. Часы из PCD убраны (PCD 1.4.0), мод в сборке `S5 Packs`.
  Подробно — `docs/00_plan.md` §14. В игре не проверено.

## Сборка
RP Core для компиляции — jar в `libs/` (в git не идёт): `rpcore-<rpcore_version>.jar` из `RP Core/build/libs`.
Запуск разработки с Core — `-Pwith_core=true`.
JDK 17: `JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-17.0.14.7-hotspot"`, затем `gradlew.bat build` → `build/libs`.
Клиент разработки — `gradlew.bat runClient`.

## Устройство
- `core/` — без Minecraft: `DayCycle` (сколько времени суток проходит за реальные тики; стык дня и ночи без скачка),
  `Clock` (0 тиков = 6:00, номер дня), `Durations` (`2h`, `90m`, `1h30m`, как в `/rptime` Core).
- `server/TimeService` — ход на сервере, дробный накопитель; время суток ведёт только верхний мир.
  `TimeState` — что задано командами (`data/rptime.dat`), иначе — конфиг `config/rptime-common.toml`.
- `client/ClientTime` — тот же ход на клиенте; сутки приходят `CyclePacket` при входе и при смене, не каждый тик.
- `mixin/` — `ServerLevel.tickTime` и `ClientLevel.tickTime`: вместо «+1 к времени суток» — наш расчёт.
  Только при включённом `doDaylightCycle`: выключенное правило = пауза, как в ванилле.
- `server/SleepService` — ускорение ночи во сне, исключения, сообщения ГМам; `server/GrowthService` — рост растений.
- `integration/CoreLink` (+ `CoreBridge`, грузится только с Core) — пауза, кто спит, прокрутка во сне.
- `client/SignClock` + `mixin/SignTextMixin` — часы на табличках.
- `command/TimeCommands` — ветки к ванильной `/time` (её `set|add|query` не трогаем):
  `info|length|speed|pause|resume|sleep|growth|exclude`.

## Правила работы
Как в остальных RP-модах: делаю только согласованное (план), сам тесты и игру не запускаю — собираю, даю автору план
проверки, он смотрит в игре. Комментарии и строки конфига — по-русски. Коммиты — только по просьбе автора.
