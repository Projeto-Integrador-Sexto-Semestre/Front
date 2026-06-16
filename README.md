# PI Smart Home - Frontend KMP

## Informacoes do Projeto

**Nome do Projeto:** SMARTHOME

**Tema Escolhido:** Sistema de Monitoramento de Casa Inteligente (Smart Home)

**Integrantes do Projeto:**

- Victor Daniel Araujo Silva
- Gustavo Henrique Santana dos Santos
- Camille Alves Cruz
- Lucas Garcia Lima
- Raphael Micucci Bomfim
- Isaias Belarmina de Souza

## Descricao do Projeto

O **Smart Home Frontend KMP** e a interface multiplataforma do projeto PI Smart Home. A aplicacao foi desenvolvida com Kotlin Multiplatform e Compose Multiplatform para executar em Android, Desktop e Web/Wasm a partir de uma base compartilhada.

O frontend se conecta a API REST do backend para autenticar usuarios, armazenar o token JWT em uma sessao compartilhada e liberar o acesso ao painel principal com CRUDs das entidades do sistema.

## Objetivo da Aplicacao

Centralizar a experiencia de uso do sistema Smart Home, permitindo que o usuario:

- realize login e registro;
- acesse os CRUDs apos autenticacao;
- visualize um dashboard resumido do ambiente inteligente;
- cadastre, liste, edite e apague registros;
- navegue entre modulos do sistema;
- utilize a mesma base de interface em Android, Desktop e Web.

## Tecnologias Utilizadas

| Tecnologia | Uso no Projeto |
|---|---|
| Kotlin Multiplatform | Compartilhamento de codigo entre Android, Desktop e Web/Wasm |
| Compose Multiplatform | Interface declarativa multiplataforma |
| Material 3 | Componentes visuais da interface |
| Ktor Client | Comunicacao HTTP com a API REST |
| kotlinx.serialization | Serializacao JSON |
| Navigation3 | Navegacao em Android e Desktop |
| Gradle Kotlin DSL | Configuracao de build |

Versoes principais em `gradle/libs.versions.toml`:

| Ferramenta | Versao |
|---|---|
| Android Gradle Plugin | `8.13.2` |
| Kotlin | `2.2.21` |
| Compose Multiplatform | `1.7.3` |
| Ktor | `3.0.3` |
| Navigation3 | `1.1.2` |

## Arquitetura Geral

O projeto usa uma arquitetura em camadas dentro do modulo `app`.

```text
Usuario
  |
  v
Telas Compose
  |
  v
ViewModels de CRUD
  |
  v
Repositorios Remotos
  |
  v
SmartHomeApi
  |
  v
Backend REST + JWT
```

### Camada de Interface

Arquivo principal:

```text
app/src/commonMain/kotlin/com/example/smarthome/App.kt
```

Responsavel por:

- tela de login;
- tela de registro;
- dashboard inicial;
- menu de CRUDs;
- tela de listagem;
- tela de formulario;
- cards de registros;
- botoes de gravar, limpar, editar e apagar;
- exibicao de mensagens de erro e carregamento.

### Camada de Sessao

Classe principal:

```kotlin
class AuthSession
```

Responsavel por guardar o estado de autenticacao:

- token JWT;
- email do usuario;
- nome do usuario;
- estado `isAuthenticated`;
- funcao de login na sessao;
- funcao de logout.

O token nao fica em variavel global solta. Ele e armazenado em um objeto de sessao compartilhado pela aplicacao.

### Camada de API

Classe principal:

```kotlin
private class SmartHomeApi
```

Responsavel por chamar o backend usando Ktor Client.

Base atual da API:

```text
https://backend-6lc0.onrender.com
```

Operacoes implementadas:

| Metodo | Funcao no frontend |
|---|---|
| `POST` | Login, registro e criacao de registros |
| `GET` | Listagem de registros |
| `PUT` | Atualizacao de registros |
| `DELETE` | Remocao de registros |

As chamadas autenticadas enviam o token no cabecalho:

```text
Authorization: Bearer <token>
```

### Camada de Repositorios

A classe base e:

```kotlin
GenericRepositorioRemoto
```

Ela concentra as operacoes comuns:

- `listar`
- `criar`
- `atualizar`
- `apagar`

Cada entidade possui sua propria classe de repositorio remoto, por exemplo:

- `PerfilRepositorioRemoto`
- `UsuarioRepositorioRemoto`
- `CasaRepositorioRemoto`
- `ComodoRepositorioRemoto`
- `DispositivoRepositorioRemoto`
- `SensorRepositorioRemoto`
- `AlertaRepositorioRemoto`

Essas classes reutilizam a logica da classe generica, mantendo um repositorio por CRUD conforme a estrutura exigida.

### Camada de ViewModel

A classe base e:

```kotlin
CrudViewModel
```

Responsavel por:

- guardar os estados dos campos do formulario;
- guardar a lista carregada da API;
- controlar o item em edicao;
- controlar loading e mensagens;
- chamar o repositorio remoto correspondente.

Cada CRUD possui seu ViewModel especifico:

- `PerfilViewModel`
- `UsuarioViewModel`
- `CasaViewModel`
- `ComodoViewModel`
- `TipoDispositivoViewModel`
- `DispositivoViewModel`
- `SensorViewModel`
- `AcaoViewModel`
- `RegraAutomacaoViewModel`
- `TipoAlertaViewModel`
- `AlertaViewModel`
- `NotificacaoViewModel`
- `LogEventoViewModel`
- `HistoricoSensorViewModel`

## Arquitetura Multiplataforma

O projeto esta organizado por source sets do Kotlin Multiplatform.

```text
app/src
|-- commonMain
|   `-- kotlin/com/example/smarthome
|       |-- App.kt
|       `-- Navigation.kt
|-- androidMain
|   `-- kotlin/com/example/smarthome
|       |-- MainActivity.kt
|       `-- Navigation.android.kt
|-- desktopMain
|   `-- kotlin/com/example/smarthome
|       |-- Main.kt
|       `-- Navigation.desktop.kt
`-- wasmJsMain
    |-- kotlin/com/example/smarthome
    |   |-- Main.kt
    |   `-- Navigation.wasmJs.kt
    `-- resources/index.html
```

### `commonMain`

Contem a maior parte da aplicacao:

- modelos;
- sessao de autenticacao;
- cliente REST;
- catalogo dos CRUDs;
- repositorios;
- ViewModels;
- telas Compose;
- abstracao de navegacao.

### `androidMain`

Contem a entrada Android:

```kotlin
class MainActivity : ComponentActivity()
```

Tambem contem a implementacao Android da navegacao usando Navigation3.

### `desktopMain`

Contem a entrada Desktop:

```kotlin
fun main() = application { ... }
```

Tambem contem a implementacao Desktop da navegacao usando Navigation3.

### `wasmJsMain`

Contem a entrada Web/Wasm:

```kotlin
fun main() {
    ComposeViewport(document.body!!) { App() }
}
```

No Web/Wasm, a navegacao usa a mesma abstracao do `commonMain`, mas com uma pilha baseada em estado Compose para manter compatibilidade com o alvo Web.

## Navegacao

A navegacao possui dois niveis:

| Nivel | Responsabilidade |
|---|---|
| Navegacao entre CRUDs | Trocar entre Perfis, Usuarios, Casas, Comodos, Sensores, Alertas e demais modulos |
| Navegacao interna do CRUD | Alternar entre Listagem e Formulario |

No `commonMain`, a navegacao e declarada por uma abstracao:

```kotlin
expect fun <T : PlatformNavKey> rememberPlatformNavStack(initialKey: T): MutableList<T>
```

Chaves de navegacao:

```kotlin
CrudNavKey
ScreenNavKey
```

Uso na tela principal:

```kotlin
val crudStack = rememberPlatformNavStack(CrudNavKey(CrudKind.Home.id))
val screenStack = rememberPlatformNavStack(ScreenNavKey("list"))
```

Implementacoes por plataforma:

| Plataforma | Implementacao |
|---|---|
| Android | `Navigation.android.kt` com Navigation3 |
| Desktop | `Navigation.desktop.kt` com Navigation3 |
| Web/Wasm | `Navigation.wasmJs.kt` com estado Compose |

## Entidades do Sistema

O frontend possui 14 entidades, alinhadas aos modelos do backend.

| Frontend | Backend |
|---|---|
| `Perfil` | `Profile` |
| `Usuario` | `User` |
| `Casa` | `House` |
| `Comodo` | `Room` |
| `TipoDispositivo` | `DeviceType` |
| `Dispositivo` | `IotDevice` |
| `Sensor` | `Sensor` |
| `Acao` | `Action` |
| `RegraAutomacao` | `AutomationRule` |
| `TipoAlerta` | `AlertType` |
| `Alerta` | `Alert` |
| `Notificacao` | `Notification` |
| `LogEvento` | `EventLog` |
| `HistoricoSensor` | `SensorHistory` |

## Catalogo de CRUDs

Os CRUDs sao descritos por `EntitySpec`, que define:

- entidade;
- endpoint da API;
- campos do formulario;
- tipo de cada campo;
- campos exibidos nos cards;
- caminho para leitura de dados retornados pela API.

Endpoints usados:

| CRUD | Endpoint |
|---|---|
| Perfis | `/profiles` |
| Usuarios | `/users` |
| Casas | `/houses` |
| Comodos | `/rooms` |
| Tipos de dispositivo | `/device-types` |
| Dispositivos | `/devices` |
| Sensores | `/sensors` |
| Acoes | `/actions` |
| Regras de automacao | `/automation-rules` |
| Tipos de alerta | `/alert-types` |
| Alertas | `/alerts` |
| Notificacoes | `/notifications` |
| Logs | `/event-logs` |
| Historico de sensor | `/sensor-history` |

## Fluxo de Autenticacao

```text
Usuario informa email e senha
  |
  v
Frontend chama POST /users/login
  |
  v
Backend retorna JWT
  |
  v
AuthSession armazena o token
  |
  v
App libera a tela principal dos CRUDs
```

Registro de novo usuario:

```text
Usuario informa nome, email, senha e confirmacao
  |
  v
Frontend valida a confirmacao da senha
  |
  v
Frontend chama POST /users/register
  |
  v
Frontend chama login automaticamente
  |
  v
Usuario entra na area principal
```

## Fluxo de CRUD

### Listagem

```text
Tela de listagem
  |
  v
ViewModel.carregar()
  |
  v
RepositorioRemoto.listar()
  |
  v
SmartHomeApi GET
  |
  v
Lista atualizada na interface
```

### Criacao

```text
Formulario preenchido
  |
  v
Botao Gravar
  |
  v
ViewModel.gravar()
  |
  v
SmartHomeApi POST
  |
  v
Carrega a lista novamente
```

### Edicao

```text
Clique no icone de editar
  |
  v
Formulario recebe os dados do card
  |
  v
Botao Gravar
  |
  v
SmartHomeApi PUT
  |
  v
Carrega a lista novamente
```

### Exclusao

```text
Clique no icone de apagar
  |
  v
SmartHomeApi DELETE
  |
  v
Carrega a lista novamente
```

## Estrutura de Pastas

```text
.
|-- build.gradle.kts
|-- settings.gradle.kts
|-- gradle.properties
|-- gradle
|   |-- libs.versions.toml
|   `-- wrapper
|-- app
|   |-- build.gradle.kts
|   |-- proguard-rules.pro
|   `-- src
|       |-- commonMain
|       |   `-- kotlin/com/example/smarthome
|       |       |-- App.kt
|       |       `-- Navigation.kt
|       |-- androidMain
|       |   |-- AndroidManifest.xml
|       |   `-- kotlin/com/example/smarthome
|       |       |-- MainActivity.kt
|       |       `-- Navigation.android.kt
|       |-- desktopMain
|       |   `-- kotlin/com/example/smarthome
|       |       |-- Main.kt
|       |       `-- Navigation.desktop.kt
|       |-- wasmJsMain
|       |   |-- kotlin/com/example/smarthome
|       |   |   |-- Main.kt
|       |   |   `-- Navigation.wasmJs.kt
|       |   `-- resources/index.html
|       |-- androidTest
|       `-- test
`-- README.md
```

## Como Executar

### Android

Abra o projeto no Android Studio, sincronize o Gradle e execute o modulo `app` em um emulador ou dispositivo.

Configuracoes Android:

```kotlin
applicationId = "com.example.smarthome"
minSdk = 26
targetSdk = 35
compileSdk = 36
```

### Desktop

No terminal:

```bash
./gradlew :app:run
```

No Windows:

```powershell
.\gradlew.bat :app:run
```

### Web/Wasm

No terminal:

```bash
./gradlew :app:wasmJsBrowserDevelopmentRun
```

No Windows:

```powershell
.\gradlew.bat :app:wasmJsBrowserDevelopmentRun
```

## Comandos Uteis

Compilar Android:

```powershell
.\gradlew.bat :app:compileDebugKotlinAndroid
```

Compilar Desktop:

```powershell
.\gradlew.bat :app:compileKotlinDesktop
```

Compilar Web/Wasm:

```powershell
.\gradlew.bat :app:compileKotlinWasmJs
```

Compilar os tres alvos:

```powershell
.\gradlew.bat :app:compileDebugKotlinAndroid :app:compileKotlinDesktop :app:compileKotlinWasmJs
```

Limpar arquivos compilados antes de compactar:

```powershell
.\gradlew.bat clean
```

## Cuidados Antes da Entrega

Antes de gerar o `.zip`, remova pastas de build:

```text
build/
app/build/
```

O `.gitignore` tambem ignora arquivos grandes de dump de memoria:

```gitignore
*.hprof
```

Esses arquivos nao devem ser enviados para o GitHub nem incluidos no zip da entrega.

## Observacoes de Integracao

O frontend esta preparado para usar `PUT` no fluxo de edicao. Para que a edicao funcione em todos os CRUDs, o backend tambem precisa disponibilizar os respectivos endpoints `PUT /{recurso}/{id}`.

No backend atual, alguns controllers possuem apenas `GET`, `POST` e `DELETE`. Nesses casos, a listagem, criacao e exclusao ficam alinhadas, mas a edicao depende da implementacao do `PUT` no backend.
