package com.nexadev.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nexadev.app.agent.FallbackAgent
import com.nexadev.app.builder.AndroidBuilder
import com.nexadev.app.codex.CodexProcess
import com.nexadev.app.fs.ProjectStore
import com.nexadev.app.mistral.MistralApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NexaDevApp(this) }
    }
}

@Composable
fun NexaDevApp(ctx: ComponentActivity) {
    MaterialTheme(colorScheme = darkColorScheme()) {
        val store = remember { ProjectStore(ctx) }
        val prefs = remember { ctx.getSharedPreferences("nexadev", 0) }
        var apiKey by remember { mutableStateOf(prefs.getString("mistral_key", "") ?: "") }
        var model by remember { mutableStateOf(prefs.getString("model", "codestral-latest") ?: "codestral-latest") }
        var models by remember { mutableStateOf(listOf<String>()) }
        var screen by remember { mutableStateOf(if (apiKey.isBlank()) "login" else "home") }
        var project by remember { mutableStateOf<ProjectStore.Project?>(null) }

        when (screen) {
            "login" -> LoginScreen(apiKey, models, model,
                onKeyChange = { apiKey = it; prefs.edit().putString("mistral_key", it).apply() },
                onModelChange = { model = it; prefs.edit().putString("model", it).apply() },
                onModels = { models = it },
                onDone = { screen = "home" })
            "home" -> HomeScreen(store, onOpen = { project = it; screen = "work" }, onNew = { project = store.create(it); screen = "work" }, engineReady = {
                CodexProcess(ctx, File(ctx.filesDir, "x"), apiKey, model).available
            })
            "work" -> project?.let { p ->
                WorkspaceScreen(ctx, p, apiKey, model, onBack = { screen = "home" })
            } ?: run { screen = "home" }
        }
    }
}

@Composable
fun LoginScreen(apiKey: String, models: List<String>, model: String,
                onKeyChange: (String) -> Unit, onModelChange: (String) -> Unit,
                onModels: (List<String>) -> Unit, onDone: () -> Unit) {
    var key by remember { mutableStateOf(apiKey) }
    var status by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("NexaDev — connexion Mistral", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(key, { key = it }, label = { Text("Clé API Mistral (api.mistral.ai)") },
            modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        Button(onClick = {
            loading = true; status = "Vérification…"
            scope.launch(Dispatchers.IO) {
                try {
                    val list = MistralApi.listModels(key)
                    onKeyChange(key); onModels(list)
                    if (list.isNotEmpty() && list.none { it == model }) onModelChange(list.first())
                    withContext(Dispatchers.Main) { loading = false; onDone() }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { loading = false; status = "Échec : " + e.message }
                }
            }
        }, enabled = !loading && key.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
            Text(if (loading) "Connexion…" else "Se connecter")
        }
        if (models.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text("Modèles disponibles :", style = MaterialTheme.typography.titleSmall)
            LazyColumn(Modifier.fillMaxWidth().height(180.dp)) {
                items(models) { m ->
                    Row(Modifier.fillMaxWidth().padding(6.dp)) {
                        RadioButton(selected = m == model, onClick = { onModelChange(m) })
                        Text(m, Modifier.padding(top = 12.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(status, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onDone) { Text("Continuer sans IA (fonctions locales uniquement)") }
        Text("Aucune requête n est envoyée à OpenAI. La clé reste sur votre appareil.",
            style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun HomeScreen(store: ProjectStore, onOpen: (ProjectStore.Project) -> Unit,
               onNew: (String) -> Unit, engineReady: () -> Boolean) {
    var name by remember { mutableStateOf("") }
    val projects = remember { mutableStateOf(store.list()) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("NexaDev", style = MaterialTheme.typography.headlineLarge)
        Text(if (engineReady()) "Moteur Codex (Rust) : présent" else "Moteur : agent intégré Kotlin/Mistral",
            style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Nouveau projet") }, modifier = Modifier.weight(1f))
            Button(onClick = { if (name.isNotBlank()) { onNew(name); name = "" } }) { Text("Créer") }
        }
        Spacer(Modifier.height(16.dp))
        Text("Projets récents", style = MaterialTheme.typography.titleMedium)
        LazyColumn {
            items(projects.value) { p ->
                Card(onClick = { store.save(p.copy(lastOpened = System.currentTimeMillis())); onOpen(p) },
                    modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(p.name, style = MaterialTheme.typography.titleMedium)
                        Text(p.path, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceScreen(ctx: ComponentActivity, project: ProjectStore.Project, apiKey: String, model: String, onBack: () -> Unit) {
    var tab by remember { mutableStateOf(0) }
    val logs = remember { mutableStateListOf<String>() }
    val files = remember { mutableStateOf(listOf<File>()) }
    var currentFile by remember { mutableStateOf<File?>(null) }
    var fileContent by remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var pendingWrite by remember { mutableStateOf<Pair<String, String>?>(null) }
    var buildRequest by remember { mutableStateOf("") }
    var building by remember { mutableStateOf(false) }
    val root = File(project.path)
    val scope = rememberCoroutineScope()
    val codex = remember { CodexProcess(ctx, root, apiKey, model) }
    fun refresh() { files.value = root.walkTopDown().filter { it.isFile }.take(500).toList() }

    LaunchedEffect(Unit) { refresh() }
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(project.name) },
                navigationIcon = { TextButton(onClick = onBack) { Text("◀ Accueil") } },
                actions = { Text(model, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp)) })
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Text("📁") }, label = { Text("Fichiers") })
                NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Text("🤖") }, label = { Text("Agent") })
                NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, icon = { Text("💻") }, label = { Text("Terminal") })
                NavigationBarItem(selected = tab == 3, onClick = { tab = 3 }, icon = { Text("🏗️") }, label = { Text("Builder") })
            }
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            when (tab) {
                0 -> Column(Modifier.fillMaxSize().padding(8.dp)) {
                    LazyColumn(Modifier.weight(1f)) {
                        items(files.value) { f ->
                            Card(onClick = { currentFile = f; fileContent = f.readText() },
                                modifier = Modifier.fillMaxWidth().padding(2.dp)) {
                                Text(f.relativeTo(root).path, Modifier.padding(10.dp),
                                    style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    currentFile?.let { f ->
                        OutlinedTextField(fileContent, { fileContent = it },
                            label = { Text(f.name) }, modifier = Modifier.fillMaxWidth().height(220.dp))
                        Button(onClick = { f.writeText(fileContent); refresh() }) { Text("Enregistrer") }
                    }
                }
                1 -> Column(Modifier.fillMaxSize().padding(8.dp)) {
                    Text(if (codex.available) "Agent : moteur Codex (Rust) + Mistral" else "Agent intégré (Mistral)", style = MaterialTheme.typography.bodySmall)
                    LazyColumn(Modifier.weight(1f)) { items(logs) { l -> Text(l, style = MaterialTheme.typography.bodySmall) } }
                    pendingWrite?.let { (path, content) ->
                        Card(Modifier.fillMaxWidth().padding(4.dp)) {
                            Column(Modifier.padding(8.dp)) {
                                Text("Modification proposée : " + path, style = MaterialTheme.typography.titleSmall)
                                Button(onClick = {
                                    File(root, path).apply { parentFile?.mkdirs() }.writeText(content)
                                    logs.add("✅ Appliqué : " + path); refresh(); pendingWrite = null
                                }) { Text("Accepter") }
                                TextButton(onClick = { logs.add("❌ Rejeté : " + path); pendingWrite = null }) { Text("Rejeter") }
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(prompt, { prompt = it }, label = { Text("Demande…") }, modifier = Modifier.weight(1f))
                        Button(enabled = !busy, onClick = {
                            busy = true; val req = prompt; prompt = ""
                            scope.launch(Dispatchers.IO) {
                                val agent = FallbackAgent(root, apiKey, model) { path, content ->
                                    pendingWrite = path to content
                                    true
                                }
                                val result = try { agent.run(req) { l -> scope.launch { logs.add(l) } } }
                                catch (e: Exception) { "Erreur : " + e.message }
                                withContext(Dispatchers.Main) { logs.add("🤖 " + result); busy = false; refresh() }
                            }
                        }) { Text(if (busy) "…" else "Envoyer") }
                    }
                }
                2 -> TerminalScreen(root)
                3 -> Column(Modifier.fillMaxSize().padding(8.dp)) {
                    Text("Builder — crée une application Android depuis une demande", style = MaterialTheme.typography.titleSmall)
                    LazyColumn(Modifier.weight(1f)) { items(logs) { l -> Text(l, style = MaterialTheme.typography.bodySmall) } }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(buildRequest, { buildRequest = it },
                            label = { Text("Ex : une app qui classe mes documents…") }, modifier = Modifier.weight(1f))
                        Button(enabled = !building && buildRequest.isNotBlank() && apiKey.isNotBlank(), onClick = {
                            building = true
                            val req = buildRequest; buildRequest = ""
                            scope.launch(Dispatchers.IO) {
                                val b = AndroidBuilder(apiKey, model)
                                try {
                                    val plan = b.generate(req) { p -> scope.launch { logs.add(p) } }
                                    val outDir = File(root, "generated-" + plan.optString("name", "app").replace(Regex("[^A-Za-z0-9]"), "-").lowercase())
                                    outDir.mkdirs()
                                    val written = b.materialize(outDir, plan) { p -> scope.launch { logs.add(p) } }
                                    withContext(Dispatchers.Main) {
                                        logs.add("🏗️ Projet généré : " + plan.optString("name") + " — " + written.size + " fichiers dans " + outDir.name)
                                        building = false; refresh()
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) { logs.add("Erreur Builder : " + e.message); building = false } }
                            }
                        }) { Text(if (building) "…" else "Générer") }
                    }
                    if (apiKey.isBlank()) Text("Ajoutez votre clé Mistral pour utiliser le Builder.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun TerminalScreen(root: File) {
    var cmd by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().padding(8.dp)) {
        Text("Terminal (sh) — " + root.name, style = MaterialTheme.typography.titleSmall)
        LazyColumn(Modifier.weight(1f)) {
            items(output.lines()) { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(cmd, { cmd = it }, label = { Text("Commande") }, modifier = Modifier.weight(1f))
            Button(onClick = {
                val c = cmd; cmd = ""
                scope.launch(Dispatchers.IO) {
                    val p = ProcessBuilder("sh", "-c", c).directory(root).start()
                    val out = p.inputStream.bufferedReader().readText()
                    val err = p.errorStream.bufferedReader().readText()
                    p.waitFor()
                    withContext(Dispatchers.Main) { output += "\n$ " + c + "\n" + out + err }
                }
            }) { Text("Exécuter") }
        }
    }
}
