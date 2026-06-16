package com.example.smarthome

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

private const val ApiBaseUrl = "https://backend-6lc0.onrender.com"

@Serializable
private data class LoginRequest(val email: String, val password: String)

@Serializable
private data class RegisterUserRequest(
    val email: String,
    val password: String,
    val name: String,
    val profileId: Long = 1L
)

@Serializable
private data class TokenResponse(val token: String)

@Serializable data class Perfil(val id: Long? = null, val name: String = "", val description: String = "", val canControlDevices: Boolean = false, val canEditStructure: Boolean = false, val canViewLogs: Boolean = true)
@Serializable data class Usuario(val id: Long? = null, val name: String = "", val email: String = "", val password: String = "", val profileId: Long? = null, val active: Boolean = true)
@Serializable data class Casa(val id: Long? = null, val name: String = "", val address: String = "", val userId: Long? = null, val active: Boolean = true, val area: Double = 0.0)
@Serializable data class Comodo(val id: Long? = null, val name: String = "", val type: String = "", val houseId: Long? = null, val floor: Int = 0, val active: Boolean = true)
@Serializable data class TipoDispositivo(val id: Long? = null, val name: String = "", val manufacturer: String = "", val unit: String = "", val monitored: Boolean = false)
@Serializable data class Dispositivo(val id: Long? = null, val name: String = "", val deviceTypeId: Long? = null, val topic: String = "", val status: String = "OFF", val roomId: Long? = null)
@Serializable data class Sensor(val id: Long? = null, val name: String = "", val mqttTopic: String = "", val deviceTypeId: Long? = null, val roomId: Long? = null, val precision: Double = 0.0)
@Serializable data class Acao(val id: Long? = null, val name: String = "", val deviceId: Long? = null, val command: String = "", val enabled: Boolean = true)
@Serializable data class RegraAutomacao(val id: Long? = null, val name: String = "", val condition: String = "", val enabled: Boolean = true, val actionId: Long? = null, val priority: Int = 0)
@Serializable data class TipoAlerta(val id: Long? = null, val name: String = "", val description: String = "", val severity: Int = 1, val active: Boolean = true)
@Serializable data class Alerta(val id: Long? = null, val message: String = "", val alertTypeId: Long? = null, val sensorId: Long? = null, val deviceId: Long? = null, val acknowledged: Boolean = false)
@Serializable data class Notificacao(val id: Long? = null, val message: String = "", val userId: Long? = null, val read: Boolean = false, val priority: Int = 0)
@Serializable data class LogEvento(val id: Long? = null, val eventType: String = "", val message: String = "", val userId: Long? = null, val important: Boolean = false)
@Serializable data class HistoricoSensor(val id: Long? = null, val value: String = "", val sensorId: Long? = null, val timestamp: String = "", val numericValue: Double = 0.0)

class AuthSession {
    var token by mutableStateOf<String?>(null)
        private set
    var email by mutableStateOf("")
        private set
    var name by mutableStateOf("")
        private set

    val isAuthenticated: Boolean get() = token != null

    fun authenticate(jwt: String, userEmail: String, userName: String = userEmail) {
        token = jwt
        email = userEmail
        name = userName
    }

    fun logout() {
        token = null
        email = ""
        name = ""
    }
}

private class SmartHomeApi(private val session: AuthSession) {
    private val client = HttpClient {
        install(ContentNegotiation) { json() }
        install(HttpTimeout) {
            requestTimeoutMillis = 60000
            connectTimeoutMillis = 60000
            socketTimeoutMillis = 60000
        }
    }
    private fun endpoint(path: String): String = ApiBaseUrl.trimEnd('/') + "/" + path.trimStart('/')

    suspend fun login(email: String, password: String) {
        val response: TokenResponse = client.post(endpoint("users/login")) {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email = email, password = password))
        }.body()
        session.authenticate(response.token, email)
    }

    suspend fun register(name: String, email: String, password: String) {
        client.post(endpoint("users/register")) {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            setBody(RegisterUserRequest(email = email, password = password, name = name))
        }
        login(email, password)
    }

    suspend fun list(path: String): JsonElement {
        val body: JsonElement = client.get(endpoint(path)) { authorizedJson() }.body()
        println("API LIST $path: $body")
        return body
    }

    suspend fun create(path: String, payload: JsonObject) {
        println("API CREATE $path: $payload")
        client.post(endpoint(path)) { authorizedJson(); setBody(payload) }
    }

    suspend fun update(path: String, id: Long, payload: JsonObject) {
        println("API UPDATE $path/$id: $payload")
        client.put(endpoint("$path/$id")) { authorizedJson(); setBody(payload) }
    }

    suspend fun delete(path: String, id: Long) {
        println("API DELETE $path/$id")
        client.delete(endpoint("$path/$id")) { authorizedJson() }
    }

    private fun HttpRequestBuilder.authorizedJson() {
        accept(ContentType.Application.Json)
        contentType(ContentType.Application.Json)
        session.token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
    }
}

private enum class FieldType { Text, Password, Boolean, Int, Double, Date, Time, DateTime }

private data class FieldSpec(
    val key: String,
    val label: String,
    val type: FieldType = FieldType.Text,
    val responsePath: String = key,
    val required: Boolean = true,
    val form: Boolean = true,
    val sendInPayload: Boolean = true,
    val defaultValue: String = ""
)

private data class CrudItem(val id: Long?, val values: Map<String, String>)

private enum class CrudKind(val id: String, val title: String) {
    Home("home", "Início"),
    Perfis("profiles", "Perfis"),
    Usuarios("users", "Usuarios"),
    Casas("houses", "Casas"),
    Comodos("rooms", "Comodos"),
    TiposDispositivo("device-types", "Tipos de dispositivo"),
    Dispositivos("devices", "Dispositivos"),
    Sensores("sensors", "Sensores"),
    Acoes("actions", "Acoes"),
    RegrasAutomacao("automation-rules", "Regras"),
    TiposAlerta("alert-types", "Tipos de alerta"),
    Alertas("alerts", "Alertas"),
    Notificacoes("notifications", "Notificacoes"),
    Logs("event-logs", "Logs"),
    HistoricoSensor("sensor-history", "Historico")
}

private data class EntitySpec(
    val kind: CrudKind,
    val endpoint: String,
    val createEndpoint: String = endpoint,
    val fields: List<FieldSpec>,
    val cardFields: List<String>,
    val listPath: (Map<String, String>) -> String = { endpoint }
)

private object CrudSpecs {
    val perfis = EntitySpec(
        kind = CrudKind.Perfis,
        endpoint = "profiles",
        fields = listOf(
            FieldSpec("name", "Nome"),
            FieldSpec("description", "Descricao"),
            FieldSpec("canControlDevices", "Controla dispositivos", FieldType.Boolean, defaultValue = "false"),
            FieldSpec("canEditStructure", "Edita estrutura", FieldType.Boolean, defaultValue = "false"),
            FieldSpec("canViewLogs", "Visualiza logs", FieldType.Boolean, defaultValue = "true")
        ),
        cardFields = listOf("name", "description", "canControlDevices")
    )

    val usuarios = EntitySpec(
        kind = CrudKind.Usuarios,
        endpoint = "users",
        createEndpoint = "users/register",
        fields = listOf(
            FieldSpec("name", "Nome"),
            FieldSpec("email", "Email"),
            FieldSpec("password", "Senha", FieldType.Password),
            FieldSpec("profileId", "Perfil ID", FieldType.Int, responsePath = "profile.id", defaultValue = "1", form = false),
            FieldSpec("active", "Ativo", FieldType.Boolean, sendInPayload = false, defaultValue = "true")
        ),
        cardFields = listOf("name", "email")
    )

    val casas = EntitySpec(
        kind = CrudKind.Casas,
        endpoint = "houses",
        fields = listOf(
            FieldSpec("name", "Nome"),
            FieldSpec("address", "Endereco"),
            FieldSpec("userId", "Usuario ID", FieldType.Int, responsePath = "user.id", defaultValue = "1", form = false),
            FieldSpec("active", "Ativa", FieldType.Boolean, sendInPayload = false, defaultValue = "true"),
            FieldSpec("area", "Area", FieldType.Double, sendInPayload = false, defaultValue = "0")
        ),
        cardFields = listOf("name", "address")
    )

    val comodos = EntitySpec(
        kind = CrudKind.Comodos,
        endpoint = "rooms",
        fields = listOf(
            FieldSpec("name", "Nome"),
            FieldSpec("type", "Tipo"),
            FieldSpec("houseId", "Casa ID", FieldType.Int, responsePath = "house.id", defaultValue = "1", form = false),
            FieldSpec("floor", "Andar", FieldType.Int, sendInPayload = false, defaultValue = "0"),
            FieldSpec("active", "Ativo", FieldType.Boolean, sendInPayload = false, defaultValue = "true")
        ),
        cardFields = listOf("name", "type")
    )

    val tiposDispositivo = EntitySpec(
        kind = CrudKind.TiposDispositivo,
        endpoint = "device-types",
        fields = listOf(
            FieldSpec("name", "Nome"),
            FieldSpec("manufacturer", "Fabricante"),
            FieldSpec("unit", "Unidade", required = false),
            FieldSpec("monitored", "Monitorado", FieldType.Boolean, sendInPayload = false, defaultValue = "false")
        ),
        cardFields = listOf("name", "manufacturer", "unit")
    )

    val dispositivos = EntitySpec(
        kind = CrudKind.Dispositivos,
        endpoint = "devices",
        fields = listOf(
            FieldSpec("name", "Nome"),
            FieldSpec("deviceTypeId", "Tipo ID", FieldType.Int, responsePath = "deviceType.id", defaultValue = "1", form = false),
            FieldSpec("topic", "Topico MQTT"),
            FieldSpec("status", "Status", defaultValue = "OFF"),
            FieldSpec("roomId", "Comodo ID", FieldType.Int, responsePath = "room.id", defaultValue = "1", form = false)
        ),
        cardFields = listOf("name", "status", "topic")
    )

    val sensores = EntitySpec(
        kind = CrudKind.Sensores,
        endpoint = "sensors",
        fields = listOf(
            FieldSpec("name", "Nome"),
            FieldSpec("mqttTopic", "Topico MQTT"),
            FieldSpec("deviceTypeId", "Tipo ID", FieldType.Int, responsePath = "deviceType.id", defaultValue = "1", form = false),
            FieldSpec("roomId", "Comodo ID", FieldType.Int, responsePath = "room.id", defaultValue = "1", form = false),
            FieldSpec("precision", "Precisao", FieldType.Double, sendInPayload = false, defaultValue = "0")
        ),
        cardFields = listOf("name", "mqttTopic")
    )

    val acoes = EntitySpec(
        kind = CrudKind.Acoes,
        endpoint = "actions",
        fields = listOf(
            FieldSpec("name", "Nome"),
            FieldSpec("deviceId", "Dispositivo ID", FieldType.Int, responsePath = "device.id", defaultValue = "1", form = false),
            FieldSpec("command", "Comando"),
            FieldSpec("enabled", "Ativa", FieldType.Boolean, sendInPayload = false, defaultValue = "true")
        ),
        cardFields = listOf("name", "command")
    )

    val regrasAutomacao = EntitySpec(
        kind = CrudKind.RegrasAutomacao,
        endpoint = "automation-rules",
        fields = listOf(
            FieldSpec("name", "Nome"),
            FieldSpec("condition", "Condicao"),
            FieldSpec("enabled", "Habilitada", FieldType.Boolean, defaultValue = "true"),
            FieldSpec("actionId", "Acao ID", FieldType.Int, responsePath = "action.id", defaultValue = "1", form = false),
            FieldSpec("priority", "Prioridade", FieldType.Int, sendInPayload = false, defaultValue = "0")
        ),
        cardFields = listOf("name", "condition", "enabled")
    )

    val tiposAlerta = EntitySpec(
        kind = CrudKind.TiposAlerta,
        endpoint = "alert-types",
        fields = listOf(
            FieldSpec("name", "Nome"),
            FieldSpec("description", "Descricao", required = false),
            FieldSpec("severity", "Severidade", FieldType.Int, sendInPayload = false, defaultValue = "1"),
            FieldSpec("active", "Ativo", FieldType.Boolean, sendInPayload = false, defaultValue = "true")
        ),
        cardFields = listOf("name", "description", "active")
    )

    val alertas = EntitySpec(
        kind = CrudKind.Alertas,
        endpoint = "alerts",
        fields = listOf(
            FieldSpec("message", "Mensagem"),
            FieldSpec("alertTypeId", "Tipo de alerta ID", FieldType.Int, responsePath = "alertType.id", defaultValue = "1", form = false),
            FieldSpec("sensorId", "Sensor ID", FieldType.Int, responsePath = "sensor.id", required = false, defaultValue = "1", form = false),
            FieldSpec("deviceId", "Dispositivo ID", FieldType.Int, responsePath = "device.id", required = false, defaultValue = "1", form = false),
            FieldSpec("acknowledged", "Reconhecido", FieldType.Boolean, sendInPayload = false, defaultValue = "false"),
            FieldSpec("timestamp", "Data/hora", FieldType.DateTime, form = false, sendInPayload = false)
        ),
        cardFields = listOf("message", "timestamp", "acknowledged")
    )

    val notificacoes = EntitySpec(
        kind = CrudKind.Notificacoes,
        endpoint = "notifications",
        fields = listOf(
            FieldSpec("message", "Mensagem"),
            FieldSpec("userId", "Usuario ID", FieldType.Int, responsePath = "user.id", defaultValue = "1", form = false),
            FieldSpec("read", "Lida", FieldType.Boolean, sendInPayload = false, defaultValue = "false"),
            FieldSpec("priority", "Prioridade", FieldType.Int, sendInPayload = false, defaultValue = "0"),
            FieldSpec("timestamp", "Data/hora", FieldType.DateTime, form = false, sendInPayload = false)
        ),
        cardFields = listOf("message", "timestamp", "read")
    )

    val logs = EntitySpec(
        kind = CrudKind.Logs,
        endpoint = "event-logs",
        fields = listOf(
            FieldSpec("eventType", "Tipo de evento"),
            FieldSpec("message", "Mensagem"),
            FieldSpec("userId", "Usuario ID", FieldType.Int, responsePath = "user.id", defaultValue = "1", form = false),
            FieldSpec("important", "Importante", FieldType.Boolean, sendInPayload = false, defaultValue = "false"),
            FieldSpec("timestamp", "Data/hora", FieldType.DateTime, form = false, sendInPayload = false)
        ),
        cardFields = listOf("eventType", "message", "timestamp")
    )

    val historicoSensor = EntitySpec(
        kind = CrudKind.HistoricoSensor,
        endpoint = "sensor-history",
        fields = listOf(
            FieldSpec("value", "Valor"),
            FieldSpec("sensorId", "Sensor ID", FieldType.Int, responsePath = "sensor.id", defaultValue = "1", form = false),
            FieldSpec("timestamp", "Data/hora", FieldType.DateTime, form = false, sendInPayload = false),
            FieldSpec("numericValue", "Valor numerico", FieldType.Double, sendInPayload = false, defaultValue = "0"),
            FieldSpec("confirmed", "Confirmado", FieldType.Boolean, sendInPayload = false, defaultValue = "true")
        ),
        cardFields = listOf("value", "timestamp"),
        listPath = { values -> "sensor-history/sensor/${values["sensorId"].asLongOrDefault(1L)}" }
    )

    fun byKind(kind: CrudKind): EntitySpec = when (kind) {
        CrudKind.Home -> throw IllegalArgumentException("Home has no spec")
        CrudKind.Perfis -> perfis
        CrudKind.Usuarios -> usuarios
        CrudKind.Casas -> casas
        CrudKind.Comodos -> comodos
        CrudKind.TiposDispositivo -> tiposDispositivo
        CrudKind.Dispositivos -> dispositivos
        CrudKind.Sensores -> sensores
        CrudKind.Acoes -> acoes
        CrudKind.RegrasAutomacao -> regrasAutomacao
        CrudKind.TiposAlerta -> tiposAlerta
        CrudKind.Alertas -> alertas
        CrudKind.Notificacoes -> notificacoes
        CrudKind.Logs -> logs
        CrudKind.HistoricoSensor -> historicoSensor
    }
}

private open class GenericRepositorioRemoto(protected val api: SmartHomeApi, val spec: EntitySpec) {
    open suspend fun listar(formValues: Map<String, String>): List<CrudItem> {
        val element = api.list(spec.listPath(formValues))
        val array = element as? JsonArray ?: JsonArray(emptyList())
        return array.map { spec.toItem(it) }
    }

    open suspend fun criar(payload: JsonObject) { api.create(spec.createEndpoint, payload) }
    open suspend fun atualizar(id: Long, payload: JsonObject) { api.update(spec.endpoint, id, payload) }
    open suspend fun apagar(id: Long) { api.delete(spec.endpoint, id) }
}

private class PerfilRepositorioRemoto(api: SmartHomeApi) : GenericRepositorioRemoto(api, CrudSpecs.perfis)
private class UsuarioRepositorioRemoto(api: SmartHomeApi) : GenericRepositorioRemoto(api, CrudSpecs.usuarios)
private class CasaRepositorioRemoto(api: SmartHomeApi) : GenericRepositorioRemoto(api, CrudSpecs.casas)
private class ComodoRepositorioRemoto(api: SmartHomeApi) : GenericRepositorioRemoto(api, CrudSpecs.comodos)
private class TipoDispositivoRepositorioRemoto(api: SmartHomeApi) : GenericRepositorioRemoto(api, CrudSpecs.tiposDispositivo)
private class DispositivoRepositorioRemoto(api: SmartHomeApi) : GenericRepositorioRemoto(api, CrudSpecs.dispositivos)
private class SensorRepositorioRemoto(api: SmartHomeApi) : GenericRepositorioRemoto(api, CrudSpecs.sensores)
private class AcaoRepositorioRemoto(api: SmartHomeApi) : GenericRepositorioRemoto(api, CrudSpecs.acoes)
private class RegraAutomacaoRepositorioRemoto(api: SmartHomeApi) : GenericRepositorioRemoto(api, CrudSpecs.regrasAutomacao)
private class TipoAlertaRepositorioRemoto(api: SmartHomeApi) : GenericRepositorioRemoto(api, CrudSpecs.tiposAlerta)
private class AlertaRepositorioRemoto(api: SmartHomeApi) : GenericRepositorioRemoto(api, CrudSpecs.alertas)
private class NotificacaoRepositorioRemoto(api: SmartHomeApi) : GenericRepositorioRemoto(api, CrudSpecs.notificacoes)
private class LogEventoRepositorioRemoto(api: SmartHomeApi) : GenericRepositorioRemoto(api, CrudSpecs.logs)
private class HistoricoSensorRepositorioRemoto(api: SmartHomeApi) : GenericRepositorioRemoto(api, CrudSpecs.historicoSensor)

private open class CrudViewModel(val spec: EntitySpec, private val repositorio: GenericRepositorioRemoto) {
    val fieldStates: Map<String, MutableState<String>> = spec.fields.associate { field -> field.key to mutableStateOf(field.defaultValue) }
    var lista by mutableStateOf<List<CrudItem>>(emptyList())
        private set
    var editingId by mutableStateOf<Long?>(null)
        private set
    var loading by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
        private set

    fun value(field: FieldSpec): String = fieldStates[field.key]?.value.orEmpty()
    fun setValue(field: FieldSpec, value: String) { fieldStates[field.key]?.value = value }

    suspend fun carregar() {
        loading = true
        message = null
        runCatching { repositorio.listar(currentValues()) }
            .onSuccess { lista = it; message = if (it.isEmpty()) "Nenhum registro encontrado." else null }
            .onFailure { message = it.cleanMessage() }
        loading = false
    }

    suspend fun gravar(): Boolean {
        loading = true
        message = null
        val saved = runCatching {
            val payload = spec.toPayload(currentValues())
            val id = editingId
            if (id == null) repositorio.criar(payload) else repositorio.atualizar(id, payload)
            carregar()
            limpar()
        }.onFailure { message = it.cleanMessage() }.isSuccess
        loading = false
        return saved
    }

    suspend fun apagar(item: CrudItem) {
        val id = item.id ?: return
        loading = true
        message = null
        runCatching { repositorio.apagar(id); carregar() }
            .onFailure { message = it.cleanMessage() }
        loading = false
    }

    fun editar(item: CrudItem) {
        editingId = item.id
        spec.fields.forEach { field -> fieldStates[field.key]?.value = item.values[field.key].orEmpty().ifBlank { field.defaultValue } }
        message = null
    }

    fun limpar() {
        editingId = null
        spec.fields.forEach { field -> fieldStates[field.key]?.value = field.defaultValue }
        message = null
    }

    private fun currentValues(): Map<String, String> = fieldStates.mapValues { it.value.value }
}

private class PerfilViewModel(repo: PerfilRepositorioRemoto) : CrudViewModel(repo.spec, repo)
private class UsuarioViewModel(repo: UsuarioRepositorioRemoto) : CrudViewModel(repo.spec, repo)
private class CasaViewModel(repo: CasaRepositorioRemoto) : CrudViewModel(repo.spec, repo)
private class ComodoViewModel(repo: ComodoRepositorioRemoto) : CrudViewModel(repo.spec, repo)
private class TipoDispositivoViewModel(repo: TipoDispositivoRepositorioRemoto) : CrudViewModel(repo.spec, repo)
private class DispositivoViewModel(repo: DispositivoRepositorioRemoto) : CrudViewModel(repo.spec, repo)
private class SensorViewModel(repo: SensorRepositorioRemoto) : CrudViewModel(repo.spec, repo)
private class AcaoViewModel(repo: AcaoRepositorioRemoto) : CrudViewModel(repo.spec, repo)
private class RegraAutomacaoViewModel(repo: RegraAutomacaoRepositorioRemoto) : CrudViewModel(repo.spec, repo)
private class TipoAlertaViewModel(repo: TipoAlertaRepositorioRemoto) : CrudViewModel(repo.spec, repo)
private class AlertaViewModel(repo: AlertaRepositorioRemoto) : CrudViewModel(repo.spec, repo)
private class NotificacaoViewModel(repo: NotificacaoRepositorioRemoto) : CrudViewModel(repo.spec, repo)
private class LogEventoViewModel(repo: LogEventoRepositorioRemoto) : CrudViewModel(repo.spec, repo)
private class HistoricoSensorViewModel(repo: HistoricoSensorRepositorioRemoto) : CrudViewModel(repo.spec, repo)

private class DashboardViewModel(private val api: SmartHomeApi) {
    var temperature by mutableStateOf("--")
    var luminosity by mutableStateOf("--")
    var movement by mutableStateOf("--")
    var monitoredRooms by mutableStateOf<List<Pair<String, String>>>(emptyList())
    var recentAlerts by mutableStateOf<List<String>>(emptyList())
    var loading by mutableStateOf(false)
    var activeAlertsCount by mutableStateOf(0)

    suspend fun refresh() {
        loading = true
        runCatching {
            val sensorsJson = api.list("sensors") as? JsonArray ?: JsonArray(emptyList())
            val alertsJson = api.list("alerts") as? JsonArray ?: JsonArray(emptyList())
            val roomsJson = api.list("rooms") as? JsonArray ?: JsonArray(emptyList())

            recentAlerts = alertsJson.take(3).map { it.jsonObject["message"]?.jsonPrimitive?.content ?: "Sem msg" }.ifEmpty { listOf("Nenhum alerta") }
            activeAlertsCount = alertsJson.count { it.jsonObject["acknowledged"]?.jsonPrimitive?.boolean == false }

            sensorsJson.forEach { s ->
                val name = s.jsonObject["name"]?.jsonPrimitive?.content?.lowercase() ?: ""
                val id = s.jsonObject["id"]?.jsonPrimitive?.longOrNull ?: return@forEach
                val history = api.list("sensor-history/sensor/$id") as? JsonArray
                val lastVal = history?.lastOrNull()?.jsonObject?.get("value")?.jsonPrimitive?.content ?: "--"
                if (name.contains("temp")) temperature = "$lastVal°C"
                else if (name.contains("lumin")) luminosity = "$lastVal%"
                else if (name.contains("movi") || name.contains("presen")) movement = lastVal
            }

            monitoredRooms = roomsJson.map { r ->
                val rName = r.jsonObject["name"]?.jsonPrimitive?.content ?: "Comodo"
                val rId = r.jsonObject["id"]?.jsonPrimitive?.longOrNull
                val rSensors = sensorsJson.filter { it.jsonObject["room"]?.jsonObject?.get("id")?.jsonPrimitive?.longOrNull == rId }
                val status = if (rSensors.isNotEmpty()) {
                    val firstS = rSensors.first()
                    val sId = firstS.jsonObject["id"]?.jsonPrimitive?.longOrNull
                    val valStr = if (sId != null) (api.list("sensor-history/sensor/$sId") as? JsonArray)?.lastOrNull()?.jsonObject?.get("value")?.jsonPrimitive?.content ?: "--" else "--"
                    "${firstS.jsonObject["name"]?.jsonPrimitive?.content}: $valStr"
                } else "Sem sensores"
                rName to status
            }
        }
        loading = false
    }
}

private class AppViewModels(api: SmartHomeApi) {
    val dashboard = DashboardViewModel(api)
    private val perfis = PerfilViewModel(PerfilRepositorioRemoto(api))
    private val usuarios = UsuarioViewModel(UsuarioRepositorioRemoto(api))
    private val casas = CasaViewModel(CasaRepositorioRemoto(api))
    private val comodos = ComodoViewModel(ComodoRepositorioRemoto(api))
    private val tiposDispositivo = TipoDispositivoViewModel(TipoDispositivoRepositorioRemoto(api))
    private val dispositivos = DispositivoViewModel(DispositivoRepositorioRemoto(api))
    private val sensores = SensorViewModel(SensorRepositorioRemoto(api))
    private val acoes = AcaoViewModel(AcaoRepositorioRemoto(api))
    private val regras = RegraAutomacaoViewModel(RegraAutomacaoRepositorioRemoto(api))
    private val tiposAlerta = TipoAlertaViewModel(TipoAlertaRepositorioRemoto(api))
    private val alertas = AlertaViewModel(AlertaRepositorioRemoto(api))
    private val notificacoes = NotificacaoViewModel(NotificacaoRepositorioRemoto(api))
    private val logs = LogEventoViewModel(LogEventoRepositorioRemoto(api))
    private val historico = HistoricoSensorViewModel(HistoricoSensorRepositorioRemoto(api))

    fun viewModel(kind: CrudKind): CrudViewModel = when (kind) {
        CrudKind.Home -> throw IllegalArgumentException("Home has no viewmodel")
        CrudKind.Perfis -> perfis
        CrudKind.Usuarios -> usuarios
        CrudKind.Casas -> casas
        CrudKind.Comodos -> comodos
        CrudKind.TiposDispositivo -> tiposDispositivo
        CrudKind.Dispositivos -> dispositivos
        CrudKind.Sensores -> sensores
        CrudKind.Acoes -> acoes
        CrudKind.RegrasAutomacao -> regras
        CrudKind.TiposAlerta -> tiposAlerta
        CrudKind.Alertas -> alertas
        CrudKind.Notificacoes -> notificacoes
        CrudKind.Logs -> logs
        CrudKind.HistoricoSensor -> historico
    }
}

@Composable
fun App() {
    val session = remember { AuthSession() }
    val api = remember { SmartHomeApi(session) }
    val viewModels = remember { AppViewModels(api) }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF226C63),
            secondary = Color(0xFF49645F),
            tertiary = Color(0xFF815513),
            background = Color(0xFFF6F7F9),
            surface = Color.White,
            onSurface = Color(0xFF18201E)
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (session.isAuthenticated) MainCrudArea(session, viewModels) else AuthArea(api)
        }
    }
}

@Composable
private fun AuthArea(api: SmartHomeApi) {
    var mode by remember { mutableStateOf("login") }
    Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.fillMaxWidth().width(420.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Text("Smart Home", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                }
                if (mode == "login") LoginScreen(api, onRegisterClick = { mode = "register" }) else RegisterScreen(api, onLoginClick = { mode = "login" })
            }
        }
    }
}

@Composable
private fun LoginScreen(api: SmartHomeApi, onRegisterClick: () -> Unit) {
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Senha") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            onClick = {
                scope.launch {
                    loading = true
                    error = null
                    runCatching { api.login(email.trim(), password) }.onFailure { error = it.cleanMessage() }
                    loading = false
                }
            },
            enabled = !loading && email.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (loading) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp) else {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Logar")
            }
        }
        OutlinedButton(onClick = onRegisterClick, modifier = Modifier.fillMaxWidth()) { Text("Ir para registro") }
    }
}

@Composable
private fun RegisterScreen(api: SmartHomeApi, onLoginClick: () -> Unit) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Senha") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = confirmPassword, onValueChange = { confirmPassword = it }, label = { Text("Confirmar senha") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            onClick = {
                if (password != confirmPassword) {
                    error = "As senhas nao conferem."
                    return@Button
                }
                scope.launch {
                    loading = true
                    error = null
                    runCatching { api.register(name.trim(), email.trim(), password) }.onFailure { error = it.cleanMessage() }
                    loading = false
                }
            },
            enabled = !loading && name.isNotBlank() && email.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (loading) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp) else {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Registrar")
            }
        }
        OutlinedButton(onClick = onLoginClick, modifier = Modifier.fillMaxWidth()) { Text("Ir para login") }
    }
}

@Composable
private fun MainCrudArea(session: AuthSession, viewModels: AppViewModels) {
    val crudStack = rememberPlatformNavStack(CrudNavKey(CrudKind.Home.id))
    val screenStack = rememberPlatformNavStack(ScreenNavKey("list"))
    val selectedKind = CrudKind.entries.firstOrNull { it.id == (crudStack.lastOrNull() as? CrudNavKey)?.kindId } ?: CrudKind.Home
    val selectedViewModel = if (selectedKind == CrudKind.Home) null else viewModels.viewModel(selectedKind)

    LaunchedEffect(selectedKind) {
        screenStack.setOnly(ScreenNavKey("list"))
        selectedViewModel?.carregar()
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val compact = maxWidth < 860.dp
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(session)
            if (compact) {
                CompactCrudNav(selectedKind) { kind -> crudStack.setOnly(CrudNavKey(kind.id)) }
                CrudContent(
                    kind = selectedKind,
                    viewModel = selectedViewModel,
                    dashboardViewModel = viewModels.dashboard,
                    isCompact = true,
                    screenName = (screenStack.lastOrNull() as? ScreenNavKey)?.screen ?: "list",
                    onShowForm = { screenStack.setOnly(ScreenNavKey("form")) },
                    onShowList = { screenStack.setOnly(ScreenNavKey("list")) }
                )
            } else {
                Row(modifier = Modifier.fillMaxSize()) {
                    SideCrudNav(selectedKind) { kind -> crudStack.setOnly(CrudNavKey(kind.id)) }
                    CrudContent(
                        kind = selectedKind,
                        viewModel = selectedViewModel,
                        dashboardViewModel = viewModels.dashboard,
                        isCompact = false,
                        screenName = (screenStack.lastOrNull() as? ScreenNavKey)?.screen ?: "list",
                        onShowForm = { screenStack.setOnly(ScreenNavKey("form")) },
                        onShowList = { screenStack.setOnly(ScreenNavKey("list")) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TopBar(session: AuthSession) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Smart Home", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(session.email, color = Color(0xFF61706C), fontSize = 13.sp)
            }
        }
        TextButton(onClick = { session.logout() }) {
            Icon(Icons.Default.Logout, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Sair")
        }
    }
    HorizontalDivider(color = Color(0xFFE5E8EB))
}

@Composable
private fun SideCrudNav(selectedKind: CrudKind, onSelect: (CrudKind) -> Unit) {
    Column(
        modifier = Modifier.width(240.dp).fillMaxHeight().background(Color(0xFFEDF4F1)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("CRUDs", fontWeight = FontWeight.Bold, color = Color(0xFF41534F), modifier = Modifier.padding(8.dp))
        CrudKind.entries.forEach { kind ->
            val selected = kind == selectedKind
            Surface(
                onClick = { onSelect(kind) },
                shape = RoundedCornerShape(8.dp),
                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                contentColor = if (selected) Color.White else Color(0xFF263331),
                border = if (selected) null else BorderStroke(1.dp, Color(0xFFD6DEDB))
            ) {
                Text(kind.title, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@Composable
private fun CompactCrudNav(selectedKind: CrudKind, onSelect: (CrudKind) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().background(Color(0xFFEDF4F1)),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(12.dp)
    ) {
        items(CrudKind.entries.toList()) { kind ->
            val selected = kind == selectedKind
            Surface(
                onClick = { onSelect(kind) },
                shape = RoundedCornerShape(8.dp),
                color = if (selected) MaterialTheme.colorScheme.primary else Color.White,
                contentColor = if (selected) Color.White else Color(0xFF263331),
                border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else Color(0xFFD6DEDB))
            ) { Text(kind.title, modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) }
        }
    }
}

@Composable
private fun CrudContent(kind: CrudKind, viewModel: CrudViewModel?, dashboardViewModel: DashboardViewModel, isCompact: Boolean, screenName: String, onShowForm: () -> Unit, onShowList: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(if (isCompact) 12.dp else 18.dp)) {
        if (kind == CrudKind.Home) {
            DashboardScreen(dashboardViewModel, isCompact)
        } else if (viewModel != null) {
            if (screenName == "form") {
                CrudFormScreen(viewModel = viewModel, onBack = onShowList)
            } else {
                CrudListScreen(
                    viewModel = viewModel,
                    onNew = { viewModel.limpar(); onShowForm() },
                    onEdit = { item -> viewModel.editar(item); onShowForm() }
                )
            }
        }
    }
}

@Composable
private fun DashboardScreen(viewModel: DashboardViewModel, isCompact: Boolean) {
    LaunchedEffect(Unit) { viewModel.refresh() }

    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(if (isCompact) 14.dp else 20.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Resumo do Sistema", fontWeight = FontWeight.Bold, fontSize = if (isCompact) 20.sp else 24.sp, color = Color(0xFF1D2724))
                if (viewModel.loading) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 16.dp)) {
                DashboardCard(Modifier.weight(1f), "Temperatura", "Normal", viewModel.temperature, Color(0xFFE57373), isCompact)
                DashboardCard(Modifier.weight(1f), "Luminosidade", "Ambiente", viewModel.luminosity, Color(0xFFFFB74D), isCompact)
                DashboardCard(Modifier.weight(1f), "Movimento", "Presenca", viewModel.movement, Color(0xFF64B5F6), isCompact)
            }
        }

        item {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val sideBySide = maxWidth > 700.dp
                if (sideBySide) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        MonitoredEnvironmentsCard(Modifier.weight(1f), viewModel.monitoredRooms)
                        RecentAlertsCard(Modifier.weight(1f), viewModel.recentAlerts, viewModel.activeAlertsCount)
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        MonitoredEnvironmentsCard(Modifier.fillMaxWidth(), viewModel.monitoredRooms)
                        RecentAlertsCard(Modifier.fillMaxWidth(), viewModel.recentAlerts, viewModel.activeAlertsCount)
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardCard(modifier: Modifier, title: String, tag: String, value: String, accentColor: Color, isCompact: Boolean) {
    Card(
        modifier = modifier.height(if (isCompact) 100.dp else 140.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.width(if (isCompact) 4.dp else 6.dp).fillMaxHeight().background(accentColor))
            Column(modifier = Modifier.padding(if (isCompact) 10.dp else 16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(title, fontWeight = FontWeight.Bold, color = Color(0xFF41534F), fontSize = if (isCompact) 11.sp else 16.sp, maxLines = 1)
                    if (!isCompact) {
                        Spacer(Modifier.height(4.dp))
                        Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(12.dp)) {
                            Text(tag, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 11.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Medium)
                        }
                    }
                }
                Text(value, fontSize = if (isCompact) 22.sp else 36.sp, fontWeight = FontWeight.Black, color = Color(0xFF1D2724))
            }
        }
    }
}

@Composable
private fun MonitoredEnvironmentsCard(modifier: Modifier, rooms: List<Pair<String, String>>) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Ambientes monitorados", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1D2724))
            rooms.forEach { (name, status) ->
                Surface(color = Color(0xFFF8F9F9), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(name, fontWeight = FontWeight.Bold, color = Color(0xFF1D2724))
                        Text(status, color = Color(0xFF71807B), fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentAlertsCard(modifier: Modifier, alerts: List<String>, activeCount: Int) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Alertas recentes", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1D2724))
                Surface(color = if (activeCount > 0) Color(0xFFFFEBEE) else Color(0xFFE0F2F1), shape = RoundedCornerShape(12.dp)) {
                    Text("$activeCount ativos", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 12.sp, color = if (activeCount > 0) Color(0xFFC62828) else Color(0xFF00695C), fontWeight = FontWeight.Bold)
                }
            }
            alerts.forEach { alert ->
                Surface(color = Color(0xFFF8F9F9), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                    Text(alert, modifier = Modifier.padding(14.dp), color = Color(0xFF1D2724), fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun CrudListScreen(viewModel: CrudViewModel, onNew: () -> Unit, onEdit: (CrudItem) -> Unit) {
    val scope = rememberCoroutineScope()
    Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(viewModel.spec.kind.title, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                Text("Listagem", color = Color(0xFF60706C))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { scope.launch { viewModel.carregar() } }) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Carregar")
                }
                Button(onClick = onNew) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Novo")
                }
            }
        }
        viewModel.message?.let { Text(it, color = Color(0xFF5C6B67)) }
        if (viewModel.loading) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
            items(viewModel.lista, key = { item -> item.id ?: item.hashCode().toLong() }) { item ->
                CrudCard(
                    spec = viewModel.spec,
                    item = item,
                    onEdit = { onEdit(item) },
                    onDelete = { scope.launch { viewModel.apagar(item) } }
                )
            }
        }
    }
}

@Composable
private fun CrudCard(spec: EntitySpec, item: CrudItem, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.weight(1f)) {
                Text("ID: ${item.id ?: "-"}", color = Color(0xFF71807B), fontSize = 12.sp)
                spec.cardFields.forEachIndexed { index, key ->
                    val field = spec.fields.firstOrNull { it.key == key }
                    val label = field?.label ?: key
                    val value = item.values[key].orEmpty().ifBlank { "-" }
                    Text(
                        text = "$label: $value",
                        fontWeight = if (index == 0) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (index == 0) Color(0xFF1D2724) else Color(0xFF53635F)
                    )
                }
            }
            Row {
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary) }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Apagar", tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun CrudFormScreen(viewModel: CrudViewModel, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Voltar") }
                Column {
                    Text(viewModel.spec.kind.title, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                    Text(if (viewModel.editingId == null) "Formulario" else "Editando ID ${viewModel.editingId}", color = Color(0xFF60706C))
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                viewModel.spec.fields.filter { it.form }.forEach { field ->
                    FieldEditor(field = field, value = viewModel.value(field), onValueChange = { viewModel.setValue(field, it) })
                }
                viewModel.message?.let { Text(it, color = Color(0xFF5C6B67)) }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            scope.launch {
                                val saved = viewModel.gravar()
                                if (saved) onBack()
                            }
                        },
                        enabled = !viewModel.loading
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Gravar")
                    }
                    OutlinedButton(onClick = { viewModel.limpar() }, enabled = !viewModel.loading) { Text("Limpar Campos") }
                }
            }
        }
    }
}

@Composable
private fun FieldEditor(field: FieldSpec, value: String, onValueChange: (String) -> Unit) {
    if (field.type == FieldType.Boolean) {
        Surface(shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color(0xFFD5DDD9)), color = Color.White, modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(field.label)
                Checkbox(checked = value.equals("true", ignoreCase = true), onCheckedChange = { onValueChange(it.toString()) })
            }
        }
        return
    }

    val keyboardType = when (field.type) {
        FieldType.Int -> KeyboardType.Number
        FieldType.Double -> KeyboardType.Decimal
        else -> KeyboardType.Text
    }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(field.label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = field.type != FieldType.Text,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (field.type == FieldType.Password) PasswordVisualTransformation() else VisualTransformation.None
    )
}

private fun EntitySpec.toPayload(values: Map<String, String>): JsonObject = buildJsonObject {
    fields.filter { it.sendInPayload }.forEach { field ->
        val raw = values[field.key].orEmpty().trim()
        if (field.form && raw.isBlank() && !field.required) return@forEach
        put(field.key, field.toJson(raw))
    }
}

private fun FieldSpec.toJson(raw: String): JsonElement = when (type) {
    FieldType.Boolean -> JsonPrimitive(raw.equals("true", ignoreCase = true))
    FieldType.Int -> JsonPrimitive(raw.asLongOrDefault(0L))
    FieldType.Double -> JsonPrimitive(raw.replace(',', '.').toDoubleOrNull() ?: 0.0)
    else -> JsonPrimitive(raw)
}

private fun EntitySpec.toItem(element: JsonElement): CrudItem {
    val id = element.valueAt("id").toLongOrNull()
    val values = fields.associate { field -> field.key to element.valueAt(field.responsePath) } + ("id" to (id?.toString().orEmpty()))
    return CrudItem(id = id, values = values)
}

private fun JsonElement.valueAt(path: String): String {
    var current: JsonElement = this
    path.split('.').forEach { part ->
        val obj = current as? JsonObject ?: return ""
        current = obj[part] ?: return ""
    }
    return current.displayValue()
}

private fun JsonElement.displayValue(): String = when (this) {
    JsonNull -> ""
    is JsonPrimitive -> contentOrNull.orEmpty()
    is JsonArray -> joinToString("-") { it.displayValue() }
    is JsonObject -> this["name"]?.displayValue() ?: this["id"]?.displayValue().orEmpty()
}

private fun String?.asLongOrDefault(default: Long): Long = this?.toLongOrNull() ?: default

private fun Throwable.cleanMessage(): String = message?.replace(ApiBaseUrl, "API")?.take(160) ?: "Nao foi possivel completar a operacao."

private fun <T> MutableList<T>.setOnly(item: T) {
    clear()
    add(item)
}
