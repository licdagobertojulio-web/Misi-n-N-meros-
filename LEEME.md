# Misión Números para Android

Proyecto Android listo para compilar. Contiene el juego completo de Matemáticas Divertidas (conjuntos, números reales e intervalos) dentro de una aplicación nativa.

Diseño: Mg. Dagoberto Julio Gamarra.

© 2026 Todos los derechos reservados.

- Paquete: `com.matematicasdivertidas.misionnumeros`
- Versión: 1.0 (versionCode 1)
- Android 7.0 o superior (minSdk 24), compilado para Android 15 (targetSdk 35)

## Qué cambia frente a la versión web

| Función | En la app |
| --- | --- |
| Voz del presentador | Usa el motor de voz del teléfono (Google o Samsung), porque el navegador interno de Android no trae voz. Las voces en español e inglés instaladas aparecen en el engrane. |
| Compartir avance | El botón verde abre WhatsApp directamente con la imagen adjunta. Si WhatsApp no está instalado, aparece el menú de compartir. |
| Sin internet | El juego funciona completo sin conexión. Con internet se cargan las tipografías. |
| Progreso | Se guarda en el teléfono, dentro de la app. |
| Botón atrás | Cierra el menú de configuración o vuelve al mapa; en el mapa sale de la app. |

## Opción 1: compilar en la nube con GitHub (sin instalar nada)

1. Crea una cuenta gratuita en github.com y un repositorio nuevo (puede ser privado).
2. Sube todo el contenido de esta carpeta al repositorio, incluida la carpeta oculta `.github`. Desde la web: «Add file» → «Upload files» y arrastra las carpetas.
3. Entra a la pestaña **Actions**. El flujo «Construir APK» arranca solo; tarda unos 5 minutos.
4. Al terminar, abre la ejecución y descarga **MisionNumeros-APK** en la sección «Artifacts». Dentro está `app-debug.apk`.

## Opción 2: compilar en Android Studio

1. Instala Android Studio (gratuito).
2. «File» → «Open» y elige esta carpeta. Espera a que termine de sincronizar.
3. «Build» → «Build App Bundle(s) / APK(s)» → «Build APK(s)».
4. El archivo queda en `app/build/outputs/apk/debug/app-debug.apk`.

## Instalar en los celulares de los estudiantes

1. Envía el archivo `.apk` por WhatsApp, Drive o cable.
2. En el celular, ábrelo. Android pedirá permitir «Instalar apps desconocidas» para esa aplicación (WhatsApp, Archivos o Drive); se acepta una sola vez.
3. Toca «Instalar».

Recomendación para la voz: en el celular, Ajustes → Accesibilidad → Texto a voz, verifica que el motor sea «Servicios de voz de Google» y descarga los datos de voz de «Español (Estados Unidos)» o «Español (México)». Así la voz funciona sin internet.

## Actualizar el juego

El juego vive en un solo archivo: `app/src/main/assets/www/index.html`. Si se cambia el juego, se reemplaza ese archivo, se sube `versionCode` y `versionName` en `app/build.gradle` y se vuelve a compilar.

## Publicar en Play Store (opcional)

La APK de prueba (`debug`) sirve para instalar directamente. Para Play Store se necesita una versión firmada:

1. Crea una llave (una sola vez y guárdala muy bien; sin ella no se pueden publicar actualizaciones):
   `keytool -genkey -v -keystore misionnumeros.jks -keyalg RSA -keysize 2048 -validity 10000 -alias misionnumeros`
2. Agrega en `gradle.properties` (no lo subas a un repositorio público):
   ```
   MN_KEYSTORE=../misionnumeros.jks
   MN_STORE_PASSWORD=tu_clave
   MN_KEY_ALIAS=misionnumeros
   MN_KEY_PASSWORD=tu_clave
   ```
3. Genera el paquete con `./gradlew bundleRelease` (queda en `app/build/outputs/bundle/release/`).
4. En la consola de Play se necesita una cuenta de desarrollador, una política de privacidad y, como la usan menores de edad, cumplir la política de Familias. El ícono de 512 px está en `play-store/icono-512.png`.

## Estructura

```
MisionNumerosAndroid/
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/www/index.html        ← el juego
│       ├── java/com/matematicasdivertidas/misionnumeros/MainActivity.java
│       └── res/                         ← íconos, colores, temas
├── .github/workflows/construir-apk.yml  ← compilación automática en GitHub
├── play-store/icono-512.png
├── build.gradle, settings.gradle, gradle.properties
└── gradlew, gradlew.bat, gradle/        ← Gradle 8.11.1
```
