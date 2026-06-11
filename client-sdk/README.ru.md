# Paspo ID Android SDK

[English](README.md) | Русский

Paspo ID SDK позволяет пользователям проходить аутентификацию в вашем приложении через аккаунт Paspo - аналогично «Войти через Google».

## Содержание

- [Обзор](#обзор)
- [Требования](#требования)
- [Установка](#установка)
- [Быстрый старт: готовая кнопка](#быстрый-старт-готовая-кнопка)
- [Собственная интеграция](#собственная-интеграция)
  - [Compose](#compose)
  - [Activity](#activity)
  - [Fragment](#fragment)
- [Вход, инициированный из Paspo](#вход-инициированный-из-paspo)
- [Проверка доступности](#проверка-доступности)
- [Поток аутентификации](#поток-аутентификации)
- [Справочник](#справочник)
  - [PaspoScope](#pasposcope)
  - [PaspoAuthResult](#paspoauthresult)
  - [Ошибки](#ошибки)
- [ProGuard / R8](#proguard--r8)
- [Безопасность](#безопасность)
- [Частые вопросы](#частые-вопросы)
- [Поддержка](#поддержка)

## Обзор

Интеграция сводится к одному вызову `authenticate(scope, nonce)`, который возвращает одноразовый код авторизации. Приложение передаёт этот код на свой сервер, а тот обменивает его у Paspo на данные пользователя. Со стороны пользователя поток выглядит так: нажатие «Войти через Paspo ID», подтверждение на экране согласия Paspo и возврат в приложение.

Весь обмен между приложением и Paspo защищён end-to-end шифрованием (ECDH P-256 + AES-256-GCM, эфемерные ключи хранятся только в памяти) и выполняется SDK; криптографические операции с вашей стороны не требуются.

Две обязанности остаются на вашем сервере и не могут быть перенесены на клиент, поскольку являются основой безопасности потока: генерация `nonce` и обмен кода авторизации на данные пользователя.

Быстрее всего интегрировать Paspo через [готовую кнопку](#быстрый-старт-готовая-кнопка). Для полного контроля над внешним видом используйте [headless API](#собственная-интеграция).

Примеры ниже ссылаются на следующие функции на стороне приложения:

- `backend.requestSsoNonce(): String` - suspend-вызов, запрашивающий у вашего сервера свежий одноразовый nonce.
- `backend.signInWithPaspo(authCode: String)` - отправляет код авторизации на ваш сервер для завершения входа.
- `showError(error: PaspoClientError)` - ваше отображение ошибки.
- `onPaspoResult(result: PaspoAuthResult)` - единый обработчик результата, определённый один раз:

```kotlin
private fun onPaspoResult(result: PaspoAuthResult) {
    when (result) {
        is PaspoAuthResult.Success -> backend.signInWithPaspo(result.authCode)
        is PaspoAuthResult.Failure -> showError(result.error)
        PaspoAuthResult.Cancelled,
        PaspoAuthResult.NotInstalled -> Unit
    }
}
```

## Требования

| | |
|---|---|
| minSdk | 23 (Android 6.0) |
| compileSdk | 36 |
| Kotlin | 2.x |
| Корутины | требуются; весь API состоит из `suspend`-функций |

SDK интегрируется в приложения с minSdk 23, однако самому приложению Paspo требуется Android 7.0 (API 24). На устройствах с API 23 Paspo не устанавливается, поэтому `authenticate` возвращает `NotInstalled`; это штатный исход, который следует обработать. Кнопку можно скрыть заранее через `checkInstallation()`.

## Установка

Добавьте репозиторий и зависимость. Дополнительная настройка не требуется: декларация `<queries>` и consumer-правила R8 поставляются внутри AAR.

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        maven(url = "https://cdn.paspo.id/android")
    }
}
```

```kotlin
// app/build.gradle.kts
dependencies {
    implementation("id.paspo:sdk:<version>")          // ядро, headless API

    // опционально - готовая кнопка (одна из; ядро подтянется само)
    implementation("id.paspo:ui-compose:<version>")   // Jetpack Compose
    implementation("id.paspo:ui:<version>")           // классические View
}
```

## Быстрый старт: готовая кнопка

![Кнопка входа Paspo ID - темы brand, light и dark, полная и icon-only](buttons.svg)

Готовая кнопка запускает весь поток по нажатию и блокирует повторные нажатия, пока выполняется запрос. Ей требуются две вещи: поставщик nonce и обработчик результата.

```kotlin
PaspoSignInButton(
    nonceProvider = backend::requestSsoNonce,
    onResult = ::onPaspoResult,
)
```

По умолчанию кнопка залита фирменным цветом Paspo. Её можно стилизовать под ваше приложение:

```kotlin
PaspoSignInButton(
    nonceProvider = backend::requestSsoNonce,
    onResult = ::onPaspoResult,
    theme = PaspoButtonTheme.LIGHT,   // BRAND (по умолчанию), LIGHT, DARK, NEUTRAL
    cornerRadius = 12.dp,             // по умолчанию рисуется pill
)
```

Для классических View объявите кнопку в разметке:

```xml
<paspo.id.ssoprovider.ui.PaspoSignInButton
    android:id="@+id/paspo_button"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    app:paspoTheme="light"
    app:paspoCornerRadius="12dp" />
```

и привяжите поток один раз:

```kotlin
binding.paspoButton.setAuthHandler(
    activity = this,
    scope = PaspoScope.PHONES,
    nonceProvider = backend::requestSsoNonce,
    onResult = ::onPaspoResult,
)
```

Кнопка рендерится независимо от темы хоста, чтобы выглядеть одинаково во всех приложениях. Если `nonceProvider` бросит исключение, результатом будет `Failure(SERVICE_UNAVAILABLE)`. Compose-модуль дополнительно предоставляет render-only `PaspoSignInButtonContent(colors = PaspoButtonColors(...))`, который сообщает о кликах, не запуская поток, - для полного контроля над отрисовкой.

| Параметр | Значения | По умолчанию |
|---|---|---|
| `theme` / `app:paspoTheme` | `BRAND`, `LIGHT`, `DARK`, `NEUTRAL` | `BRAND` |
| `cornerRadius` / `app:paspoCornerRadius` | любая величина; большое значение даёт pill | pill |
| `iconOnly` / `app:paspoIconOnly` | `true` / `false` (только логотип) | `false` |

## Собственная интеграция

Чтобы использовать свой UI, вызывайте SDK напрямую. Создайте `PaspoID` из host-Activity и вызовите `authenticate` из корутины. Метод не бросает исключений; любой исход, включая отмену и отсутствие приложения Paspo, возвращается как `PaspoAuthResult`.

Действуют два ограничения:

- Одновременно может выполняться только один `authenticate`; новый вызов сбрасывает предыдущий.
- `PaspoID` держит ссылку на Activity и должен оставаться в UI-слое. Не храните его в ViewModel или синглтоне; передавайте во ViewModel только результат.

### Compose

```kotlin
@Composable
fun LoginScreen() {
    val activity = LocalActivity.current as? ComponentActivity ?: return
    val paspoId = remember(activity) { PaspoID(activity) }
    val scope = rememberCoroutineScope()

    Button(
        onClick = {
            scope.launch {
                onPaspoResult(paspoId.authenticate(PaspoScope.PHONES, backend.requestSsoNonce()))
            }
        },
    ) {
        Text("Войти через Paspo")
    }
}
```

`remember(activity)` хранит единственный `PaspoID` между рекомпозициями; `rememberCoroutineScope` отменяет вызов, если пользователь уходит с экрана.

### Activity

```kotlin
class LoginActivity : AppCompatActivity() {

    private val paspoId = PaspoID(this)

    private fun signInWithPaspo() = lifecycleScope.launch {
        onPaspoResult(paspoId.authenticate(PaspoScope.PHONES, backend.requestSsoNonce()))
    }
}
```

### Fragment

```kotlin
class LoginFragment : Fragment(R.layout.fragment_login) {

    // lazy: на момент создания фрагмента Activity ещё не присоединена
    private val paspoId by lazy { PaspoID(requireActivity() as ComponentActivity) }

    private fun signInWithPaspo() = viewLifecycleOwner.lifecycleScope.launch {
        onPaspoResult(paspoId.authenticate(PaspoScope.PHONES, backend.requestSsoNonce()))
    }
}
```

Запускайте на `viewLifecycleOwner.lifecycleScope`, а не на скоупе самого фрагмента, чтобы колбэк не отработал на уничтоженной view.

> Если Activity пересоздаётся, пока открыт экран Paspo (поворот, смерть процесса), результат теряется. Обработайте это как отмену и дайте пользователю повторить; проще всего зафиксировать ориентацию экрана входа.

## Вход, инициированный из Paspo

Пользователь может начать вход и изнутри Paspo. В этом случае Paspo запускает ваше приложение с action `paspo.id.ssoprovider.action.AUTHENTICATE` (константа `PaspoID.ACTION_SIGN_IN`). Поддержка этой точки входа опциональна.

Объявите intent-filter на Activity входа:

```xml
<activity
    android:name=".LoginActivity"
    android:exported="true">
    <intent-filter>
        <action android:name="paspo.id.ssoprovider.action.AUTHENTICATE" />
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
</activity>
```

и запускайте поток при получении action:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (intent.action == PaspoID.ACTION_SIGN_IN) {
        signInWithPaspo()
    }
}
```

Intent не несёт дополнительных данных; это лишь сигнал о намерении пользователя войти.

## Проверка доступности

Чтобы адаптировать UI до любого взаимодействия:

```kotlin
if (paspoId.checkInstallation()) {
    // Paspo установлен
}
```

Вызов опционален: `authenticate` выполняет проверку сам и, если Paspo не установлен, открывает Play Store и возвращает `NotInstalled`.

`paspoId.cleanup()` очищает эфемерные ключи. SDK вызывает его автоматически при завершении и при ошибках; вручную он нужен только если вы сами прерываете поток, например когда пользователь уходит с экрана входа.

## Поток аутентификации

```
Ваш сервер          Приложение                   Paspo (приложение)        Сервер Paspo
    |                    |                            |                       |
    |<--- запрос nonce --|                            |                       |
    |---- nonce -------->|                            |                       |
    |                    |-- authenticate(scope,      |                       |
    |                    |   nonce) ----------------->|                       |
    |                    |                            |-- проверка приложения>|
    |                    |                            |   экран согласия      |
    |                    |<-- зашифрованный ответ ----|                       |
    |<-- код авторизации-|                            |                       |
    |--- обмен кода на данные пользователя (server-to-server) --------------->|
    |<-- данные профиля --------------------------------------------------------|
```

1. Сервер генерирует `nonce` и передаёт его приложению.
2. Приложение вызывает `authenticate(scope, nonce)`. SDK генерирует эфемерную пару ключей ECDH и открывает Paspo.
3. Paspo проверяет подпись и package приложения и показывает экран согласия с запрошенным scope.
4. После подтверждения Paspo возвращает зашифрованный ответ; SDK расшифровывает его и возвращает `Success(authCode)`.
5. Приложение отправляет код на свой сервер, тот обменивает его у Paspo на данные пользователя (см. серверную документацию Paspo).

Если Paspo не установлен, SDK сохраняет `nonce`, открывает Play Store и возвращает `NotInstalled`. После установки Paspo и повторного вызова `authenticate` сохранённый nonce передаётся как referral для аналитики установки.

## Справочник

### PaspoScope

Данные профиля, запрашиваемые у пользователя:

| Значение | Что получает ваш сервер |
|---|---|
| `PHONES` | Телефонные номера |
| `EMAILS` | E-mail адреса |
| `NATIONAL_ID` | Номер национального удостоверения |
| `PASPO_ID` | Постоянный идентификатор пользователя в Paspo |

### PaspoAuthResult

Sealed-тип, покрывающий все исходы `authenticate`:

| Результат | Поля | Действие |
|---|---|---|
| `Success` | `authCode: String` | Отправить `authCode` на ваш сервер |
| `Cancelled` | - | Пользователь закрыл экран согласия; обычно UI ошибки не нужен |
| `NotInstalled` | - | Paspo не установлен; SDK открыл Play Store |
| `Failure` | `error: PaspoClientError`, `message: String?` | Показать ошибку. `message` предназначен для логов, не для пользователя |

`Cancelled` и `NotInstalled` возвращаются отдельными ветками и никогда не приходят внутри `Failure`.

### Ошибки

| Код | Константа | Значение |
|---|---|---|
| 100 | `UNKNOWN` | Неизвестная ошибка |
| 101 | `INVALID_REQUEST` | Некорректный запрос или повреждённые данные |
| 102 | `SERVICE_UNAVAILABLE` | Paspo временно недоступен |
| 103 | `CANCELLED` | Пользователь отменил операцию |
| 110 | `SESSION_EXPIRED` | Сессия истекла; начните заново с новым nonce |
| 111 | `PACKAGE_VERIFICATION_FAILED` | Приложение не прошло проверку подписи/package в Paspo. Убедитесь, что оба зарегистрированы при подключении |
| 120 | `ACCESS_DENIED` | Пользователь отказал в доступе |
| 121 | `IDENTITY_NOT_VERIFIED` | Личность пользователя в Paspo не подтверждена |
| 122 | `ATTEMPTS_EXCEEDED` | Превышено число попыток |
| 123 | `ACTION_NOT_ALLOWED` | Недоступно для этого пользователя |
| 130 | `AUTH_METHOD_UNAVAILABLE` | Метод подтверждения временно недоступен |
| 131 | `UNSUPPORTED_AUTH_METHOD` | Метод подтверждения не поддерживается |
| 140 | `PASPO_NOT_INSTALLED` | Paspo не установлен |
| 141 | `CRYPTO_ERROR` | Ошибка шифрования или расшифровки; начните заново |

SDK очищает криптографическое состояние при любой ошибке, поэтому дополнительный `cleanup()` после неё не требуется.

## ProGuard / R8

Действий не требуется. Consumer-правила поставляются внутри AAR и применяются автоматически при сборке приложения с `minifyEnabled = true`.

## Безопасность

- Ключи эфемерные: новая пара ECDH генерируется на каждый вызов `authenticate`, хранится только в памяти процесса и уничтожается после расшифровки ответа.
- Генерируйте `nonce` на сервере и там же проверяйте его при обмене кода; это защита от replay-атак.
- `authCode` одноразовый и сам по себе не содержит персональных данных; данные пользователя передаются только по вашему защищённому server-to-server каналу.
- Не логируйте `nonce` и код авторизации.
- Объявления, помеченные `@PaspoInternalApi` (крипто-слой и wire-модели), не являются публичным API; компилятор запрещает их использование, и они могут меняться в любом релизе.

## Частые вопросы

**Нужен ли приложению интернет в момент вызова?**
Самому SDK - нет; он общается с Paspo через Intent. Paspo и ваш сервер должны быть онлайн.

**Что произойдёт, если пользователь удалит Paspo между входами?**
`authenticate` это обнаружит, откроет Play Store и вернёт `NotInstalled`.

**Можно ли использовать SDK из Compose?**
Да; см. первый пример в разделе [Собственная интеграция](#compose) или используйте готовую кнопку.

## Поддержка

По вопросам подключения (регистрация package и подписи приложения, URL репозитория, серверный API обмена кода): [pingocean.com](https://pingocean.com).
