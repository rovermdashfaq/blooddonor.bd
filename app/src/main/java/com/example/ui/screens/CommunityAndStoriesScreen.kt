package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.CommunityPost
import com.example.data.model.DonorEligibilityUtils
import com.example.data.model.SuccessStory
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.BloodGroupBadge
import com.example.ui.components.BloodGroupSelectorRow
import com.example.ui.components.sharePlainText

@Composable
fun CommunityAndStoriesScreen(
    currentUser: UserProfile,
    postsState: UiState<List<CommunityPost>>,
    storiesState: UiState<List<SuccessStory>>,
    onCreatePost: (text: String, imageUrl: String?, pollQuestion: String?, pollOptions: List<String>) -> Unit,
    onEditPost: (postId: String, text: String, imageUrl: String?) -> Unit = { _, _, _ -> },
    onTogglePostLike: (String) -> Unit,
    onAddComment: (postId: String, text: String) -> Unit,
    onVotePoll: (postId: String, optionId: String) -> Unit,
    onDeletePost: (String) -> Unit,
    onReportPost: (postId: String, reason: String) -> Unit,
    onCreateStory: (title: String, story: String, hospitalName: String, bloodGroup: String) -> Unit,
    onToggleStoryLike: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var showCreatePost by remember { mutableStateOf(false) }
    var editingPost by remember { mutableStateOf<CommunityPost?>(null) }
    var showCreateStory by remember { mutableStateOf(false) }
    var commentTargetPost by remember { mutableStateOf<CommunityPost?>(null) }
    var reportTargetPostId by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize()
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Community Feed") },
                    modifier = Modifier.testTag("community_feed_tab")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Success Stories") },
                    modifier = Modifier.testTag("success_stories_tab")
                )
            }

            if (selectedTab == 0) {
                when (postsState) {
                    UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    is UiState.Error -> Text(postsState.message, color = MaterialTheme.colorScheme.error)
                    is UiState.Success -> {
                        if (postsState.data.isEmpty()) {
                            EmptyCommunityCard(
                                title = "Share the first community update!",
                                subtitle = "Post blood awareness tips, donation moments, or community polls."
                            )
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(postsState.data, key = { it.id }) { post ->
                                    CommunityPostCard(
                                        post = post,
                                        myUid = currentUser.uid,
                                        onLike = { onTogglePostLike(post.id) },
                                        onComment = { commentTargetPost = post },
                                        onVote = { optId -> onVotePoll(post.id, optId) },
                                        onEdit = { editingPost = post },
                                        onShare = {
                                            val body = "${post.userName} (${post.bloodGroup ?: "Donor"}) posted on Blood Donor BD:\n\n${post.text.orEmpty()}"
                                            sharePlainText(context, "Blood Donor BD Community Post", body)
                                        },
                                        onDelete = { onDeletePost(post.id) },
                                        onReport = { reportTargetPostId = post.id }
                                    )
                                }
                                item { Spacer(Modifier.height(72.dp)) }
                            }
                        }
                    }
                }
            } else {
                when (storiesState) {
                    UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    is UiState.Error -> Text(storiesState.message, color = MaterialTheme.colorScheme.error)
                    is UiState.Success -> {
                        if (storiesState.data.isEmpty()) {
                            EmptyCommunityCard(
                                title = "No success stories published yet",
                                subtitle = "Share how a blood donation saved a life in Bangladesh."
                            )
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(storiesState.data, key = { it.id }) { story ->
                                    SuccessStoryCard(
                                        story = story,
                                        myUid = currentUser.uid,
                                        onLike = { onToggleStoryLike(story.id) },
                                        onShare = {
                                            val body = "${story.title} — by ${story.userName}\n\n${story.story}"
                                            sharePlainText(context, "Blood Donor BD Success Story", body)
                                        }
                                    )
                                }
                                item { Spacer(Modifier.height(72.dp)) }
                            }
                        }
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = {
                if (selectedTab == 0) showCreatePost = true else showCreateStory = true
            },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text(if (selectedTab == 0) "New Post" else "Share Story") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("community_create_fab")
        )
    }

    if (showCreatePost) {
        CreatePostDialog(
            onDismiss = { showCreatePost = false },
            onSubmit = { text, img, pq, opts ->
                onCreatePost(text, img, pq, opts)
                showCreatePost = false
            }
        )
    }

    editingPost?.let { post ->
        EditPostDialog(
            post = post,
            onDismiss = { editingPost = null },
            onSubmit = { newText, newImage ->
                onEditPost(post.id, newText, newImage)
                editingPost = null
            }
        )
    }

    if (showCreateStory) {
        CreateStoryDialog(
            defaultBloodGroup = currentUser.bloodGroup ?: "O+",
            onDismiss = { showCreateStory = false },
            onSubmit = { title, story, hosp, bg ->
                onCreateStory(title, story, hosp, bg)
                showCreateStory = false
            }
        )
    }

    commentTargetPost?.let { post ->
        val freshPost = (postsState as? UiState.Success)?.data?.firstOrNull { it.id == post.id } ?: post
        CommentsDialog(
            post = freshPost,
            onDismiss = { commentTargetPost = null },
            onAddComment = { commentText -> onAddComment(freshPost.id, commentText) }
        )
    }

    reportTargetPostId?.let { postId ->
        var reason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { reportTargetPostId = null },
            title = { Text("Report Post") },
            text = {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason (min 3 characters)") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReportPost(postId, reason)
                        reportTargetPostId = null
                    },
                    enabled = reason.trim().length >= 3
                ) { Text("Submit Report") }
            },
            dismissButton = {
                TextButton(onClick = { reportTargetPostId = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun EmptyCommunityCard(title: String, subtitle: String) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_community_care),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(14.dp))
            )
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CommunityPostCard(
    post: CommunityPost,
    myUid: String,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onVote: (String) -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onReport: () -> Unit
) {
    val isLiked = myUid in post.likedBy
    val comments = remember(post.comments) { post.parsedComments() }
    val poll = remember(post.poll) { post.parsedPoll() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    if (!post.bloodGroup.isNullOrBlank()) {
                        BloodGroupBadge(post.bloodGroup)
                        Spacer(Modifier.width(8.dp))
                    }
                    Column {
                        Text(post.userName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "${post.district ?: "Bangladesh"} • ${DonorEligibilityUtils.formatTimestamp(post.createdAt)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (post.userId == myUid) {
                        IconButton(onClick = onEdit, modifier = Modifier.testTag("edit_post_${post.id}")) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit post")
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.testTag("delete_post_${post.id}")) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete post")
                        }
                    } else {
                        IconButton(onClick = onReport, modifier = Modifier.testTag("report_post_${post.id}")) {
                            Icon(Icons.Default.Flag, contentDescription = "Report post")
                        }
                    }
                }
            }

            if (!post.text.isNullOrBlank()) {
                Text(post.text, style = MaterialTheme.typography.bodyMedium)
            }

            if (!post.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = post.imageUrl,
                    contentDescription = "Post image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }

            if (poll != null) {
                val totalVotes = poll.options.sumOf { it.voters.size }.coerceAtLeast(1)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Poll, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(poll.question, style = MaterialTheme.typography.labelLarge)
                        }
                        poll.options.forEach { opt ->
                            val voted = myUid in opt.voters
                            val fraction = opt.voters.size.toFloat() / totalVotes.toFloat()
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onVote(opt.id) }
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        opt.text + if (voted) " ✓" else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (voted) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text("${opt.voters.size}", style = MaterialTheme.typography.labelSmall)
                                }
                                LinearProgressIndicator(
                                    progress = { fraction },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TextButton(onClick = onLike, modifier = Modifier.testTag("like_post_${post.id}")) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like post",
                        tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("${post.likesCount}")
                }
                TextButton(onClick = onComment, modifier = Modifier.testTag("comment_post_${post.id}")) {
                    Icon(Icons.Default.Comment, contentDescription = "Comments")
                    Spacer(Modifier.width(4.dp))
                    Text("${comments.size}")
                }
                TextButton(onClick = onShare, modifier = Modifier.testTag("share_post_${post.id}")) {
                    Icon(Icons.Default.Share, contentDescription = "Share post")
                    Spacer(Modifier.width(4.dp))
                    Text("Share")
                }
            }
        }
    }
}

@Composable
private fun SuccessStoryCard(
    story: SuccessStory,
    myUid: String,
    onLike: () -> Unit,
    onShare: () -> Unit
) {
    val isLiked = myUid in story.likedBy
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(story.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (!story.bloodGroup.isNullOrBlank()) BloodGroupBadge(story.bloodGroup)
            }
            Text(
                "By ${story.userName}${if (!story.hospitalName.isNullOrBlank()) " • ${story.hospitalName}" else ""}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(story.story, style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onLike) {
                    Icon(
                        if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Appreciate story"
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("${story.likesCount} Appreciations")
                }
                TextButton(onClick = onShare, modifier = Modifier.testTag("share_story_${story.id}")) {
                    Icon(Icons.Default.Share, contentDescription = "Share story")
                    Spacer(Modifier.width(4.dp))
                    Text("Share")
                }
            }
        }
    }
}

@Composable
private fun EditPostDialog(
    post: CommunityPost,
    onDismiss: () -> Unit,
    onSubmit: (text: String, imageUrl: String?) -> Unit
) {
    var text by remember(post.id) { mutableStateOf(post.text.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Community Post") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Post text") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth().testTag("edit_post_input")
            )
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(text, post.imageUrl) },
                enabled = text.isNotBlank() || !post.imageUrl.isNullOrBlank(),
                modifier = Modifier.testTag("save_edit_post_button")
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun CreatePostDialog(
    onDismiss: () -> Unit,
    onSubmit: (text: String, imageUrl: String?, pollQuestion: String?, pollOptions: List<String>) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var includePoll by remember { mutableStateOf(false) }
    var pollQuestion by remember { mutableStateOf("") }
    var opt1 by remember { mutableStateOf("") }
    var opt2 by remember { mutableStateOf("") }
    var opt3 by remember { mutableStateOf("") }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            imageUrl = uri.toString().take(1000)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Community Post") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Share an update or awareness message") },
                    modifier = Modifier.fillMaxWidth().testTag("create_post_text"),
                    minLines = 3
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Pick Photo")
                    }
                    OutlinedButton(
                        onClick = { includePoll = !includePoll },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Poll, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (includePoll) "Hide Poll" else "Add Poll")
                    }
                }
                if (imageUrl.isNotBlank()) {
                    Text(
                        "Photo attached",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (includePoll) {
                    OutlinedTextField(
                        value = pollQuestion,
                        onValueChange = { pollQuestion = it },
                        label = { Text("Poll Question") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(value = opt1, onValueChange = { opt1 = it }, label = { Text("Option 1") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = opt2, onValueChange = { opt2 = it }, label = { Text("Option 2") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = opt3, onValueChange = { opt3 = it }, label = { Text("Option 3 (optional)") }, modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(
                        text,
                        imageUrl.ifBlank { null },
                        if (includePoll) pollQuestion else null,
                        if (includePoll) listOf(opt1, opt2, opt3) else emptyList()
                    )
                },
                modifier = Modifier.testTag("publish_post_button")
            ) { Text("Publish") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun CreateStoryDialog(
    defaultBloodGroup: String,
    onDismiss: () -> Unit,
    onSubmit: (title: String, story: String, hospitalName: String, bloodGroup: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var story by remember { mutableStateOf("") }
    var hospital by remember { mutableStateOf("") }
    var bloodGroup by remember { mutableStateOf(defaultBloodGroup) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Share Donation Success Story") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Story Title *") },
                    modifier = Modifier.fillMaxWidth().testTag("story_title_input")
                )
                BloodGroupSelectorRow(selectedGroup = bloodGroup, onSelect = { bloodGroup = it })
                OutlinedTextField(
                    value = hospital,
                    onValueChange = { hospital = it },
                    label = { Text("Hospital / Location") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = story,
                    onValueChange = { story = it },
                    label = { Text("Your Lifesaving Experience (min 10 chars) *") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("story_body_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(title, story, hospital, bloodGroup) },
                enabled = title.trim().length >= 3 && story.trim().length >= 10,
                modifier = Modifier.testTag("publish_story_button")
            ) { Text("Publish") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun CommentsDialog(
    post: CommunityPost,
    onDismiss: () -> Unit,
    onAddComment: (String) -> Unit
) {
    var draft by remember { mutableStateOf("") }
    val comments = remember(post.comments) { post.parsedComments() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Comments (${comments.size})") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(
                    modifier = Modifier
                        .heightIn(max = 260.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (comments.isEmpty()) {
                        Text("No comments yet. Be the first to encourage!")
                    } else {
                        comments.forEach { c ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(c.userName, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    Text(c.text, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    label = { Text("Write a comment") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val toSend = draft
                    draft = ""
                    onAddComment(toSend)
                },
                enabled = draft.isNotBlank()
            ) { Text("Send") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
