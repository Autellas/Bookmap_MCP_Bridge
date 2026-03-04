# IntelliJ IDEA Konfiguration für Java 17

## 0. Gradle Wrapper ist bereit ✓

Das Projekt enthält jetzt den Gradle Wrapper (gradlew.bat). Sie benötigen **keine** systemweite Gradle-Installation mehr.

Gradle 8.5 wird beim ersten Build automatisch heruntergeladen.

---

## 1. Project SDK auf Java 17 setzen

### Option A: Über Project Structure
1. **File → Project Structure...** (oder `Ctrl+Alt+Shift+S`)
2. **Project** auswählen (linke Seite)
3. **SDK:** auf Java 17 setzen
   - Falls Java 17 nicht in der Liste ist: **Add SDK → Download JDK...**
   - Provider: Oracle OpenJDK oder Eclipse Temurin
   - Version: 17
4. **Language level:** auf "17 - Sealed types, always-strict floating-point semantics" setzen
5. **Apply** → **OK**

### Option B: Über Gradle JVM
1. **File → Settings** (oder `Ctrl+Alt+S`)
2. **Build, Execution, Deployment → Build Tools → Gradle**
3. **Gradle JVM:** auf Java 17 setzen
   - Falls nicht verfügbar: **Download JDK...** wählen
4. **Apply** → **OK**

---

## 2. Gradle Sync

Nach der Konfiguration:
1. Rechtsklick auf `build.gradle`
2. **Reload Gradle Project** wählen

Oder verwende das Gradle-Tool-Window:
- **View → Tool Windows → Gradle**
- Reload-Button (🔄) klicken

---

## 3. Java 17 Installation verifizieren

### Im Terminal (IntelliJ oder System):
```bash
java -version
```

Erwartete Ausgabe:
```
openjdk version "17.0.x" 2024-xx-xx
OpenJDK Runtime Environment (build 17.0.x+x)
OpenJDK 64-Bit Server VM (build 17.0.x+x, mixed mode, sharing)
```

---

## 4. Projekt bauen

### Via Gradle Task:
```bash
cd java-bridge
gradlew clean build
```

### Via IntelliJ:
1. **Gradle** Tool Window öffnen
2. **bookmap-mcp-bridge → Tasks → build → build** doppelklicken

---

## 5. Troubleshooting

### Problem: "Invalid source release: 17"
**Lösung:** Gradle verwendet ein älteres JDK
- Settings → Build Tools → Gradle → Gradle JVM auf Java 17 setzen

### Problem: Java 17 nicht in der Liste
**Lösung:** JDK 17 herunterladen
1. **File → Project Structure → SDKs**
2. **+ → Download JDK...**
3. Vendor: **Eclipse Temurin** oder **Oracle OpenJDK**
4. Version: **17**
5. Download & Install

### Problem: Gradle Daemon verwendet falsches JDK
**Lösung:** Gradle Daemon neu starten
```bash
cd java-bridge
gradlew --stop
gradlew clean build
```

---

## 6. Build & Deploy zu Bookmap

Wenn alles konfiguriert ist:
```bash
# Im Terminal (java-bridge Verzeichnis)
gradlew buildAndDeploy
```

Oder in IntelliJ:
1. **Gradle** Tool Window
2. **Tasks → custom → buildAndDeploy** doppelklicken

Das JAR wird dann automatisch nach `%USERPROFILE%\BookmapHome\addons` kopiert.

---

## Optional: Run Configuration erstellen

1. **Run → Edit Configurations...**
2. **+ → Gradle**
3. Name: "Build and Deploy to Bookmap"
4. Gradle project: **bookmap-mcp-bridge**
5. Tasks: **buildAndDeploy**
6. **Apply** → **OK**

Jetzt können Sie mit `Shift+F10` oder dem Run-Button bauen und deployen.
