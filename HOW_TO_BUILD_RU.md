# Как собрать DnsVisuals (для тех, у кого gradlew барахлит)

У тебя на ПК глючит локальная сборка? Собери мод **в облаке GitHub** — бесплатно, ничего ставить не нужно. Уже всё настроено.

## Вариант 1 — GitHub (рекомендую) ☁️

1. Создай бесплатный аккаунт на https://github.com (если ещё нет).
2. Нажми **New repository** -> придумай имя (например `DnsVisuals`) -> **Create**.
3. На странице репозитория нажми **Add file -> Upload files** и перетащи туда
   ВСЁ содержимое папки `DnsVisuals` (включая скрытую папку `.github`).
   - Если `.github` не перетаскивается, создай файл вручную:
     **Add file -> Create new file**, в имени впиши
     `.github/workflows/build.yml` и вставь содержимое из этого репозитория.
4. Нажми **Commit changes**.
5. Открой вкладку **Actions** -> сборка `Build DnsVisuals` запустится сама
   (или нажми **Run workflow**). Подожди ~2-4 минуты.
6. Когда появится зелёная галочка, открой этот запуск и внизу в разделе
   **Artifacts** скачай **dnsvisuals-jar**. Внутри — `dnsvisuals-1.0.0.jar`.

Готово! Это и есть твой скомпилированный мод.

## Вариант 2 — починить локально 🛠️

Чаще всего "gradlew плохо работает" из-за:
- **Не той Java.** Нужен **JDK 17** (подойдёт 17-21). Проверь: `java -version`.
  Скачать Temurin 17: https://adoptium.net/temurin/releases/?version=17
- **Нет файлов wrapper'а.** В архиве их нет, поэтому `gradlew` нечем запускать.
  Установи Gradle (https://gradle.org/install/), затем в папке проекта выполни:
  ```
  gradle wrapper
  gradle build
  ```
  Готовый jar появится в `build/libs/dnsvisuals-1.0.0.jar`.
- **Слабый интернет/прокси** — первая сборка качает 200+ МБ; запусти ещё раз,
  Gradle докачает с того места, где оборвалось.

## Вариант 3 — IntelliJ IDEA 💡

1. Установи IntelliJ IDEA Community (бесплатно).
2. **Open** -> выбери папку проекта. IDEA сама подтянет Gradle и зависимости.
3. Убедись, что Project SDK = **17** (File -> Project Structure -> SDK).
4. Справа открой панель **Gradle** -> `Tasks -> build -> build`. Двойной клик.
5. Jar появится в `build/libs/`.

## Куда ставить готовый мод

1. Установи **Fabric Loader** для своей версии Minecraft (https://fabricmc.net/use/).
2. Скачай **Fabric API** для той же версии
   (https://modrinth.com/mod/fabric-api) и положи в `.minecraft/mods`.
3. Положи туда же `dnsvisuals-1.0.0.jar`.
4. Запусти Minecraft с профилем Fabric. В игре жми **Right Shift** -> откроется меню.

> Важно: версии должны совпадать. По умолчанию проект собран под **1.20.4**.
> Для другой версии поменяй значения в `gradle.properties`
> (`minecraft_version`, `yarn_mappings`, `loader_version`, `fabric_version`)
> с сайта https://fabricmc.net/develop/ .
