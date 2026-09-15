package com.noirsocial.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LockReset
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.FieldValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NoirApp() }
    }
}

enum class Tab { HOME, SEARCH, CREATE, MESSAGES, PROFILE }
enum class AuthMode { LOGIN, REGISTER, RESET }

data class UserProfile(val uid: String, val username: String, val email: String, val displayName: String = "", val bio: String = "")
data class Post(val id: String, val uid: String, val username: String, val text: String, val likes: Long)
data class ChatMessage(val id: String, val senderId: String, val text: String, val createdAt: Long)

fun db() = FirebaseFirestore.getInstance()
fun auth() = FirebaseAuth.getInstance()

@Composable
fun NoirTheme(content: @Composable () -> Unit) {
    val colors = androidx.compose.material3.darkColorScheme(
        background = Color(0xFF08080A), surface = Color(0xFF101014), surfaceVariant = Color(0xFF17171D),
        primary = Color(0xFFE7D7FF), secondary = Color(0xFFBDA6E8), onBackground = Color(0xFFF4F2F7),
        onSurface = Color(0xFFF4F2F7), outline = Color(0xFF35353D)
    )
    MaterialTheme(colorScheme = colors) { content() }
}

@Composable
fun NoirApp() {
    var user by remember { mutableStateOf(auth().currentUser) }
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { user = it.currentUser }
        auth().addAuthStateListener(listener)
        onDispose { auth().removeAuthStateListener(listener) }
    }
    NoirTheme {
        if (user == null) AuthScreen() else MainShell(user!!.uid)
    }
}

@Composable
fun AuthScreen() {
    var mode by remember { mutableStateOf(AuthMode.LOGIN) }
    Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0xFF1C1724), Color(0xFF08080A), Color(0xFF08080A))))) {
        Column(Modifier.fillMaxSize().padding(horizontal = 28.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(shape = RoundedCornerShape(24.dp), color = Color(0xFF111015).copy(alpha = .94f)) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.SmartToy, null, tint = Color(0xFFE7D7FF), modifier = Modifier.size(44.dp))
                    Spacer(Modifier.height(12.dp)); Text("NOIR", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text("Social, but darker.", color = Color(0xFFAAA7B2)); Spacer(Modifier.height(24.dp))
                    when (mode) {
                        AuthMode.LOGIN -> LoginForm { mode = it }
                        AuthMode.REGISTER -> RegisterForm { mode = it }
                        AuthMode.RESET -> ResetForm { mode = it }
                    }
                }
            }
        }
    }
}

@Composable
fun LoginForm(change: (AuthMode) -> Unit) {
    var email by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }; var error by remember { mutableStateOf("") }; var busy by remember { mutableStateOf(false) }
    OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Email") }, singleLine = true)
    Spacer(Modifier.height(10.dp)); OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("Password") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
    if (error.isNotBlank()) { Spacer(Modifier.height(8.dp)); Text(error, color = Color(0xFFFF8F9C)) }
    Spacer(Modifier.height(16.dp)); Button(modifier = Modifier.fillMaxWidth(), enabled = !busy, onClick = { busy = true; error = ""; auth().signInWithEmailAndPassword(email.trim(), password).addOnCompleteListener { task -> busy = false; if (!task.isSuccessful) error = task.exception?.localizedMessage ?: "Login failed" } }, shape = RoundedCornerShape(16.dp)) { Text(if (busy) "Signing in…" else "Login") }
    Spacer(Modifier.height(8.dp)); Text("Forgot password?", color = Color(0xFFD5C1FF), modifier = Modifier.clickable { change(AuthMode.RESET) })
    Spacer(Modifier.height(14.dp)); OutlinedButton(onClick = { change(AuthMode.REGISTER) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Text("Create account") }
}

@Composable
fun RegisterForm(change: (AuthMode) -> Unit) {
    var username by remember { mutableStateOf("") }; var email by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }; var error by remember { mutableStateOf("") }; var busy by remember { mutableStateOf(false) }
    OutlinedTextField(username, { username = it }, Modifier.fillMaxWidth(), label = { Text("Username") }, prefix = { Text("@") }, singleLine = true)
    Spacer(Modifier.height(10.dp)); OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Email") }, singleLine = true)
    Spacer(Modifier.height(10.dp)); OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("Password (6+ chars)") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
    if (error.isNotBlank()) { Spacer(Modifier.height(8.dp)); Text(error, color = Color(0xFFFF8F9C)) }
    Spacer(Modifier.height(16.dp)); Button(modifier = Modifier.fillMaxWidth(), enabled = !busy, onClick = {
        val u = username.trim().lowercase().replace(" ", "")
        if (!u.matches(Regex("[a-z0-9_]{3,20}"))) { error = "Username: 3-20 chars, a-z, 0-9, _"; return@Button }
        busy = true; error = ""
        auth().createUserWithEmailAndPassword(email.trim(), password).addOnCompleteListener { create ->
            if (!create.isSuccessful) { busy = false; error = create.exception?.localizedMessage ?: "Registration failed"; return@addOnCompleteListener }
            val uid = auth().currentUser!!.uid
            db().runTransaction { tx ->
                val ref = db().collection("usernames").document(u)
                if (tx.get(ref).exists()) throw IllegalStateException("Username is already taken")
                tx.set(ref, mapOf("uid" to uid, "username" to u))
                tx.set(db().collection("users").document(uid), mapOf("username" to u, "usernameLower" to u, "email" to email.trim(), "displayName" to u, "bio" to "", "createdAt" to FieldValue.serverTimestamp()), SetOptions.merge())
                null
            }.addOnCompleteListener { result ->
                busy = false
                if (!result.isSuccessful) { auth().currentUser?.delete(); error = result.exception?.localizedMessage ?: "Username could not be reserved" }
            }
        }
    }, shape = RoundedCornerShape(16.dp)) { Text(if (busy) "Creating…" else "Create account") }
    Spacer(Modifier.height(12.dp)); Text("Already have an account? Login", color = Color(0xFFD5C1FF), modifier = Modifier.clickable { change(AuthMode.LOGIN) })
}

@Composable
fun ResetForm(change: (AuthMode) -> Unit) {
    var email by remember { mutableStateOf("") }; var status by remember { mutableStateOf("") }; var error by remember { mutableStateOf("") }
    Text("Reset your password", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Spacer(Modifier.height(6.dp)); Text("Enter your email and Firebase will send a reset link.", color = Color(0xFFAAA7B2)); Spacer(Modifier.height(16.dp))
    OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Email") }, singleLine = true)
    if (status.isNotBlank()) { Spacer(Modifier.height(8.dp)); Text(status, color = Color(0xFFBFF2D8)) }; if (error.isNotBlank()) { Spacer(Modifier.height(8.dp)); Text(error, color = Color(0xFFFF8F9C)) }
    Spacer(Modifier.height(16.dp)); Button(onClick = { auth().sendPasswordResetEmail(email.trim()).addOnCompleteListener { if (it.isSuccessful) { status = "Reset email sent."; error = "" } else error = it.exception?.localizedMessage ?: "Could not send reset email" } }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Text("Send reset email") }
    Spacer(Modifier.height(12.dp)); Text("Back to login", color = Color(0xFFD5C1FF), modifier = Modifier.clickable { change(AuthMode.LOGIN) })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainShell(uid: String) {
    var tab by remember { mutableStateOf(Tab.HOME) }; var settings by remember { mutableStateOf(false) }; var profile by remember { mutableStateOf<UserProfile?>(null) }
    LaunchedEffect(uid) { db().collection("users").document(uid).addSnapshotListener { snap, _ -> if (snap != null && snap.exists()) profile = UserProfile(uid, snap.getString("username") ?: "", snap.getString("email") ?: "", snap.getString("displayName") ?: "", snap.getString("bio") ?: "") } }
    Scaffold(containerColor = Color(0xFF08080A), topBar = { TopAppBar(title = { Text(if (settings) "Settings" else "NOIR", fontWeight = FontWeight.Black) }, navigationIcon = { if (settings) IconButton({ settings = false }) { Icon(Icons.Outlined.ArrowBack, null) } }, actions = { if (!settings) IconButton({ settings = true }) { Icon(Icons.Outlined.Menu, null) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF08080A))) }, bottomBar = { if (!settings) BottomBar(tab) { tab = it } }) { padding ->
        AnimatedContent(targetState = if (settings) "settings" else tab.name, transitionSpec = { (fadeIn() + scaleIn()).togetherWith(fadeOut()) }, modifier = Modifier.padding(padding)) { screen -> if (screen == "settings") SettingsScreen() else when (tab) { Tab.HOME -> FeedScreen(); Tab.SEARCH -> SearchScreen(); Tab.CREATE -> CreateScreen(uid, profile?.username ?: ""); Tab.MESSAGES -> MessagesScreen(uid, profile?.username ?: ""); Tab.PROFILE -> ProfileScreen(uid, profile) } }
    }
}

@Composable fun BottomBar(tab: Tab, onTab: (Tab) -> Unit) { Surface(color = Color(0xFF0D0D11), tonalElevation = 3.dp) { Row(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceAround) { BottomItem(Icons.Outlined.Home, "Home", tab == Tab.HOME) { onTab(Tab.HOME) }; BottomItem(Icons.Outlined.Search, "Explore", tab == Tab.SEARCH) { onTab(Tab.SEARCH) }; BottomItem(Icons.Outlined.AddBox, "Create", tab == Tab.CREATE) { onTab(Tab.CREATE) }; BottomItem(Icons.Outlined.ChatBubbleOutline, "Messages", tab == Tab.MESSAGES) { onTab(Tab.MESSAGES) }; BottomItem(Icons.Outlined.Person, "Profile", tab == Tab.PROFILE) { onTab(Tab.PROFILE) } } } }
@Composable fun BottomItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, selected: Boolean, onClick: () -> Unit) { Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) { Icon(icon, label, tint = if (selected) Color(0xFFE7D7FF) else Color(0xFF78747F)); Text(label, style = MaterialTheme.typography.labelSmall, color = if (selected) Color(0xFFE7D7FF) else Color(0xFF78747F)) } }

@Composable
fun FeedScreen() {
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    DisposableEffect(Unit) { val reg = db().collection("posts").orderBy("createdAt", Query.Direction.DESCENDING).limit(50).addSnapshotListener { snap, _ -> posts = snap?.documents?.map { d -> Post(d.id, d.getString("uid") ?: "", d.getString("username") ?: "", d.getString("text") ?: "", d.getLong("likes") ?: 0) } ?: emptyList() }; onDispose { reg.remove() } }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { item { Spacer(Modifier.height(4.dp)); Text("For you", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("Live from Firebase", color = Color(0xFF77737E)) }; items(posts) { p -> PostCard(p) } }
}

@Composable fun PostCard(p: Post) { Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF111116)), shape = RoundedCornerShape(20.dp)) { Column(Modifier.padding(16.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(42.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFF312B3B), Color(0xFF151419)))), contentAlignment = Alignment.Center) { Text(p.username.take(1).uppercase(), fontWeight = FontWeight.Bold) }; Spacer(Modifier.width(10.dp)); Column { Text("@${p.username}", fontWeight = FontWeight.Bold); Text("Noir feed", style = MaterialTheme.typography.labelSmall, color = Color(0xFF77737E)) } }; Spacer(Modifier.height(14.dp)); Text(p.text, style = MaterialTheme.typography.bodyLarge); Spacer(Modifier.height(12.dp)); Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.FavoriteBorder, null, tint = Color(0xFFE7D7FF)); Spacer(Modifier.width(6.dp)); Text("${p.likes} likes") } } } }

@Composable
fun CreateScreen(uid: String, username: String) {
    var text by remember { mutableStateOf("") }; var status by remember { mutableStateOf("") }; var busy by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(18.dp)) { Text("Create post", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Spacer(Modifier.height(18.dp)); OutlinedTextField(text, { if (it.length <= 500) text = it }, Modifier.fillMaxWidth().height(180.dp), label = { Text("What's on your mind?") }); Spacer(Modifier.height(14.dp)); if (status.isNotBlank()) Text(status, color = Color(0xFFBFF2D8)); Button(modifier = Modifier.fillMaxWidth(), enabled = text.isNotBlank() && !busy, onClick = { busy = true; db().collection("posts").add(mapOf("uid" to uid, "username" to username, "text" to text.trim(), "likes" to 0L, "createdAt" to FieldValue.serverTimestamp())).addOnCompleteListener { busy = false; if (it.isSuccessful) { text = ""; status = "Posted ✓" } else status = it.exception?.localizedMessage ?: "Post failed" } }, shape = RoundedCornerShape(16.dp)) { Text(if (busy) "Posting…" else "Publish") } }
}

@Composable
fun SearchScreen() {
    var username by remember { mutableStateOf("") }; var result by remember { mutableStateOf<UserProfile?>(null) }; var status by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(18.dp)) { Text("Find people", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Spacer(Modifier.height(14.dp)); OutlinedTextField(username, { username = it }, Modifier.fillMaxWidth(), label = { Text("Username") }, prefix = { Text("@") }, singleLine = true); Spacer(Modifier.height(10.dp)); Button(modifier = Modifier.fillMaxWidth(), onClick = { val key = username.trim().lowercase(); if (key.isBlank()) return@Button; db().collection("usernames").document(key).get().addOnSuccessListener { snap -> val foundUid = snap.getString("uid"); if (foundUid == null) { result = null; status = "User not found" } else db().collection("users").document(foundUid).get().addOnSuccessListener { u -> result = UserProfile(foundUid, u.getString("username") ?: key, u.getString("email") ?: "", u.getString("displayName") ?: key, u.getString("bio") ?: ""); status = "" } } }) { Text("Search") }; Spacer(Modifier.height(20.dp)); if (status.isNotBlank()) Text(status, color = Color(0xFFFF8F9C)); result?.let { UserCard(it) } }
}

@Composable fun UserCard(u: UserProfile) { Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF111116)), shape = RoundedCornerShape(18.dp)) { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(54.dp).clip(CircleShape).background(Color(0xFF28232F)), contentAlignment = Alignment.Center) { Text(u.username.take(1).uppercase(), fontWeight = FontWeight.Bold) }; Spacer(Modifier.width(12.dp)); Column { Text("@${u.username}", fontWeight = FontWeight.Bold); Text(u.bio.ifBlank { "No bio yet." }, color = Color(0xFFAAA7B2)) } } } }

@Composable
fun MessagesScreen(uid: String, myUsername: String) {
    var target by remember { mutableStateOf("") }; var other by remember { mutableStateOf<UserProfile?>(null) }; var error by remember { mutableStateOf("") }
    if (other == null) { Column(Modifier.fillMaxSize().padding(18.dp)) { Text("Messages", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Spacer(Modifier.height(10.dp)); Text("Start a chat by entering a username.", color = Color(0xFFAAA7B2)); Spacer(Modifier.height(18.dp)); OutlinedTextField(target, { target = it }, Modifier.fillMaxWidth(), label = { Text("Username") }, prefix = { Text("@") }, singleLine = true); Spacer(Modifier.height(10.dp)); Button(modifier = Modifier.fillMaxWidth(), onClick = { val key = target.trim().lowercase(); db().collection("usernames").document(key).get().addOnSuccessListener { snap -> val id = snap.getString("uid"); if (id == null || id == uid) error = "User not found or invalid" else db().collection("users").document(id).get().addOnSuccessListener { u -> other = UserProfile(id, u.getString("username") ?: key, u.getString("email") ?: "", u.getString("displayName") ?: key) } } }) { Text("Open chat") }; if (error.isNotBlank()) Text(error, color = Color(0xFFFF8F9C)) } } else ChatScreen(uid, myUsername, other!!, onBack = { other = null })
}

fun chatId(a: String, b: String) = listOf(a, b).sorted().joinToString("_")

@Composable
fun ChatScreen(uid: String, myUsername: String, other: UserProfile, onBack: () -> Unit) {
    val cid = chatId(uid, other.uid); var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }; var text by remember { mutableStateOf("") }
    DisposableEffect(cid) { val conversation = db().collection("conversations").document(cid); conversation.set(mapOf("members" to listOf(uid, other.uid), "memberNames" to mapOf(uid to myUsername, other.uid to other.username)), SetOptions.merge()); val reg = conversation.collection("messages").orderBy("createdAt", Query.Direction.ASCENDING).limitToLast(100).addSnapshotListener { snap, _ -> messages = snap?.documents?.map { d -> ChatMessage(d.id, d.getString("senderId") ?: "", d.getString("text") ?: "", d.getTimestamp("createdAt")?.toDate()?.time ?: 0) } ?: emptyList() }; onDispose { reg.remove() } }
    Column(Modifier.fillMaxSize()) { Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onBack) { Icon(Icons.Outlined.ArrowBack, null) }; Text("@${other.username}", fontWeight = FontWeight.Bold) }; LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(messages) { m -> Row(Modifier.fillMaxWidth(), horizontalArrangement = if (m.senderId == uid) Arrangement.End else Arrangement.Start) { Surface(shape = RoundedCornerShape(16.dp), color = if (m.senderId == uid) Color(0xFF2A2334) else Color(0xFF15151A)) { Text(m.text, Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) } } } }; Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) { OutlinedTextField(text, { text = it }, Modifier.weight(1f), placeholder = { Text("Message…") }, singleLine = true); IconButton(onClick = { if (text.isNotBlank()) { db().collection("conversations").document(cid).collection("messages").add(mapOf("senderId" to uid, "text" to text.trim(), "createdAt" to FieldValue.serverTimestamp())); text = "" } }) { Icon(Icons.Outlined.Send, "Send") } } }
}

@Composable
fun ProfileScreen(uid: String, profile: UserProfile?) { Column(Modifier.fillMaxSize().padding(18.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(82.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFF312B3B), Color(0xFF151419)))), contentAlignment = Alignment.Center) { Text((profile?.username ?: "?").take(1).uppercase(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black) }; Spacer(Modifier.width(16.dp)); Column { Text("@${profile?.username ?: "loading"}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text(profile?.bio ?: "", color = Color(0xFFAAA7B2)) } }; Spacer(Modifier.height(28.dp)); Text("Your account is connected to Firebase.", color = Color(0xFFBFF2D8)); Spacer(Modifier.height(8.dp)); Text(profile?.email ?: "") } }

@Composable
fun SettingsScreen() { Column(Modifier.fillMaxSize().padding(18.dp)) { Text("Account", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Spacer(Modifier.height(12.dp)); SettingRow(Icons.Outlined.LockReset, "Reset password") { auth().currentUser?.email?.let { auth().sendPasswordResetEmail(it) } }; SettingRow(Icons.Outlined.Visibility, "Dark mode", null); SettingRow(Icons.Outlined.Settings, "Privacy & security", null); Spacer(Modifier.height(18.dp)); OutlinedButton(onClick = { auth().signOut() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Icon(Icons.Outlined.Logout, null); Spacer(Modifier.width(8.dp)); Text("Log out") } } }
@Composable fun SettingRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, action: (() -> Unit)?) { Row(Modifier.fillMaxWidth().clickable(enabled = action != null) { action?.invoke() }.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = Color(0xFFD5C1FF)); Spacer(Modifier.width(14.dp)); Text(title) } }
