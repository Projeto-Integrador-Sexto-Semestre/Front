# PI Smart Home Mobile

Base Kotlin Multiplatform com Compose para Android Studio.

## O que ja esta pronto

- 14 CRUDs mockados, divididos pelos 7 integrantes.
- Tela mobile em Compose Multiplatform.
- Interface `SmartHomeApi` com operacoes de CRUD.
- `MockSmartHomeApi` para demonstracao estatica.
- `RestSmartHomeApi` com Ktor preparado para backend Spring Boot em `http://10.0.2.2:8080`.
- Painel IoT simulado com broker Mosquitto e topico MQTT.

## Abrir no Android Studio

Abra a pasta `mobile` pelo Android Studio e sincronize o Gradle.

Quando o backend estiver pronto, substitua o uso de `MockSmartHomeApi` em `App.kt` por uma implementacao REST baseada no `RestSmartHomeApi`.
