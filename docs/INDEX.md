# RP Time — указатель кода

Создано `python _family/tools/mapgen.py` 15.11.2026, версия 1.2.0-1.20.1-forge, коммит `4b2ae5a`.
**Руками не править**: пересоздаётся скриптом. Смысл, потоки работы и рецепты — в `docs/MAP.md`.
Разбор по шаблонам, без компиляции: необычно оформленная регистрация может отсутствовать в списках.

Всего: 22 файлов, 1556 строк Java.

## 1. Пакеты

| Пакет | Файлов | Строк |
|---|---:|---:|
| `faygolover.rptime` | 2 | 115 |
| `faygolover.rptime.api` | 1 | 47 |
| `faygolover.rptime.client` | 2 | 104 |
| `faygolover.rptime.command` | 1 | 186 |
| `faygolover.rptime.core` | 3 | 188 |
| `faygolover.rptime.integration` | 2 | 129 |
| `faygolover.rptime.mixin` | 5 | 149 |
| `faygolover.rptime.network` | 2 | 68 |
| `faygolover.rptime.server` | 4 | 570 |

## 2. Классы

### `faygolover.rptime`
- `RpTime`, 49 — RP Time — день и ночь своей длины, скорость и пауза времени, ускоренная ночь во сне, растения по игровому времени, часы на табличках (план — docs/00_plan.md).
- `TimeConfig`, 66 — Конфиг RP Time: config/rptime-common.toml.

### `faygolover.rptime.api`
- `RpTimeAPI`, 47 — Для других модов (сервер).

### `faygolover.rptime.client`
- `ClientTime`, 47 — Ход времени на клиенте: те же сутки, что на сервере (приходят CyclePacket).
- `SignClock`, 57 — Часы на табличках (план §8): метки в тексте таблички заменяются при отрисовке на клиенте — серверу ни работы, ни трафика.

### `faygolover.rptime.command`
- `TimeCommands`, 186 — Подкоманды к ванильной /time (её set|add|query не трогаем): /rptime info — который час, день, длины, скорость; /rptime length day [night ], /rptime length night , /rptime length reset; /rptime speed |reset — временно быстрее или м…

### `faygolover.rptime.core`
- `Clock`, 32 — Часы по времени суток: 0 тиков = 6:00, 1000 тиков = час (как у часов PCD и SovietEra).
- `DayCycle` (record), 113 — Ход времени суток без Minecraft: сколько тиков времени суток проходит за реальные тики.
- `Durations`, 43 — Длительности в командах — как в /rptime RP Core: 2h, 90m, 1h30m, 45s, просто число — часы.

### `faygolover.rptime.integration`
- `CoreBridge`, 54 — Вызовы RP Core.
- `CoreLink`, 75 — Мягкая связь с RP Core.

### `faygolover.rptime.mixin`
- `ClientLevelTimeMixin`, 18 — Ход времени суток на клиенте — так же, как на сервере (ClientTime).
- `CropBlockAccess` (interface), 18 — Ванильная скорость роста культуры (грядка, вода, соседи) — для расчёта ускорения роста во сне.
- `SaplingBlockMixin`, 25 — Саженцы растут по игровому времени (GrowthService).
- `ServerLevelTimeMixin`, 37 — Время суток на сервере: ход по длинам дня и ночи (TimeService); ванильный мгновенный пропуск ночи и его строка «спят N из M» выключены — вместо них ускорение сна (SleepService).
- `SignTextMixin`, 51 — Часы на табличках (SignClock).

### `faygolover.rptime.network`
- `CyclePacket` (record), 32 — Сервер → клиент: как сейчас идёт время (длины, множитель, пауза, сон).
- `TimeNetwork`, 36 — Канал rptime:main.

### `faygolover.rptime.server`
- `GrowthService`, 75 — Растения растут по игровому времени (план §7): пшеница в среднем за wheat_days игровых суток, остальные — в той же пропорции к ванилле.
- `SleepService`, 208 — Сон вместо ванильного пропуска ночи (план §5).
- `TimeService`, 187 — Ход времени на сервере.
- `TimeState`, 100 — Что ведущий поменял командами — хранится в мире (data/rptime.dat), переживает перезапуск.

Классов без описания: 0 из 22.

## 3. Подписки на события

| Событие | Кто слушает |
|---|---|
| `BlockEvent.CropGrowEvent.Pre` | `GrowthService.onCropGrow` |
| `ClientPlayerNetworkEvent.LoggingOut` | `ClientTime.onLoggingOut` |
| `FMLCommonSetupEvent` | `RpTime.commonSetup` |
| `ModConfigEvent.Reloading` | `TimeService.onConfigReload` |
| `PlayerEvent.PlayerLoggedInEvent` | `TimeService.onLogin` |
| `RegisterCommandsEvent` | `TimeCommands.register` |
| `ServerStartedEvent` | `TimeService.onServerStarted` |
| `ServerStoppedEvent` | `SleepService.onServerStopped`, `TimeService.onServerStopped` |
| `TickEvent.ServerTickEvent` | `SleepService.onServerTick` |

## 4. Сеть

Канал: `faygolover/rptime/network/TimeNetwork.java`, имя `main`, версия протокола `не объявлена константой`.
- `CyclePacket` (направление не указано в регистрации)

Классы с обработчиками пакетов (1): `CyclePacket`.

## 5. Команды

- `faygolover/rptime/command/TimeCommands.java`: `rptime` `day` `night` `reset`

## 6. Данные

- SavedData: `TimeState`, файл "rptime"

## 7. Конфиги

- common: `TimeConfig`, вызовов define 11

## 8. Что берёт из RP Core и что ему даёт

Методы `RpCoreAPI` (сколько вызовов): `registerTimeSource` 2, `setTimeRunning` 1, `timeControlEnabled` 1, `timeRunning` 1.

События Core: `RpSleepCountEvent`, `RpTimeSkipEvent`, `RpTimeSkipOfflineEvent`.

## 9. Связи с другими модами

Проверки `isLoaded`: `rpcore` (CoreLink).

Пакеты других модов семейства в коде: `rpcore` — 1 файлов.

Миксины (5):
- `ServerLevelTimeMixin` (mixins) → ServerLevel.class
- `SaplingBlockMixin` (mixins) → SaplingBlock.class
- `CropBlockAccess` (mixins) → CropBlock.class
- `ClientLevelTimeMixin` (client) → ClientLevel.class
- `SignTextMixin` (client) → SignText.class

## 10. Зарегистрированные id

Нет или регистрируются не строковыми литералами.
