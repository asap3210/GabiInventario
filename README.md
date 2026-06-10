# Inventario (Android)

App Android de gestión de inventario por escaneo de códigos. Lee códigos de barras y QR
con la cámara, distingue entre dos tipos de código —**salas** y **objetos**—, los
relaciona ("este objeto está en esta sala"), guarda un *timestamp* en cada registro y
avisa cuando un objeto ya estaba en la base de datos.

No requiere Google Play Services (usa la librería ZXing embebida) y guarda todo en una
base de datos local SQLite del propio teléfono.

## Cómo funciona

La app trabaja con una **"sala activa"**. El criterio para distinguir los dos tipos de
código es un **prefijo configurable** (por defecto `SALA`), editable en la pantalla:

- Si el código escaneado **empieza por el prefijo** (p. ej. `SALA-A1`, `SALA-ALMACEN`), se
  interpreta como una **sala** y pasa a ser la sala activa.
- Cualquier otro código se interpreta como un **objeto**: se asocia a la sala activa, se le
  pone la fecha y hora del escaneo y se guarda en la tabla.

Flujo típico de uso:

1. Pulsa **ESCANEAR CÓDIGO** y escanea el código de una sala (debe empezar por el prefijo).
2. Escanea los objetos de esa sala, uno tras otro. Cada uno queda registrado con su sala y
   su timestamp.
3. Cambia de sala escaneando otro código de sala, y continúa.

Cuando escaneas un objeto que **ya existe** en la tabla, la app lo detecta y muestra su sala
actual, el primer y último escaneo y cuántas veces se ha escaneado, ofreciendo moverlo a la
sala activa o mantenerlo como estaba.

Además puedes **Exportar CSV** (para compartir o abrir en Excel/Sheets) y **Borrar todo**.

La tabla `inventario` guarda: `object_code`, `room_code`, `first_seen`, `last_seen` y
`scan_count`. El código de objeto es único, que es lo que permite detectar duplicados.

## Estructura del proyecto

```
inventario-app/
├─ app/
│  ├─ build.gradle                 Configuración del módulo (SDK, dependencias)
│  └─ src/main/
│     ├─ AndroidManifest.xml       Permiso de cámara, actividad, FileProvider
│     ├─ java/com/inventario/app/
│     │  ├─ MainActivity.java       Lógica: escaneo, detección, duplicados, CSV
│     │  ├─ InventoryDbHelper.java  Base de datos SQLite
│     │  ├─ InventoryItem.java      Modelo de datos
│     │  └─ InventoryAdapter.java   Lista (RecyclerView)
│     └─ res/                       Layouts, textos, tema, iconos
├─ .github/workflows/build.yml     Compilación automática del APK en GitHub
├─ build.gradle, settings.gradle   Configuración raíz de Gradle
└─ gradlew, gradle/wrapper/...      Gradle Wrapper (no borrar)
```

## Subir a GitHub y compilar el APK

No necesitas instalar Android Studio: GitHub compila el APK por ti con GitHub Actions.

### Opción A — desde la web de GitHub (sin línea de comandos)

1. Crea una cuenta en https://github.com si no la tienes.
2. Pulsa **New repository**, ponle un nombre (p. ej. `inventario-app`) y créalo
   (puede ser público o privado). No añadas README ni .gitignore desde la web.
3. En la página del repo vacío, pulsa **uploading an existing file**.
4. Arrastra **todo el contenido** de la carpeta `inventario-app` (no la carpeta en sí, sino
   lo que hay dentro: `app`, `.github`, `gradlew`, `build.gradle`, etc.). Asegúrate de subir
   también la carpeta oculta `.github` y `gradle/wrapper/gradle-wrapper.jar`.
5. Pulsa **Commit changes**.
6. Ve a la pestaña **Actions**. Verás el workflow *Build APK* ejecutándose. Espera a que
   termine (icono verde, unos minutos).
7. Entra en la ejecución terminada y, abajo, en **Artifacts**, descarga
   **inventario-debug-apk**. Dentro está `app-debug.apk`.

> Nota: al arrastrar archivos, la web de GitHub a veces no sube carpetas ocultas como
> `.github`. Si la pestaña Actions no muestra nada, usa la Opción B, que es más fiable.

### Opción B — con Git desde tu ordenador (recomendada)

Necesitas tener Git instalado (https://git-scm.com).

1. Crea el repositorio vacío en GitHub (paso 2 de la Opción A) y copia su URL.
2. Abre una terminal dentro de la carpeta `inventario-app` y ejecuta:

```bash
git init
git add .
git commit -m "Inventario app inicial"
git branch -M main
git remote add origin https://github.com/TU_USUARIO/inventario-app.git
git push -u origin main
```

3. En GitHub, abre la pestaña **Actions**: la compilación arranca sola con el push.
4. Cuando termine, descarga el APK desde **Artifacts** (igual que en la Opción A, paso 7).

### Obtener el APK como Release (opcional, más cómodo para instalar)

Si quieres un enlace de descarga permanente del APK, publica un *tag*:

```bash
git tag v1.0
git push origin v1.0
```

El workflow adjuntará automáticamente `app-debug.apk` a una **Release** en la pestaña
*Releases* del repo.

## Instalar el APK en el móvil

1. Pasa el `app-debug.apk` al teléfono (cable, Drive, email, etc.).
2. En Ajustes, permite **instalar apps de orígenes desconocidos** para el gestor de archivos
   o navegador que uses.
3. Abre el APK y pulsa **Instalar**.
4. Al primer escaneo, concede el permiso de **cámara**.

> Es un APK de *debug* (sin firmar para Play Store). Sirve perfectamente para instalarlo a
> mano. Para publicar en Google Play habría que firmarlo, paso que no se incluye aquí.

## Compilar en local (opcional)

Con Android Studio: abre la carpeta `inventario-app` y deja que sincronice Gradle; luego
**Build > Build APK(s)**. O por terminal, con un JDK 17 instalado:

```bash
./gradlew assembleDebug
```

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`.

## Detalles técnicos

- Lenguaje: Java. minSdk 24 (Android 7.0), targetSdk/compileSdk 34.
- Android Gradle Plugin 8.5.2, Gradle 8.7, JDK 17.
- Escaneo: `com.journeyapps:zxing-android-embedded` (códigos 1D y QR).
- Persistencia: SQLite mediante `SQLiteOpenHelper` (sin dependencias extra).
