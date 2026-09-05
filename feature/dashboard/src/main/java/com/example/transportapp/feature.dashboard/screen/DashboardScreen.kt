package com.example.transportapp.feature.dashboard.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.transportapp.core.designsystem.component.AppPrimaryButton
import com.example.transportapp.core.designsystem.component.NavDestination
import com.example.transportapp.core.designsystem.component.TransportBottomNavBar
import com.example.transportapp.core.designsystem.theme.HaulMotion
import com.example.transportapp.core.designsystem.theme.PlexMonoFamily
import com.example.transportapp.core.designsystem.theme.TransportTypeScale
import com.example.transportapp.core.designsystem.theme.transportColors
import com.example.transportapp.core.ui.AppNavDrawer
import com.example.transportapp.core.ui.DrawerDestination
import com.example.transportapp.core.ui.rememberAppDrawerState
import kotlinx.coroutines.launch

/**
 * Dashboard — the home screen. Exception strip above the numbers, 2-column tiles,
 * one sparkline. App shell with hamburger drawer, tiles navigate to detail screens.
 */
@Composable
fun DashboardScreen(
    onNewBilty: () -> Unit,
    onRegister: () -> Unit,
    onVehicles: () -> Unit,
    onReports: () -> Unit,
    onMasters: () -> Unit,
    onExports: () -> Unit,
    onSettings: () -> Unit,
    onAccountData: () -> Unit,
    onUnbilled: () -> Unit,
    onPayments: () -> Unit = {},
    onException: (String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    DashboardContent(
        state = state,
        onEvent = viewModel::onEvent,
        onNewBilty = onNewBilty,
        onRegister = onRegister,
        onVehicles = onVehicles,
        onReports = onReports,
        onMasters = onMasters,
        onExports = onExports,
        onSettings = onSettings,
        onAccountData = onAccountData,
        onUnbilled = onUnbilled,
        onPayments = onPayments,
        onException = onException,
    )
}

@Composable
fun DashboardContent(
    state: DashboardUiState,
    onEvent: (DashboardEvent) -> Unit,
    onNewBilty: () -> Unit,
    onRegister: () -> Unit,
    onVehicles: () -> Unit,
    onReports: () -> Unit,
    onMasters: () -> Unit,
    onExports: () -> Unit,
    onSettings: () -> Unit,
    onAccountData: () -> Unit,
    onUnbilled: () -> Unit,
    onPayments: () -> Unit = {},
    onException: (String) -> Unit
) {
    val drawerState = rememberAppDrawerState()
    val scope = rememberCoroutineScope()
    AppNavDrawer(
        drawerState = drawerState,
        companyInitials = state.companyInitials,
        companyName = state.companyName,
        branchName = state.branchName,
        activeDestination = DrawerDestination.HOME,
        onSelect = { destination ->
            scope.launch { drawerState.close() }
            when (destination) {
                DrawerDestination.REGISTER -> onRegister()
                DrawerDestination.VEHICLES -> onVehicles()
                DrawerDestination.REPORTS -> onReports()
                DrawerDestination.MASTERS -> onMasters()
                DrawerDestination.EXPORTS -> onExports()
                DrawerDestination.SETTINGS -> onSettings()
                DrawerDestination.ACCOUNT_DATA -> onAccountData()
                DrawerDestination.HOME -> Unit
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.fillMaxSize()) {
                DashboardTopBar(
                    initials = state.companyInitials,
                    companyName = state.companyName,
                    branchName = state.branchName,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onSettingsClick = onSettings
                )

                Text(
                    state.asOf,
                    style = TransportTypeScale.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    textAlign = TextAlign.End
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (state.isEmpty) {
                        item {
                            com.example.transportapp.core.designsystem.component.EmptyStateIllustrated(
                                title = "Nothing booked yet",
                                body = "Book your first bilty and its four copies print straight away. The dashboard fills in from there.",
                                buttonText = "Book the first bilty",
                                onButtonClick = onNewBilty,
                            )
                        }
                    } else {
                        // Hero money strip — the month's position at hero size on an elevated card
                        item { HeroMoneyStrip(state) }

                        // Exception cards with slide-in animation
                        item {
                            state.visibleExceptions.forEachIndexed { visibleIndex, exc ->
                                val index = state.exceptions.indexOfFirst { it == exc }
                                ExceptionCard(
                                    title = exc.title,
                                    body = exc.body,
                                    isLate = exc.isLate,
                                    hasAction = exc.biltyNo.isNotEmpty(),
                                    onCardClick = if (exc.biltyNo.isNotEmpty()) { { onException(exc.biltyNo) } } else null,
                                    onDismiss = { onEvent(DashboardEvent.DismissException(index)) }
                                )
                            }
                        }

                        // Dashboard tiles in 2-column grid with staggered animation
                        state.tiles.chunked(2).forEachIndexed { rowIndex, rowTiles ->
                            item {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    rowTiles.forEachIndexed { colIndex, tile ->
                                        DashboardTile(
                                            tile = tile,
                                            // S27: every tile lands somewhere real — the §13 rule
                                            // "never a number with a dead tap" now holds.
                                            onClick = when (tile.label) {
                                                "Unbilled freight" -> onUnbilled
                                                "Vehicles idle", "Running services", "In transit" -> onVehicles
                                                "Exceptions", "Booked today", "Receivable", "Overdue arrivals" -> onRegister
                                                "To Pay to collect" -> onPayments
                                                else -> null
                                            },
                                            staggerDelay = (rowIndex * 2 + colIndex) * 40,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (rowTiles.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // Extended FAB — floating above the bottom nav
            Box(modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 96.dp)) {
                AppPrimaryButton(
                    text = state.newBiltyLabel,
                    onClick = onNewBilty,
                    leadingIcon = Icons.Rounded.Add
                )
            }

            // Bottom navigation
            TransportBottomNavBar(
                destinations = listOf(
                    NavDestination("Home", Icons.Outlined.Home, Icons.Rounded.Home),
                    NavDestination("Register", Icons.Outlined.ListAlt, Icons.AutoMirrored.Rounded.ListAlt),
                    NavDestination("Vehicles", Icons.Outlined.LocalShipping, Icons.Rounded.LocalShipping)
                ),
                activeIndex = 0,
                onSelect = { index -> when (index) { 1 -> onRegister(); 2 -> onVehicles() } },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun DashboardTopBar(
    initials: String,
    companyName: String,
    branchName: String,
    onMenuClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onMenuClick) {
            Icon(Icons.Rounded.Menu, contentDescription = "Open menu", tint = MaterialTheme.colorScheme.onSurface)
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                initials,
                style = TransportTypeScale.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(companyName, style = TransportTypeScale.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(branchName, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onSettingsClick) {
            Icon(Icons.Rounded.Person, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ExceptionCard(
    title: String,
    body: String,
    isLate: Boolean,
    hasAction: Boolean,
    onCardClick: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val offset by animateFloatAsState(
        targetValue = if (appeared) 0f else 24f,
        animationSpec = HaulMotion.enterFloat(),
        label = "excOffset",
    )
    val alpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = HaulMotion.enterFloat(),
        label = "excAlpha",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { translationY = offset; this.alpha = alpha }
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .then(if (hasAction) Modifier.clickable { onCardClick?.invoke() } else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            if (isLate) Icons.Rounded.Schedule else Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = TransportTypeScale.titleSmall, color = MaterialTheme.colorScheme.onErrorContainer)
            Text(body, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f))
        }
        IconButton(onClick = onDismiss) {
            Icon(Icons.Rounded.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(20.dp))
        }
    }
}

/**
 * The hero money strip: this month's freight/hire/margin at hero size on an
 * elevated card, with a self-drawing sparkline beneath.
 */
@Composable
private fun HeroMoneyStrip(state: DashboardUiState) {
    com.example.transportapp.core.designsystem.component.ContentCard(
        modifier = Modifier.fillMaxWidth(),
        elevated = true,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "THIS MONTH",
                style = TransportTypeScale.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                state.thisMonthDelta,
                style = TransportTypeScale.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 100))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            state.thisMonthFigures.forEach { (value, label) ->
                Column(modifier = Modifier.weight(1f)) {
                    Text(value, style = TransportTypeScale.dataLarge, fontFamily = PlexMonoFamily, color = MaterialTheme.colorScheme.onSurface)
                    Text(label, style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            AnimatedSparkline(modifier = Modifier.width(96.dp).height(36.dp))
        }
    }
}

/** The sparkline draws itself left-to-right on open, with a soft primary fill. */
@Composable
private fun AnimatedSparkline(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val progress by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = HaulMotion.enterFloat(),
        label = "sparkDraw",
    )
    Canvas(modifier = modifier) {
        val points = listOf(0.85f, 0.75f, 0.8f, 0.6f, 0.7f, 0.5f, 0.55f, 0.35f, 0.45f, 0.25f)
        val step = size.width / (points.size - 1)
        val visiblePoints = (points.size * progress).toInt().coerceIn(1, points.size)
        if (visiblePoints > 1) {
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(0f, size.height)
                for (i in 0 until visiblePoints) {
                    lineTo(i * step, size.height * points[i])
                }
                lineTo((visiblePoints - 1) * step, size.height)
                close()
            }
            drawPath(path, primary.copy(alpha = 0.10f))
        }
        points.subList(1, visiblePoints).forEachIndexed { i, p ->
            drawLine(
                primary,
                start = Offset(i * step, size.height * points[i]),
                end = Offset((i + 1) * step, size.height * points[i + 1]),
                strokeWidth = 2.dp.toPx(),
            )
        }
        if (visiblePoints in 2..points.size) {
            drawCircle(primary, radius = 3.dp.toPx(), center = Offset((visiblePoints - 1) * step, size.height * points[visiblePoints - 1]))
        }
    }
}

@Composable
private fun DashboardTile(
    tile: DashTile,
    onClick: (() -> Unit)? = null,
    staggerDelay: Int = 0,
    modifier: Modifier = Modifier
) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(staggerDelay.toLong())
        appeared = true
    }
    val appearScale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.92f,
        animationSpec = HaulMotion.bouncy,
        label = "tileIn",
    )
    val appearAlpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = HaulMotion.enterFloat(),
        label = "tileAlpha",
    )
    Column(
        modifier = modifier
            .graphicsLayer { scaleX = appearScale; scaleY = appearScale; alpha = appearAlpha }
            .height(116.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            tile.label.uppercase(),
            style = TransportTypeScale.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            tile.value,
            style = if (tile.money) TransportTypeScale.dataLarge else TransportTypeScale.titleLarge,
            fontFamily = if (tile.money) PlexMonoFamily else null,
            color = if (tile.amberBar) transportColors().haulAmber else MaterialTheme.colorScheme.onSurface
        )
        Text(tile.qualifier, style = TransportTypeScale.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
