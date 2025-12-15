package com.example.aplicacionpaseador.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.example.aplicacionpaseador.data.RetrofitClient
import com.example.aplicacionpaseador.data.UserPreferences
import com.example.aplicacionpaseador.data.Walk
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import java.io.File
import java.io.FileOutputStream
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody

@Composable
fun PaseosScreen() {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context) }
    val accessToken by userPreferences.accessToken.collectAsState(initial = null)

    val snackbarHostState = remember { SnackbarHostState() }

    var isLoadingPending by remember { mutableStateOf(true) }
    var isLoadingAccepted by remember { mutableStateOf(true) }
    var isLoadingHistory by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf<List<Walk>>(emptyList()) }
    var accepted by remember { mutableStateOf<List<Walk>>(emptyList()) }
    var history by remember { mutableStateOf<List<Walk>>(emptyList()) }
    var actionInProgress by remember { mutableStateOf(false) }
    var errorPending by remember { mutableStateOf<String?>(null) }
    var errorAccepted by remember { mutableStateOf<String?>(null) }
    var errorHistory by remember { mutableStateOf<String?>(null) }

    // estado para diálogo de detalle
    var selectedWalk by remember { mutableStateOf<Walk?>(null) }
    var walkPhotos by remember { mutableStateOf<List<String>>(emptyList()) }
    var detailLoading by remember { mutableStateOf(false) }
    var uploadInProgress by remember { mutableStateOf(false) }

    // Launcher para seleccionar imagen
    val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val walk = selectedWalk
        val token = accessToken
        if (uri != null && walk != null && token != null) {
            scope.launch {
                try {
                    uploadInProgress = true
                    // Copiar el contenido a archivo temporal
                    val input = context.contentResolver.openInputStream(uri)
                    val tempFile = File.createTempFile("walk_photo_", ".jpg", context.cacheDir)
                    FileOutputStream(tempFile).use { out ->
                        input?.copyTo(out)
                    }
                    val requestBody = tempFile.asRequestBody("image/*".toMediaTypeOrNull())
                    val part = MultipartBody.Part.createFormData("photo", tempFile.name, requestBody)
                    val resp = RetrofitClient.instance.uploadWalkPhoto(
                        token = "Bearer $token",
                        id = walk.id,
                        photo = part
                    )
                    if (resp.isSuccessful) {
                        snackbarHostState.showSnackbar("Foto subida correctamente", withDismissAction = true)
                        // refrescar fotos
                        val photosResp = RetrofitClient.instance.getWalkPhotos("Bearer $token", walk.id)
                        walkPhotos = photosResp.body() ?: emptyList()
                    } else {
                        snackbarHostState.showSnackbar("Error al subir foto: ${resp.code()}")
                    }
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar(e.localizedMessage ?: "Error de carga")
                } finally {
                    uploadInProgress = false
                }
            }
        }
    }

    LaunchedEffect(accessToken) {
        if (accessToken != null) {
            val service = RetrofitClient.instance
            // Cargar listas en paralelo con supervisorScope para que errores no cancelen las demás
            kotlinx.coroutines.supervisorScope {
                launch {
                    try {
                        val resp = service.getPendingWalks("Bearer $accessToken")
                        if (resp.isSuccessful) {
                            pending = resp.body() ?: emptyList()
                            errorPending = null
                        } else {
                            errorPending = "Error pendientes: ${resp.code()}"
                        }
                    } catch (e: Exception) {
                        errorPending = e.localizedMessage ?: "Error de red (pendientes)"
                    } finally {
                        isLoadingPending = false
                    }
                }
                launch {
                    try {
                        val resp = service.getAcceptedWalks("Bearer $accessToken")
                        if (resp.isSuccessful) {
                            accepted = resp.body() ?: emptyList()
                            errorAccepted = null
                        } else {
                            errorAccepted = "Error aceptados: ${resp.code()}"
                        }
                    } catch (e: Exception) {
                        errorAccepted = e.localizedMessage ?: "Error de red (aceptados)"
                    } finally {
                        isLoadingAccepted = false
                    }
                }
                launch {
                    try {
                        val resp = service.getAllWalks("Bearer $accessToken")
                        if (resp.isSuccessful) {
                            history = resp.body()?.filter { it.status == "ended" || it.status == "finished" || it.status == "completed" } ?: emptyList()
                            errorHistory = null
                        } else {
                            errorHistory = "Error historial: ${resp.code()}"
                        }
                    } catch (e: Exception) {
                        errorHistory = e.localizedMessage ?: "Error de red (historial)"
                    } finally {
                        isLoadingHistory = false
                    }
                }
            }
        } else {
            isLoadingPending = false
            isLoadingAccepted = false
            isLoadingHistory = false
            errorPending = "No autenticado"
            errorAccepted = "No autenticado"
            errorHistory = "No autenticado"
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Encabezado pendientes
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Solicitudes pendientes",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                AssistChip(
                    onClick = {},
                    label = { Text(text = "${pending.size}") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            when {
                isLoadingPending -> {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                errorPending != null && pending.isEmpty() -> {
                    Text("${errorPending}", color = MaterialTheme.colorScheme.error)
                }
                pending.isEmpty() -> {
                    Text("No hay solicitudes pendientes", style = MaterialTheme.typography.bodyMedium)
                }
                else -> {
                    pending.forEach { walk ->
                        WalkItem(
                            walk = walk,
                            onAccept = { id ->
                                if (actionInProgress) return@WalkItem
                                scope.launch {
                                    actionInProgress = true
                                    val service = RetrofitClient.instance
                                    try {
                                        val resp = service.acceptWalk("Bearer ${accessToken}", id)
                                        if (resp.isSuccessful) {
                                            // refrescar listas
                                            val p = service.getPendingWalks("Bearer ${accessToken}").body() ?: emptyList()
                                            val a = service.getAcceptedWalks("Bearer ${accessToken}").body() ?: emptyList()
                                            pending = p
                                            accepted = a
                                            snackbarHostState.showSnackbar("Paseo aceptado", withDismissAction = true)
                                        } else {
                                            snackbarHostState.showSnackbar("Error al aceptar: ${resp.code()}")
                                        }
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar(e.localizedMessage ?: "Error de red")
                                    } finally {
                                        actionInProgress = false
                                    }
                                }
                            },
                            onReject = { id ->
                                if (actionInProgress) return@WalkItem
                                scope.launch {
                                    actionInProgress = true
                                    val service = RetrofitClient.instance
                                    try {
                                        val resp = service.rejectWalk("Bearer ${accessToken}", id)
                                        if (resp.isSuccessful) {
                                            val p = service.getPendingWalks("Bearer ${accessToken}").body() ?: emptyList()
                                            pending = p
                                            snackbarHostState.showSnackbar("Paseo rechazado", withDismissAction = true)
                                        } else {
                                            snackbarHostState.showSnackbar("Error al rechazar: ${resp.code()}")
                                        }
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar(e.localizedMessage ?: "Error de red")
                                    } finally {
                                        actionInProgress = false
                                    }
                                }
                            },
                            actionsEnabled = !actionInProgress
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Encabezado aceptados
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Paseos aceptados",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                AssistChip(
                    onClick = {},
                    label = { Text(text = "${accepted.size}") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            when {
                isLoadingAccepted -> {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                errorAccepted != null && accepted.isEmpty() -> {
                    Text("${errorAccepted}", color = MaterialTheme.colorScheme.error)
                }
                accepted.isEmpty() -> {
                    Text("No hay paseos aceptados", style = MaterialTheme.typography.bodyMedium)
                }
                else -> {
                    accepted.forEach { walk ->
                        AcceptedWalkItem(
                            walk = walk,
                            onClick = {
                                selectedWalk = walk
                                detailLoading = true
                                scope.launch {
                                    try {
                                        val photosResp = RetrofitClient.instance.getWalkPhotos("Bearer ${accessToken}", walk.id)
                                        walkPhotos = photosResp.body() ?: emptyList()
                                    } catch (_: Exception) {
                                        walkPhotos = emptyList()
                                    } finally {
                                        detailLoading = false
                                    }
                                }
                            },
                            onStart = {
                                if (actionInProgress) return@AcceptedWalkItem
                                scope.launch {
                                    actionInProgress = true
                                    try {
                                        val resp = RetrofitClient.instance.startWalk("Bearer ${accessToken}", walk.id)
                                        if (resp.isSuccessful) {
                                            snackbarHostState.showSnackbar("Paseo Iniciado", withDismissAction = true)
                                        } else {
                                            snackbarHostState.showSnackbar("No se pudo iniciar: ${resp.code()}")
                                        }
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar(e.localizedMessage ?: "Error de red")
                                    } finally {
                                        actionInProgress = false
                                    }
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Encabezado historial
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Historial de paseos",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                AssistChip(
                    onClick = {},
                    label = { Text(text = "${history.size}") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            when {
                isLoadingHistory -> {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                errorHistory != null && history.isEmpty() -> {
                    Text("${errorHistory}", color = MaterialTheme.colorScheme.error)
                }
                history.isEmpty() -> {
                    Text("No hay paseos anteriores", style = MaterialTheme.typography.bodyMedium)
                }
                else -> {
                    history.forEach { walk ->
                        HistoryWalkItem(walk = walk)
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
    }

    // Diálogo de detalle del paseo aceptado
    if (selectedWalk != null) {
        AlertDialog(
            onDismissRequest = { selectedWalk = null },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { pickImageLauncher.launch("image/*") }, enabled = !uploadInProgress) {
                        Text(if (uploadInProgress) "Subiendo..." else "Subir foto")
                    }
                    TextButton(onClick = {
                        val walk = selectedWalk ?: return@TextButton
                        if (actionInProgress) return@TextButton
                        scope.launch {
                            actionInProgress = true
                            try {
                                val resp = RetrofitClient.instance.endWalk("Bearer ${accessToken}", walk.id)
                                if (resp.isSuccessful) {
                                    snackbarHostState.showSnackbar("Paseo #${walk.id} finalizado", withDismissAction = true)
                                    // Remover localmente de aceptados para feedback inmediato
                                    accepted = accepted.filter { it.id != walk.id }
                                    // Refrescar listas desde backend
                                    try {
                                        val acc = RetrofitClient.instance.getAcceptedWalks("Bearer ${accessToken}").body() ?: emptyList()
                                        val histResp = RetrofitClient.instance.getAllWalks("Bearer ${accessToken}")
                                        val hist = if (histResp.isSuccessful) {
                                            histResp.body()?.filter { it.status == "ended" || it.status == "finished" || it.status == "completed" } ?: emptyList()
                                        } else emptyList()
                                        accepted = acc
                                        history = hist
                                    } catch (_: Exception) { }
                                    selectedWalk = null
                                } else {
                                    snackbarHostState.showSnackbar("No se pudo finalizar: ${resp.code()}")
                                }
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar(e.localizedMessage ?: "Error de red")
                            } finally {
                                actionInProgress = false
                            }
                        }
                    }, enabled = !actionInProgress) { Text("Finalizar") }
                    TextButton(onClick = { selectedWalk = null }) { Text("Cerrar") }
                }
            },
            title = { Text("Detalle de la mascota") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Paseo #${selectedWalk!!.id}")
                    Text("Fecha: ${selectedWalk!!.scheduledAt ?: "—"}")
                    Text("Duración: ${selectedWalk!!.durationMinutes?.let { "$it min" } ?: "—"}")
                    if (!selectedWalk!!.notes.isNullOrBlank()) {
                        Text("Notas: ${selectedWalk!!.notes}")
                    }
                    if (detailLoading) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    } else {
                        val firstPhoto = walkPhotos.firstOrNull()
                        if (firstPhoto != null) {
                            Text("Foto: ${firstPhoto}")
                        } else {
                            Text("No hay fotos disponibles")
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun WalkItem(
    walk: Walk,
    onAccept: (Int) -> Unit,
    onReject: (Int) -> Unit,
    actionsEnabled: Boolean
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        ListItem(
            headlineContent = {
                Text("Paseo #${walk.id}", style = MaterialTheme.typography.titleMedium)
            },
            supportingContent = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (walk.scheduledAt != null) {
                        Text("Fecha: ${walk.scheduledAt}", style = MaterialTheme.typography.bodyMedium)
                    }
                    val duration = walk.durationMinutes?.let { "$it min" } ?: "—"
                    Text("Duración: $duration", style = MaterialTheme.typography.bodyMedium)
                    if (!walk.notes.isNullOrBlank()) {
                        Text(
                            "Notas: ${walk.notes}",
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            trailingContent = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { onAccept(walk.id) }, enabled = actionsEnabled) {
                        Text("Aceptar")
                    }
                    OutlinedButton(onClick = { onReject(walk.id) }, enabled = actionsEnabled) {
                        Text("Rechazar")
                    }
                }
            }
        )
    }
}

@Composable
private fun AcceptedWalkItem(
    walk: Walk,
    onClick: () -> Unit,
    onStart: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        ListItem(
            headlineContent = {
                Text("Paseo #${walk.id}", style = MaterialTheme.typography.titleMedium)
            },
            supportingContent = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (walk.scheduledAt != null) {
                        Text("Fecha: ${walk.scheduledAt}", style = MaterialTheme.typography.bodyMedium)
                    }
                    val duration = walk.durationMinutes?.let { "$it min" } ?: "—"
                    Text("Duración: $duration", style = MaterialTheme.typography.bodyMedium)
                    if (!walk.notes.isNullOrBlank()) {
                        Text(
                            "Notas: ${walk.notes}",
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            trailingContent = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalButton(onClick = onStart) { Text("En curso") }
                    SuggestionChip(
                        onClick = onClick,
                        label = { Text("Ver detalle") },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        )
    }
}

@Composable
private fun HistoryWalkItem(
    walk: Walk
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        ListItem(
            headlineContent = {
                Text("Paseo #${walk.id}", style = MaterialTheme.typography.titleMedium)
            },
            supportingContent = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (walk.scheduledAt != null) {
                        Text("Fecha: ${walk.scheduledAt}", style = MaterialTheme.typography.bodyMedium)
                    }
                    val duration = walk.durationMinutes?.let { "$it min" } ?: "—"
                    Text("Duración: $duration", style = MaterialTheme.typography.bodyMedium)
                    if (!walk.notes.isNullOrBlank()) {
                        Text(
                            "Notas: ${walk.notes}",
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        )
    }
}
