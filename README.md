# Помощник замеров КП

Android MVP для локального ввода замеров колесных пар локомотивов.

## Сборка

Откройте папку `ZameryKp` в Android Studio с установленным Android SDK. Проект использует Kotlin, Jetpack Compose, Room и minSdk 26.

Если на машине установлен Gradle/Android SDK, проверки запускаются командами:

```powershell
gradle test
gradle assembleDebug
```

## Обмен с ПК

Приложение экспортирует JSON `formatVersion = 1`. В текущую программу `КП.py` добавлена кнопка `ИМПОРТ JSON` во вкладке архива: она добавляет новый локомотив при необходимости и записывает замер в `archive_data`.
