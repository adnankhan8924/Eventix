package com.tech.eventix.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.snapshotFlow
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.tech.eventix.uistate.EventUiState
import com.tech.eventix.uistate.EventsScreenUiState
import com.tech.eventix.viewmodel.EventViewModel
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.filled.Clear
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun EventsScreen(
    viewModel: EventViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.eventsScreenUiState.collectAsStateWithLifecycle()
    EventsScreenContent(uiState = uiState, modifier = modifier)
}

@Composable
fun EventsScreenContent(
    uiState: EventsScreenUiState,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is EventsScreenUiState.Loading -> LoadingState(modifier)
        is EventsScreenUiState.Error -> ErrorState(uiState.message, modifier)
        is EventsScreenUiState.Success -> {
            var searchQuery by remember { mutableStateOf("") }
            var shouldClearFocus by remember { mutableStateOf(false) }
            val focusRequester = remember { FocusRequester() }
            val focusManager = LocalFocusManager.current
            
            // Clear focus when shouldClearFocus is true
            LaunchedEffect(shouldClearFocus) {
                if (shouldClearFocus) {
                    focusManager.clearFocus()
                    shouldClearFocus = false
                }
            }
            
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .pointerInput(Unit) {
                        detectTapGestures {
                            // Clear focus when tapping outside search bar
                            focusManager.clearFocus()
                        }
                    }
            ) {
                // Event list (scrolls under the search bar)
                EventsList(
                    events = uiState.events,
                    isLoadingMore = uiState.isLoadingMore,
                    paginationError = uiState.paginationError,
                    onLoadMore = { uiState.onLoadNextPage() },
                    modifier = Modifier
                        .fillMaxSize()
                )
                // Sticky search bar
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearchWithFocusClear = { query -> 
                        uiState.onSearch(query)
                        shouldClearFocus = true
                    },
                    onFieldTapped = {
                        // Clear text when tapping field, but don't reset search results
                        searchQuery = ""
                    },
                    onClear = {
                        searchQuery = ""
                        uiState.onSearch("")
                        shouldClearFocus = true
                    },
                    onFocusRequest = {
                        focusRequester.requestFocus()
                    },
                    focusRequester = focusRequester,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, top = 12.dp)
                        .align(Alignment.TopCenter)
                )
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchWithFocusClear: (String) -> Unit,
    onFieldTapped: () -> Unit,
    onClear: () -> Unit,
    onFocusRequest: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .focusRequester(focusRequester)
            .clickable {
                if (query.isNotEmpty()) {
                    // If there's text, clear it but keep search results
                    onFieldTapped()
                }
                onFocusRequest()
            },
        placeholder = { Text("Search events...") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                modifier = Modifier.clickable { 
                    if (query.isNotEmpty()) {
                        onSearchWithFocusClear(query)
                    } else {
                        onFocusRequest()
                    }
                }
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Clear search",
                    modifier = Modifier.clickable { 
                        onClear() // This resets search results
                    }
                )
            }
        },
        singleLine = true,
        maxLines = 1,
        shape = RoundedCornerShape(32.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = Color.White.copy(alpha = 0.9f),
            focusedContainerColor = Color.White.copy(alpha = 0.9f),
            disabledContainerColor = Color.White.copy(alpha = 0.9f),
            errorContainerColor = Color.White.copy(alpha = 0.9f)
        ),
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Search
        ),
        keyboardActions = KeyboardActions(
            onSearch = { onSearchWithFocusClear(query) } // Enter key triggers search and removes focus
        )
    )
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading events...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorState(message: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "Error",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { /* TODO: Add retry functionality */ },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Retry",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retry")
            }
        }
    }
}

@OptIn(FlowPreview::class)
@Composable
fun EventsList(
    events: List<EventUiState>,
    isLoadingMore: Boolean,
    paginationError: String?,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItemsCount = layoutInfo.totalItemsCount
            Pair(lastVisibleItemIndex, totalItemsCount)
        }.distinctUntilChanged()
            .debounce(1000)
            .collect { (lastVisibleItemIndex, totalItemsCount) ->

                val thresholdPercent = 0.7f // Trigger when 70% of the list is visible
                val isNearEnd = if (totalItemsCount > 0) {
                    lastVisibleItemIndex >= (totalItemsCount * thresholdPercent).toInt()
                } else {
                    false
                }
                val isNotLoading = !isLoadingMore
                val hasNoError = paginationError == null

                if (isNearEnd && isNotLoading && hasNoError) {
                    onLoadMore()
                }
            }
    }
    
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(top = 72.dp),
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(events) { event ->
            EventCard(event)
        }
        if (isLoadingMore) {
            item {
                LoadingMoreUi()
            }
        }
        if (paginationError != null) {
            item {
                PaginationErrorUi(paginationError, onLoadMore)
            }
        }
    }
}

@Composable
private fun LoadingMoreUi() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Loading more events...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PaginationErrorUi(error: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable { onRetry() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Failed to load more events",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { onRetry() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Retry",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retry")
            }
        }
    }
}

@Composable
fun EventCard(event: EventUiState, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Image(
                painter = rememberAsyncImagePainter(event.image),
                contentDescription = event.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = event.name,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = event.dateTime,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = event.location,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewEventsScreenContent_Success() {
    EventsScreenContent(
        uiState = EventsScreenUiState.Success(
            events = listOf(
                EventUiState(
                    name = "Sample Event Name",
                    image = "https://placehold.co/600x400",
                    dateTime = "Sat, 19 July, 7:30 pm",
                    location = "Yankee Stadium, Bronx"
                ),
                EventUiState(
                    name = "Another Event",
                    image = "https://placehold.co/600x400",
                    dateTime = "Sun, 20 July, 8:00 pm",
                    location = "Astros Park, Houston"
                )
            ),
            page = 0,
            onLoadNextPage = {},
            isLoadingMore = false,
            paginationError = null,
            onSearch = {}
        )
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewEventsScreenContent_Error() {
    EventsScreenContent(
        uiState = EventsScreenUiState.Error("Something went wrong loading events.")
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewEventsScreenContent_Loading() {
    EventsScreenContent(
        uiState = EventsScreenUiState.Loading
    )
} 