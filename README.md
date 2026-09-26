# BITTO-TV 2026

Proyecto Android listo para subir a GitHub y compilar con Codemagic.

## Incluye

- Fondo principal usando la imagen proporcionada.
- Icono de aplicación basado en la imagen proporcionada.
- Botón **REPRODUCTOR IPTV**.
- Lista IPTV argentina precargada desde:
  `https://iptv-org.github.io/iptv/countries/ar.m3u`
- Descarga automática de la lista al abrir el reproductor.
- Buscador de canales.
- Reproductor integrado con AndroidX Media3 / ExoPlayer.
- `codemagic.yaml` incluido para generar el APK release.

## Compilación local

Requiere JDK 17 y Android SDK. Ejecutar:

`./gradlew assembleRelease`

APK generado:

`app/build/outputs/apk/release/app-release.apk`

## Codemagic

Subir este proyecto completo a GitHub, crear/seleccionar la aplicación en Codemagic y ejecutar el workflow **BITTO-TV 2026 Android**.

La aplicación usa la lista pública indicada por el usuario. La disponibilidad de cada señal depende de que el canal esté publicado y operativo en esa lista.
