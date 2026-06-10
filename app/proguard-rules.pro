# Reglas ProGuard. El build de release no usa minify por defecto.
# Mantener clases de ZXing por si se habilita minify en el futuro.
-keep class com.journeyapps.barcodescanner.** { *; }
-keep class com.google.zxing.** { *; }
