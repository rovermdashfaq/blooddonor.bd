package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.DmConversation
import com.example.data.model.DmMessage
import com.example.data.model.DonorEligibilityUtils
import com.example.data.model.DonorSummary
import com.example.data.model.GroupChatMessage
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.BloodGroupBadge

@Composable
fun MessagesScreen(
    currentUser: UserProfile,
    effectiveRole: String,
    donorsState: UiState<List<DonorSummary>>,
    conversationsState: UiState<List<DmConversation>>,
    selectedConversationId: String?,
    dmMessagesState: UiState<List<DmMessage>>,
    groupChatState: UiState<List<GroupChatMessage>>,
    onSelectConversation: (convId: String?, peerUid: String?) -> Unit,
    onStartDmWithDonor: (DonorSummary) -> Unit,
    onSendDm: (convId: String, peerUid: String, peerName: String, peerBg: String?, text: String) -> Unit,
    onSendGroupChat: (text: String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showNewChatPicker by remember { mutableStateOf(false) }

    if (selectedTab == 0 && !selectedConversationId.isNullOrBlank()) {
        BackHandler { onSelectConversation(null, null) }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Direct Messages") },
                    modifier = Modifier.testTag("dm_inbox_tab")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Group Chat") },
                    modifier = Modifier.testTag("community_chat_tab")
                )
            }

            if (selectedTab == 0) {
                val conversations = (conversationsState as? UiState.Success)?.data ?: emptyList()
                val activeConv = conversations.firstOrNull { it.id == selectedConversationId }

                if (activeConv != null) {
                    val peerUid = activeConv.participantIds.firstOrNull { it != currentUser.uid }.orEmpty()
                    val peerName = activeConv.participantNames[peerUid] ?: "Donor"
                    val peerBg = activeConv.participantBloodGroups[peerUid]

                    DmThreadView(
                        myUid = currentUser.uid,
                        peerName = peerName,
                        peerBloodGroup = peerBg,
                        messagesState = dmMessagesState,
                        onBack = { onSelectConversation(null, null) },
                        onSend = { msg -> onSendDm(activeConv.id, peerUid, peerName, peerBg, msg) },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Private Conversations", style = MaterialTheme.typography.titleMedium)
                        OutlinedButton(
                            onClick = { showNewChatPicker = true },
                            modifier = Modifier.heightIn(min = 48.dp).testTag("new_dm_button")
                        ) {
                            Icon(Icons.Default.AddComment, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("New Chat")
                        }
                    }

                    when (conversationsState) {
                        UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                        is UiState.Error -> Text(conversationsState.message, color = MaterialTheme.colorScheme.error)
                        is UiState.Success -> {
                            if (conversationsState.data.isEmpty()) {
                                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("No direct messages yet.", style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "Tap 'New Chat' or message any donor from the directory.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    items(conversationsState.data, key = { it.id }) { conv ->
                                        val peerUid = conv.participantIds.firstOrNull { it != currentUser.uid }.orEmpty()
                                        val peerName = conv.participantNames[peerUid] ?: "Donor"
                                        val peerBg = conv.participantBloodGroups[peerUid]
                                        val unread = conv.unreadCounts[currentUser.uid] ?: 0L

                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { onSelectConversation(conv.id, peerUid) }
                                                .testTag("dm_conv_${conv.id}"),
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                BloodGroupBadge(peerBg ?: "BD")
                                                Spacer(Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(peerName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                                    Text(
                                                        conv.lastMessage.ifBlank { "Conversation opened" },
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                if (unread > 0L) {
                                                    Surface(
                                                        shape = RoundedCornerShape(50),
                                                        color = MaterialTheme.colorScheme.primary
                                                    ) {
                                                        Text(
                                                            unread.toString(),
                                                            color = MaterialTheme.colorScheme.onPrimary,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                CommunityGroupChatView(
                    myUid = currentUser.uid,
                    myRole = effectiveRole,
                    chatState = groupChatState,
                    onSend = onSendGroupChat,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (showNewChatPicker) {
        val donors = (donorsState as? UiState.Success)?.data?.filter { it.uid != currentUser.uid } ?: emptyList()
        var donorSearchQuery by remember { mutableStateOf("") }
        val filteredDonors = remember(donors, donorSearchQuery) {
            val q = donorSearchQuery.trim()
            if (q.isEmpty()) donors
            else donors.filter {
                it.fullName.contains(q, ignoreCase = true) ||
                    it.bloodGroup.contains(q, ignoreCase = true) ||
                    it.district.contains(q, ignoreCase = true) ||
                    (it.donorId?.contains(q, ignoreCase = true) == true)
            }
        }
        AlertDialog(
            onDismissRequest = { showNewChatPicker = false },
            title = { Text("Select Verified Donor to Message") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = donorSearchQuery,
                        onValueChange = { donorSearchQuery = it },
                        placeholder = { Text("Search name, blood group, district") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dm_donor_search_input")
                    )
                    if (filteredDonors.isEmpty()) {
                        Text(
                            text = if (donors.isEmpty()) "No other verified donors available yet."
                            else "No matching donors found.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 300.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filteredDonors, key = { it.uid }) { donor ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            showNewChatPicker = false
                                            onStartDmWithDonor(donor)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        BloodGroupBadge(donor.bloodGroup)
                                        Spacer(Modifier.width(10.dp))
                                        Column {
                                            Text(donor.fullName, style = MaterialTheme.typography.titleSmall)
                                            Text(donor.district, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNewChatPicker = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun DmThreadView(
    myUid: String,
    peerName: String,
    peerBloodGroup: String?,
    messagesState: UiState<List<DmMessage>>,
    onBack: () -> Unit,
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var draft by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("dm_back_button")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to conversations")
            }
            if (!peerBloodGroup.isNullOrBlank()) {
                BloodGroupBadge(peerBloodGroup)
                Spacer(Modifier.width(8.dp))
            }
            Text(peerName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        when (messagesState) {
            UiState.Loading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is UiState.Error -> Text(messagesState.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
            is UiState.Success -> {
                LaunchedEffect(messagesState.data.size) {
                    if (messagesState.data.isNotEmpty()) {
                        listState.animateScrollToItem(messagesState.data.lastIndex)
                    }
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messagesState.data, key = { it.id }) { msg ->
                        val isMine = msg.senderId == myUid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.widthIn(max = 300.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        msg.message,
                                        color = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        DonorEligibilityUtils.formatTimestamp(msg.createdAt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isMine) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        val quickReplies = remember {
            listOf(
                "I'm available to donate",
                "Which hospital & ward?",
                "Sharing my Donor ID",
                "On my way"
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickReplies.forEach { reply ->
                AssistChip(
                    onClick = { onSend(reply) },
                    label = { Text(reply) }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text("Type private message...") },
                modifier = Modifier.weight(1f).testTag("dm_message_input"),
                maxLines = 3
            )
            Spacer(Modifier.width(8.dp))
            FilledIconButton(
                onClick = {
                    val toSend = draft
                    draft = ""
                    onSend(toSend)
                },
                enabled = draft.isNotBlank(),
                modifier = Modifier.size(48.dp).testTag("dm_send_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send direct message")
            }
        }
    }
}

@Composable
private fun CommunityGroupChatView(
    myUid: String,
    myRole: String,
    chatState: UiState<List<GroupChatMessage>>,
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var draft by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Forum, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text("Bangladesh Donor Community Room (Role: $myRole)", style = MaterialTheme.typography.labelLarge)
        }

        when (chatState) {
            UiState.Loading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is UiState.Error -> Text(chatState.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
            is UiState.Success -> {
                LaunchedEffect(chatState.data.size) {
                    if (chatState.data.isNotEmpty()) {
                        listState.animateScrollToItem(chatState.data.lastIndex)
                    }
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(chatState.data, key = { it.id }) { msg ->
                        val isMine = msg.userId == myUid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isMine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.widthIn(max = 320.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        "${msg.userName}${if (!msg.bloodGroup.isNullOrBlank()) " (${msg.bloodGroup})" else ""} • ${msg.userRole}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(msg.message, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        DonorEligibilityUtils.formatTimestamp(msg.createdAt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text("Message community chat...") },
                modifier = Modifier.weight(1f).testTag("group_chat_input"),
                maxLines = 3
            )
            Spacer(Modifier.width(8.dp))
            FilledIconButton(
                onClick = {
                    val toSend = draft
                    draft = ""
                    onSend(toSend)
                },
                enabled = draft.isNotBlank(),
                modifier = Modifier.size(48.dp).testTag("group_chat_send_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send group message")
            }
        }
    }
}
