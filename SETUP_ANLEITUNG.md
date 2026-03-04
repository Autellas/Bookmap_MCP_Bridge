# 🚀 Schnellstart: Bookmap Addon mit Java 17

## ✅ Was ist bereits konfiguriert

- **Java 17** ist als Zielversion im Projekt gesetzt (`build.gradle`)
- **Gradle Wrapper 8.5** ist installiert (keine systemweite Installation nötig)
- Alle Build-Dateien sind bereit

---

## 📋 Schritt-für-Schritt Setup

### 1️⃣ IntelliJ IDEA öffnen

1. **File → Open**
2. Wähle den Ordner: `F:\IDE Projects\trading-agent-system\java-bridge`
3. IntelliJ erkennt das Gradle-Projekt automatisch

---

### 2️⃣ Java 17 in IntelliJ konfigurieren

#### Option A: Wenn Java 17 bereits installiert ist

1. **File → Project Structure** (`Ctrl+Alt+Shift+S`)
2. **Project** → **SDK:** Java 17 auswählen
3. **Language Level:** 17 setzen
4. **Apply** → **OK**

#### Option B: Java 17 über IntelliJ installieren

1. **File → Project Structure** (`Ctrl+Alt+Shift+S`)
2. **Project** → **SDK:** → **Add SDK** → **Download JDK...**
3. **Vendor:** Eclipse Temurin oder Oracle OpenJDK
4. **Version:** 17
5. Download & Install
6. **Apply** → **OK**

---

### 3️⃣ Gradle JVM setzen

1. **File → Settings** (`Ctrl+Alt+S`)
2. **Build, Execution, Deployment → Build Tools → Gradle**
3. **Gradle JVM:** Java 17 auswählen
4. **Apply** → **OK**

---

### 4️⃣ Bookmap SDK hinzufügen

1. Öffne deinen Bookmap-Installationsordner:
   - Standard: `C:\Program Files\Bookmap\lib\`
   
2. Kopiere `bookmap-api.jar` nach:
   - `F:\IDE Projects\trading-agent-system\java-bridge\libs\`

3. IntelliJ aktualisieren:
   - Rechtsklick auf `build.gradle` → **Reload Gradle Project**

---

### 5️⃣ Projekt bauen

#### Via Terminal (empfohlen):
```cmd
cd F:\IDE Projects\trading-agent-system\java-bridge
gradlew clean build
```

#### Via IntelliJ Gradle Tool Window:
1. **View → Tool Windows → Gradle**
2. **bookmap-mcp-bridge → Tasks → build → build** (Doppelklick)

---

### 6️⃣ Nach Bookmap deployen

```cmd
cd F:\IDE Projects\trading-agent-system\java-bridge
gradlew buildAndDeploy
```

Das JAR wird automatisch kopiert nach:
- `C:\Users\AlgoDev\BookmapHome\addons\`

Falls ein anderer Pfad:
```cmd
set BOOKMAP_ADDONS_DIR=C:\Dein\Pfad\zu\Bookmap\addons
gradlew buildAndDeploy
```

---

### 7️⃣ In Bookmap aktivieren

1. Bookmap starten
2. **Tools → Addons → Add**
3. Wähle: `bookmap-mcp-bridge-1.0.0.jar`
4. Addon "MCP Trading Bridge" aktivieren

---

## 🔍 Verifizierung

### Java 17 prüfen:
```cmd
java -version
```
Erwartete Ausgabe:
```
openjdk version "17.0.x"
```

### Build testen:
```cmd
cd java-bridge
gradlew --version
```
Sollte Gradle 8.5 und Java 17 anzeigen.

---

## ❌ Troubleshooting

### Problem: "JAVA_HOME is not set"
**Lösung:**
```cmd
set JAVA_HOME=C:\Program Files\Java\jdk-17
set PATH=%JAVA_HOME%\bin;%PATH%
```

Oder in IntelliJ:
- **Settings → Build Tools → Gradle → Gradle JVM** auf Java 17 setzen

### Problem: "Cannot resolve bookmap-api"
**Lösung:**
- Stelle sicher, dass `bookmap-api.jar` im Ordner `libs/` liegt
- Gradle neu laden: Rechtsklick auf `build.gradle` → **Reload Gradle Project**

### Problem: Build funktioniert nicht
**Lösung:**
```cmd
cd java-bridge
gradlew --stop
gradlew clean build --refresh-dependencies
```

---

## 📚 Weitere Dokumentation

- Detaillierte IntelliJ-Konfiguration: `INTELLIJ_SETUP.md`
- Hauptdokumentation: `../README.md`
- Bookmap API: In deiner Bookmap-Installation unter `docs/`

---

## ⚡ Quick Commands

```cmd
# Clean Build
gradlew clean build

# Build + Deploy
gradlew buildAndDeploy

# Nur Deploy (nach manuellem Build)
gradlew deployToBookmap

# Gradle Daemon stoppen
gradlew --stop

# Build mit Debug-Output
gradlew build --info
```

---

## 🎯 Nächste Schritte

Nach erfolgreichem Setup:

1. **MCP Server starten** (siehe `../README.md`)
2. **Orchestrator konfigurieren** (siehe `../orchestrator/`)
3. **Bookmap mit Live-Daten verbinden**
4. **System testen**

Viel Erfolg! 🚀
