# RiseGram local API configuration

Do not commit your Telegram API credentials to Git.

In the project root, create or edit `local.properties` (this file is ignored by Git) and add:

```properties
RISEGRAM_API_ID=YOUR_API_ID
RISEGRAM_API_HASH=YOUR_API_HASH
```

Then sync Gradle and build the Android app.

The values are injected into the app at build time. They will not appear in tracked source files, but the API hash can still be extracted from a distributed APK, so this does not make it secret after release.

## Разработка Android-клиента на Windows x64

Нужны Git for Windows, включённая в UEFI виртуализация и компонент Windows «Платформа низкоуровневой оболочки Windows» (Windows Hypervisor Platform). После включения компонента Windows может запросить перезагрузку. Для большого дерева C++ лучше выбрать короткий путь к исходникам, например `C:\src\RiseGram`. Включите режим разработчика Windows, если Git должен создавать символьные ссылки в подмодулях.

В PowerShell из корня проекта выполните:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\Tools\Windows\RiseGram.ps1 -Setup
```

Скрипт подбирает JDK 17 или устанавливает Temurin 17 в профиль пользователя. Он настраивает SDK 36, Build Tools 36.0.0, NDK 27.2.12479018, CMake 3.22.1, ADB и Android Emulator, загружает подмодули и создаёт устройство `RiseGram_API_36` с Google APIs и ABI x86_64. Лицензии SDK принимаются в интерактивном режиме. Gradle 8.13 загружает проектный Wrapper. При первом запуске загрузка SDK, образа и нативная сборка могут занять значительное время.

Для авторизации нужны действующие `RISEGRAM_API_ID` и `RISEGRAM_API_HASH`. Скрипт использует переменные окружения или значения из локального `local.properties`. Если значений нет, он запрашивает их для текущего сеанса. Код входа вводится в приложении. Используйте тестовый аккаунт на сервере RiseGram.

В следующие сеансы запускайте ту же команду без `-Setup`. Первым действием после загрузки устройства скрипт проверяет TCP-соединение из эмулятора с адресом дата-центра, указанным в нативном клиенте. Затем он собирает `afatDebug`, устанавливает APK через `adb install -r` в выбранный AVD и открывает приложение. Gradle отслеживает входные файлы задач сборки и повторяет цикл при изменениях. Для одного цикла добавьте `-Once`. Завершить наблюдение можно через Ctrl+C. После изменения конфигурации Gradle или самого скрипта запустите команду заново.

Для проверки войдите по тестовому номеру, откройте профиль, поменяйте видимую строку интерфейса в исходниках и сохраните файл. После успешной сборки эмулятор должен открыть обновлённое приложение с прежней сессией. Предупреждение о TCP означает, что следует проверить DNS, VPN и доступ к порту сервера из эмулятора. Успех TCP-проверки подтверждает доступность порта, для проверки авторизации требуется вход в аккаунт.
