# Voron 2.4 + Rapido HF + Hyper PLA — OrcaSlicer

Готовый набор профилей для максимально быстрой печати Creality Hyper PLA.

## Состав

| Тип | Профиль |
|-----|---------|
| **Принтер** | Voron 2.4 250/300/350 Rapido HF 0.4 SS |
| **Филамент** | Creality Hyper PLA @Voron Rapido 0.4 SS |
| **Процесс** | 0.28mm Hyper Max Speed (максимум) |
| **Процесс** | 0.24mm Hyper Balanced (компромисс) |
| **Процесс** | 0.20mm Hyper Quality Fast (детали) |

## Установка

1. Откройте **OrcaSlicer** (нужен встроенный профиль **Voron** и **Creality Hyper PLA**).
2. **File → Import → Import Configs…**
3. Выберите файл **`Voron-2.4-Rapido-Hyper-PLA.orca_printer`**
4. Подтвердите импорт (при конфликте — «Overwrite» или «Rename»).

## Выбор профилей

После импорта в выпадающих списках:

- **Printer:** `Voron 2.4 300 Rapido HF 0.4 SS` (или 250/350 под ваш стол)
- **Filament:** `Creality Hyper PLA @Voron Rapido 0.4 SS`
- **Process:** `0.28mm Hyper Max Speed @Voron Rapido`

## Ключевые параметры

| Параметр | Значение |
|----------|----------|
| Температура сопла | 225 °C |
| Стол | 60 °C |
| Max volumetric speed | 32 mm³/s |
| Ретракт (Rapido DD) | 0.5 mm @ 45 mm/s |
| Pressure Advance | 0.035 (калибровать!) |
| Сопло | Hardened Steel 0.4 |
| Hotend | High Flow (Rapido HF) |

## Калибровка (обязательно)

1. **Pressure Advance** — `TUNING_TOWER` или тест в Klipper
2. **Input Shaper** — `SHAPER_CALIBRATE` на вашем Voron
3. **Flow ratio** — одностенный куб (цель 0.42–0.44 мм)
4. **Max volumetric** — башня 28→32 mm³/s, при недоэкструзии −2

## Если ghosting / ringing

- Уменьшите `Outer wall speed` до 150 mm/s
- Уменьшите `Outer wall acceleration` до 5000

## Требования

- OrcaSlicer 2.0+
- В системе должны быть профили **Voron 2.4** и **Hyper PLA @K1C-all** (идут с OrcaSlicer из коробки)
