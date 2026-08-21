# ClickGUI Анимация — Инструкция

## Что изменилось

### 1. Анимация открытия ClickGui (RightShift)
**Файл: `ClickGuiFrame.java`**

Теперь при нажатии RightShift:
- Все панели **плавно вылетают снизу** (не появляются мгновенно)
- Есть **stagger-эффект** — панели появляются одна за другой с задержкой 40ms
- Есть **эффект подпрыгивания** (bounce) при вылете
- ThemeEditor и SearchField тоже **выезжают снизу**
- При закрытии (повторный RightShift) — всё **улетает обратно** и только потом закрывается

**Где настраивать тайминги:**
```java
// ClickGuiFrame.java, строка ~45
private final Animation globalOpenAnim = new Animation(Easing.BACK_OUT, 450);
//                                                    ^^^^^^^^^ ^^^^
//                                                    easing    duration (ms)

// Stagger задержка между панелями, строка ~185
float panelDelay = i * 0.04f;  // 40ms между панелями

// Дистанция вылета, строка ~180
float offscreen = windowHeight / 2f + panelHeight;
//                ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
//                чем больше — тем дальше вылет
```

### 2. Анимация раскрытия настроек модуля (ПКМ)
**Файл: `ModuleComponent.java`**

Теперь при ПКМ по модулю (например AutoTotem):
- Карточка **плавно увеличивается в высоту** (не рывком)
- Настройки появляются с **stagger-эффектом** — каждая с задержкой 60ms
- Каждая настройка **плавно проявляется и немного масштабируется**
- При закрытии — настройки **исчезают в обратном порядке**
- Иконка `{/}` **меняет прозрачность** при открытии

**Где настраивать тайминги:**
```java
// ModuleComponent.java

// Скорость раскрытия карточки
private final Animation animation = new Animation(Easing.QUINTIC_OUT, 320);

// Скорость изменения высоты
private final Animation heightAnim = new Animation(Easing.QUINTIC_OUT, 350);

// Stagger задержка между настройками (в render())
float staggerDelay = i * 0.06f;  // 60ms между настройками
```

### 3. Анимация ThemeEditor
**Файл: `ThemeEditor.java`**

- При открытии ClickGui — **выезжает снизу + масштабируется** (0.8 → 1.0)
- Использует `Easing.BACK_OUT` для эффекта "перелёта"

### 4. Анимация SearchField
**Файл: `component/SearchField.java`**

- При открытии ClickGui — **выезжает снизу на 20 пикселей**
- Плавное появление с прозрачностью
- Обводка при фокусе с анимацией

## Как установить

1. Замени файлы в своём проекте:
   - `zov/alphadlc/ui/ClickGuiFrame.java`
   - `zov/alphadlc/ui/Panel.java`
   - `zov/alphadlc/ui/ModuleComponent.java`
   - `zov/alphadlc/ui/ThemeEditor.java`
   - `zov/alphadlc/ui/component/SearchField.java`

2. Пересобери проект

3. Готово!

## Настройка под себя

| Параметр | Файл | Значение | Описание |
|----------|------|----------|----------|
| Скорость открытия GUI | ClickGuiFrame | `450` ms | Чем больше — тем медленнее |
| Easing открытия GUI | ClickGuiFrame | `BACK_OUT` | Можно: `QUINTIC_OUT`, `BACK_OUT`, `BOUNCE_OUT` |
| Задержка панелей | ClickGuiFrame | `0.04f` | Stagger между панелями |
| Дистанция вылета | ClickGuiFrame | `windowHeight/2 + panelHeight` | Как далеко вылетают |
| Скорость раскрытия | ModuleComponent | `320` ms | Скорость открытия настроек |
| Stagger настроек | ModuleComponent | `0.06f` | Задержка между настройками |
| Скорость hover | ModuleComponent | `300` ms | Скорость подсветки |
| Скорость toggle | ModuleComponent | `400` ms | Скорость переключателя |

## Easing типы (что выбрать)

- `QUINTIC_OUT` — плавное замедление (классика)
- `BACK_OUT` — немного "перелетает" и возвращается (эффект пружины)
- `BOUNCE_OUT` — отскакивает несколько раз
- `CUBIC_OUT` — быстрое замедление
- `EXPO_OUT` — очень быстрое начало, медленный конец
