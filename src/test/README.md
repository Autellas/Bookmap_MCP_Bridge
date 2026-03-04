# Unit Tests für Bookmap MCP Bridge

## Übersicht

Die Test-Suite umfasst 28 automatisierte Unit-Tests für die Kernkomponenten der Bridge:

### ✅ Test-Abdeckung

- **MarketSnapshotTest** (4 Tests)
  - Snapshot-Erstellung und Datenintegrität
  - JSON-Serialisierung
  - CVD-Berechnung (auch negative Werte)
  - Spread-Handling

- **LiquidityZoneTrackerTest** (7 Tests)
  - Zone-Erkennung und -Tracking
  - Support/Resistance-Klassifizierung
  - Volume-Aggregation
  - Top-Zones-Sortierung

- **DataCollectorTest** (9 Tests)
  - Orderbook-Updates (Bid/Ask)
  - Snapshot-Generierung
  - Fehlerbehandlung
  - Multi-Instrument-Support

- **HttpForwarderTest** (8 Tests)
  - HTTP-POST-Funktionalität
  - Fehlerresilienz (ungültige URLs)
  - JSON-Serialisierung vor Versand
  - Komplexe Datenstrukturen

## Tests ausführen

```bash
# Alle Tests
gradlew test

# Mit Clean Build
gradlew clean test

# Mit detailliertem Output
gradlew test --info
```

## Test-Ergebnisse

Nach dem Test-Lauf finden sich die Ergebnisse unter:
```
build/reports/tests/test/index.html
```

## Test-Konfiguration

- **Framework:** JUnit 5 (Jupiter)
- **Mocking:** Mockito 5.8.0
- **Java-Version:** 17
- **Bookmap-API:** Via `testImplementation` aus `libs/`

## Hinweise

1. **Bookmap-API erforderlich:** Die Tests benötigen die Bookmap-JARs im `libs/`-Verzeichnis
2. **Disabled Tests:** `HttpForwarderTest.testPushSnapshotToRealServer()` ist standardmäßig deaktiviert (benötigt laufenden MCP-Server)
3. **Integration Tests:** Für echte End-to-End-Tests muss der MCP-Server laufen

## Nächste Schritte

- [ ] Integration-Tests mit echtem MCP-Server hinzufügen
- [ ] Code-Coverage-Reports aktivieren (JaCoCo)
- [ ] Performance-Tests für High-Frequency-Szenarien
- [ ] Mock-Tests für Bookmap-Trade-Events erweitern

## CI/CD Integration

Die Tests können problemlos in CI/CD-Pipelines integriert werden:

```yaml
# GitHub Actions Example
- name: Run Tests
  run: ./gradlew test
```
