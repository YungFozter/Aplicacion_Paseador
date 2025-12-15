package com.example.aplicacionpaseador.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.aplicacionpaseador.data.RetrofitClient
import com.example.aplicacionpaseador.data.Review
import com.example.aplicacionpaseador.data.UserPreferences
import androidx.compose.ui.platform.LocalContext
import java.time.OffsetDateTime
import kotlinx.coroutines.launch

@Composable
fun ReviewsScreen(navController: NavController) {
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context) }
    val accessToken by userPreferences.accessToken.collectAsState(initial = null)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reviews by remember { mutableStateOf<List<Review>>(emptyList()) }

    LaunchedEffect(accessToken) {
        if (accessToken != null) {
            try {
                val resp = RetrofitClient.instance.getReviews("Bearer $accessToken")
                if (resp.isSuccessful) {
                    val list = resp.body() ?: emptyList()
                    // Ordenar por fecha descendente (más recientes primero)
                    reviews = list.sortedByDescending { parseDate(it.createdAt) }
                    error = null
                } else {
                    error = "Error reviews: ${resp.code()}"
                }
            } catch (e: Exception) {
                error = e.localizedMessage ?: "Error de red"
            } finally {
                isLoading = false
            }
        } else {
            isLoading = false
            error = "No autenticado"
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            Surface(shadowElevation = 2.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Mis Reviews", style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (error != null) {
                Text(text = error!!, color = MaterialTheme.colorScheme.error)
            } else if (reviews.isEmpty()) {
                Text("No hay reviews disponibles", style = MaterialTheme.typography.bodyMedium)
            } else {
                reviews.forEach { review ->
                    ReviewItem(review = review, onOpenWalk = { walkId ->
                        navController.navigate("paseos")
                        scope.launch {
                            snackbarHostState.showSnackbar("Abrir paseo #$walkId")
                        }
                    })
                }
            }
        }
    }
}

private fun parseDate(date: String?): Long {
    return try {
        if (date.isNullOrBlank()) 0L else OffsetDateTime.parse(date).toInstant().toEpochMilli()
    } catch (_: Exception) {
        0L
    }
}

@Composable
private fun ReviewItem(review: Review, onOpenWalk: (Int?) -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        ListItem(
            headlineContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Puntuación: ${review.rating ?: "—"}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
            },
            supportingContent = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (!review.text.isNullOrBlank()) {
                        val txt = review.text ?: ""
                        Text(txt, style = MaterialTheme.typography.bodyMedium)
                    }
                    val dateLabel = review.createdAt ?: "—"
                    Text("Fecha: $dateLabel", style = MaterialTheme.typography.bodySmall)
                }
            },
            trailingContent = {
                Text(
                    text = review.walkId?.let { "Ver paseo #$it" } ?: "",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onOpenWalk(review.walkId) }
                )
            }
        )
    }
}
