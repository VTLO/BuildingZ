package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Scaffold(
          modifier = Modifier
            .fillMaxSize()
            .testTag("main_scaffold"),
          containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
          GitCompanionDashboard(
            modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding)
          )
        }
      }
    }
  }
}

// Data Classes & Scenarios
enum class AppTab {
  BUILD, GUIDES, LINT
}

data class CommitTypeInfo(
  val type: String,
  val label: String,
  val icon: ImageVector,
  val description: String
)

data class GuideScenario(
  val id: String,
  val title: String,
  val summary: String,
  val danger: Boolean = false,
  val explanation: String,
  val diagram: String,
  val commands: List<String>,
  val tips: List<String>
)

data class LintResult(
  val ruleName: String,
  val status: RuleStatus,
  val message: String
)

enum class RuleStatus {
  PASS, WARN, FAIL, INFO
}

@Composable
fun GitCompanionDashboard(modifier: Modifier = Modifier) {
  var activeTab by rememberSaveable { mutableStateOf(AppTab.BUILD) }
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  // Helper to copy to clipboard
  val copyToClipboard: (String, String) -> Unit = { text, label ->
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("GitCompanion", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "$label copied to clipboard!", Toast.LENGTH_SHORT).show()
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    // Header Section (Terminal Prompt style)
    TerminalHeader()

    // Interactive Tab Bar / Chips Group
    TabChipsRow(
      selectedTab = activeTab,
      onTabSelected = { activeTab = it }
    )

    HorizontalDivider(
      color = MaterialTheme.colorScheme.surfaceVariant,
      thickness = 1.dp,
      modifier = Modifier.padding(horizontal = 16.dp)
    )

    // Main Tab Content using a Crossfade animation for clean screen switches
    Crossfade(
      targetState = activeTab,
      animationSpec = tween(durationMillis = 250),
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
    ) { tab ->
      when (tab) {
        AppTab.BUILD -> CommitBuilderScreen(onCopy = copyToClipboard)
        AppTab.GUIDES -> GuidesScreen(onCopy = copyToClipboard)
        AppTab.LINT -> LinterScreen(onCopy = copyToClipboard)
      }
    }
  }
}

@Composable
fun TerminalHeader() {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Glowing Status Indicator dot
      Box(
        modifier = Modifier
          .size(10.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.tertiary)
      )
      
      Spacer(modifier = Modifier.width(12.dp))
      
      Column {
        Text(
          text = "git-companion --status=online",
          fontFamily = FontFamily.Monospace,
          fontSize = 13.sp,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "Git Workflow & Commit Specialist",
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      
      Spacer(modifier = Modifier.weight(1f))
      
      Text(
        text = "v1.2.0",
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier
          .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
          .padding(horizontal = 8.dp, vertical = 2.dp)
      )
    }
  }
}

@Composable
fun TabChipsRow(
  selectedTab: AppTab,
  onTabSelected: (AppTab) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    AppTab.values().forEach { tab ->
      val isSelected = selectedTab == tab
      val (label, icon) = when (tab) {
        AppTab.BUILD -> "Build Commit" to Icons.Default.Build
        AppTab.GUIDES -> "Quick Guides" to Icons.Default.Book
        AppTab.LINT -> "Lint Commit" to Icons.Default.Done
      }

      val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.primary,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
        containerColor = MaterialTheme.colorScheme.surface,
        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        iconColor = MaterialTheme.colorScheme.onSurfaceVariant
      )

      FilterChip(
        selected = isSelected,
        onClick = { onTabSelected(tab) },
        label = { Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
        leadingIcon = {
          Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
          )
        },
        colors = chipColors,
        border = FilterChipDefaults.filterChipBorder(
          enabled = true,
          selected = isSelected,
          borderColor = MaterialTheme.colorScheme.surfaceVariant,
          selectedBorderColor = MaterialTheme.colorScheme.primary
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .weight(1f)
          .padding(horizontal = 4.dp)
          .testTag("tab_chip_${tab.name.lowercase()}")
      )
    }
  }
}

// ---------------------- TAB 1: COMMIT BUILDER ----------------------

@Composable
fun CommitBuilderScreen(onCopy: (String, String) -> Unit) {
  val commitTypes = remember {
    listOf(
      CommitTypeInfo("feat", "Features", Icons.Default.Add, "New feature addition"),
      CommitTypeInfo("fix", "Bug Fixes", Icons.Default.Build, "Bug resolution"),
      CommitTypeInfo("chore", "Chore", Icons.Default.Refresh, "Maintain & configure project"),
      CommitTypeInfo("docs", "Docs", Icons.Default.Info, "Documentation adjustments"),
      CommitTypeInfo("refactor", "Refactor", Icons.Default.Settings, "Refactoring source code"),
      CommitTypeInfo("style", "Style", Icons.Default.Star, "Formatting, whitespace changes"),
      CommitTypeInfo("test", "Tests", Icons.Default.Check, "Add/modify test suite")
    )
  }

  var selectedType by rememberSaveable { mutableStateOf("feat") }
  var scope by rememberSaveable { mutableStateOf("") }
  var subject by rememberSaveable { mutableStateOf("") }
  var body by rememberSaveable { mutableStateOf("") }
  var footer by rememberSaveable { mutableStateOf("") }

  // Generate formatted conventional commit
  val formattedCommit = remember(selectedType, scope, subject, body, footer) {
    val scopeString = if (scope.isNotBlank()) "(${scope.trim().lowercase()})" else ""
    val header = "$selectedType$scopeString: ${subject.trim().lowercase()}"
    buildString {
      append(header)
      if (body.isNotBlank()) {
        append("\n\n")
        append(body.trim())
      }
      if (footer.isNotBlank()) {
        append("\n\n")
        append(footer.trim())
      }
    }
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    contentPadding = PaddingValues(bottom = 24.dp)
  ) {
    item {
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "1. Select Commit Type",
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(bottom = 8.dp)
      )
    }

    item {
      // Flowable list of type selection chips
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Box(modifier = Modifier.fillMaxWidth()) {
          // Horizontal scrolling list of chips
          Row(
            modifier = Modifier.horizontalScroll(rememberScrollState())
          ) {
            commitTypes.forEach { typeInfo ->
              val isSelected = selectedType == typeInfo.type
              FilterChip(
                selected = isSelected,
                onClick = { selectedType = typeInfo.type },
                label = { Text(typeInfo.type, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                  selectedLabelColor = MaterialTheme.colorScheme.primary,
                  containerColor = MaterialTheme.colorScheme.surface,
                  labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = FilterChipDefaults.filterChipBorder(
                  enabled = true,
                  selected = isSelected,
                  borderColor = MaterialTheme.colorScheme.surfaceVariant,
                  selectedBorderColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                  .padding(end = 6.dp)
                  .testTag("commit_type_${typeInfo.type}")
              )
            }
          }
        }
      }
    }

    item {
      // Scope and Subject fields in a column
      Text(
        text = "2. Commit Details",
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(bottom = 8.dp)
      )

      OutlinedTextField(
        value = scope,
        onValueChange = { scope = it },
        label = { Text("Scope (Optional)") },
        placeholder = { Text("e.g. auth, api, ui") },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = MaterialTheme.colorScheme.primary,
          unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        keyboardOptions = KeyboardOptions(
          capitalization = KeyboardCapitalization.None,
          imeAction = ImeAction.Next
        ),
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 12.dp)
          .testTag("input_commit_scope")
      )

      OutlinedTextField(
        value = subject,
        onValueChange = { subject = it },
        label = { Text("Subject / Title") },
        placeholder = { Text("e.g. add google login provider") },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = MaterialTheme.colorScheme.primary,
          unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        keyboardOptions = KeyboardOptions(
          capitalization = KeyboardCapitalization.None,
          imeAction = ImeAction.Next
        ),
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 12.dp)
          .testTag("input_commit_subject")
      )

      OutlinedTextField(
        value = body,
        onValueChange = { body = it },
        label = { Text("Body Description (Optional)") },
        placeholder = { Text("Explain what changes were made and why.") },
        minLines = 3,
        maxLines = 5,
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = MaterialTheme.colorScheme.primary,
          unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        keyboardOptions = KeyboardOptions(
          capitalization = KeyboardCapitalization.Sentences,
          imeAction = ImeAction.Next
        ),
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 12.dp)
          .testTag("input_commit_body")
      )

      OutlinedTextField(
        value = footer,
        onValueChange = { footer = it },
        label = { Text("Footer (Optional)") },
        placeholder = { Text("e.g. Closes #104, Fixes #82") },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = MaterialTheme.colorScheme.primary,
          unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        keyboardOptions = KeyboardOptions(
          capitalization = KeyboardCapitalization.None,
          imeAction = ImeAction.Done
        ),
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 20.dp)
          .testTag("input_commit_footer")
      )
    }

    item {
      // Live Preview Section
      Text(
        text = "3. Live Commit Preview",
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(bottom = 8.dp)
      )

      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(
          containerColor = Color(0xFF07080A) // Terminal pitch black
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .clip(CircleShape)
                  .background(Color(0xFFFF5F56)) // Red terminal dot
              )
              Spacer(modifier = Modifier.width(6.dp))
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .clip(CircleShape)
                  .background(Color(0xFFFFBD2E)) // Yellow terminal dot
              )
              Spacer(modifier = Modifier.width(6.dp))
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .clip(CircleShape)
                  .background(Color(0xFF27C93F)) // Green terminal dot
              )
            }

            Text(
              text = "COMMIT_EDITMSG",
              fontFamily = FontFamily.Monospace,
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          // Format check validation warning block
          val showPlaceholder = subject.isBlank()
          val displayMessage = if (showPlaceholder) {
            "$selectedType${if (scope.isNotBlank()) "($scope)" else ""}: <your subject goes here>"
          } else {
            formattedCommit
          }

          Text(
            text = displayMessage,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            color = if (showPlaceholder) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else Color(0xFFE5E9F0),
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 8.dp)
              .testTag("commit_preview_text")
          )

          Spacer(modifier = Modifier.height(12.dp))

          Button(
            onClick = { onCopy(formattedCommit, "Commit message") },
            enabled = subject.isNotBlank(),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier
              .align(Alignment.End)
              .testTag("btn_copy_commit")
          ) {
            Icon(
              imageVector = Icons.Default.ContentCopy,
              contentDescription = null,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Copy Message", fontSize = 13.sp)
          }
        }
      }
    }
  }
}



// ---------------------- TAB 2: QUICK GUIDES ----------------------

@Composable
fun GuidesScreen(onCopy: (String, String) -> Unit) {
  val guides = remember {
    listOf(
      GuideScenario(
        id = "undo_keep",
        title = "Undo Last Commit (Keep Changes)",
        summary = "Safely undoes your last commit, leaving your edited files intact and ready to recommit.",
        danger = false,
        explanation = "Moves HEAD back one commit, but preserves all file changes in your working directory as 'unstaged'. Perfect when you made a typo or forgot to include a file.",
        diagram = "  HEAD\n   ▼\n[Commit A] ───► [Commit B] (Undo this, keep changes)\n\n  HEAD (Moved Back)\n   ▼\n[Commit A]  ◄─── [Your Unstaged Changes preserved in files]",
        commands = listOf("git reset --soft HEAD~1"),
        tips = listOf(
          "Files will remain staged if they were staged previously.",
          "Perfect for cleaning up a local mistake before pushing."
        )
      ),
      GuideScenario(
        id = "undo_discard",
        title = "Undo Last Commit (DISCARD Changes)",
        summary = "⚠️ Destructive: Completely wipes your last commit and all associated edits.",
        danger = true,
        explanation = "Completely resets your branch state back to the previous commit. All local changes, untracked modifications, and commit edits are permanently lost.",
        diagram = "  HEAD\n   ▼\n[Commit A] ───► [Commit B] (WIPED OUT Completely!)\n\n  HEAD (Moved Back)\n   ▼\n[Commit A]  (Files restored to this state exactly)",
        commands = listOf("git reset --hard HEAD~1"),
        tips = listOf(
          "WARNING: This command is irreversible! Make sure you don't need the work.",
          "Use with extreme caution on production or main branches."
        )
      ),
      GuideScenario(
        id = "wrong_branch",
        title = "Committed to the Wrong Branch",
        summary = "Safely move your latest commit work onto a brand new branch.",
        danger = false,
        explanation = "Undoes the last commit softly on the current branch, checks out a new branch, and recommits the work there.",
        diagram = "[main] ───► [Wrong Commit]\n\nStep 1: git reset --soft HEAD~1  (Undoes commit, keeps changes)\nStep 2: git checkout -b feat/new-feature  (Creates new branch)\nStep 3: git commit -m \"feat: description\"  (Commits to right branch)",
        commands = listOf(
          "git reset --soft HEAD~1",
          "git checkout -b feat/my-new-feature",
          "git add .",
          "git commit -m \"feat: move changes to correct branch\""
        ),
        tips = listOf(
          "If you already pushed the wrong commit to remote, you will need to push with lease on main.",
          "Always verify you are on the correct branch before typing commit!"
        )
      ),
      GuideScenario(
        id = "interactive_rebase",
        title = "Interactive Rebase (Clean History)",
        summary = "Squash, edit, or reorder your last 5 commits before sending a PR.",
        danger = false,
        explanation = "Opens an interactive editor terminal session where you can rewrite history locally. Allows squashing messy 'wip' commits into elegant single conventional commits.",
        diagram = "Local Commits:\n[wip A] ──► [fix typo] ──► [wip B] ──► [feat done]\n\nAfter Interactive Rebase (Squashed into one):\n[feat: complete user auth implementation]",
        commands = listOf("git rebase -i HEAD~5"),
        tips = listOf(
          "In the interactive screen, change 'pick' to 'squash' (or 's') for commits you want to merge into the previous one.",
          "Use 'reword' (or 'r') to change a commit message without changing its contents."
        )
      ),
      GuideScenario(
        id = "delete_recovery",
        title = "Accidentally Deleted a Branch",
        summary = "Use the reflog database to recover lost branches or commits.",
        danger = false,
        explanation = "Git reflog is a record of every single action you did locally. Even if a branch is deleted, its commits still exist in Git's database for up to 90 days. You can find the commit hash and restore the branch!",
        diagram = "Reflog Record:\nHEAD@{0}: checkout: moving from feat/deleted to main\nHEAD@{1}: commit: feat: complete core DB api (Commit Hash: abc1234)\n\nRecovery Command:\ngit branch feat/recovered abc1234",
        commands = listOf(
          "git reflog",
          "git branch recovered-branch <commit-hash>"
        ),
        tips = listOf(
          "Look for the commit message in the reflog output to identify the exact hash.",
          "Once recreated, the branch is fully restored with all its commits."
        )
      ),
      GuideScenario(
        id = "git_worktree",
        title = "Work in Parallel (Git Worktree)",
        summary = "Check out a second branch in a separate folder without stashing.",
        danger = false,
        explanation = "Avoids the annoying stash/unstash workflow. Creates an entirely separate physical folder linked to the same repository, checked out on a different branch.",
        diagram = "  /my-project (Working on feat-branch)\n  /my-project-hotfix (Working on critical-bug-fix, separate folder)\n\nBoth folders synchronize to the same local git configuration!",
        commands = listOf("git worktree add ../hotfix-folder hotfix/critical-bug"),
        tips = listOf(
          "You can open both folders in separate IDE windows at the same exact time.",
          "To clean up, run 'git worktree remove ../hotfix-folder' when finished."
        )
      )
    )
  }

  var expandedGuideId by rememberSaveable { mutableStateOf<String?>(null) }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
  ) {
    item {
      Text(
        text = "Git Workflow Troubleshooter",
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(vertical = 12.dp)
      )
    }

    items(guides) { guide ->
      val isExpanded = expandedGuideId == guide.id

      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 6.dp)
          .animateContentSize()
          .clickable { expandedGuideId = if (isExpanded) null else guide.id }
          .testTag("guide_card_${guide.id}"),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
          1.dp,
          if (guide.danger) MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
          else MaterialTheme.colorScheme.surfaceVariant
        )
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                  if (guide.danger) MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                  else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = if (guide.danger) Icons.Outlined.Warning else Icons.Outlined.Info,
                contentDescription = null,
                tint = if (guide.danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
              )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = guide.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (guide.danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = guide.summary,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                overflow = TextOverflow.Ellipsis
              )
            }

            Icon(
              imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          if (isExpanded) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
              text = "How it works:",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = guide.explanation,
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            // ASCII Visual Diagram Card
            Card(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
              colors = CardDefaults.cardColors(
                containerColor = Color(0xFF07080A)
              ),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
            ) {
              Text(
                text = guide.diagram,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = Color(0xFFD8DEE9),
                modifier = Modifier
                  .padding(12.dp)
                  .fillMaxWidth()
              )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
              text = "Commands to execute:",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )

            guide.commands.forEach { command ->
              Card(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                  containerColor = Color(0xFF1E222A)
                )
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text(
                    text = command,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                  )

                  IconButton(
                    onClick = { onCopy(command, "Command") },
                    modifier = Modifier.size(24.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.ContentCopy,
                      contentDescription = "Copy command",
                      tint = MaterialTheme.colorScheme.onSurfaceVariant,
                      modifier = Modifier.size(14.dp)
                    )
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
              text = "Pro-Tips:",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            guide.tips.forEach { tip ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 2.dp),
                verticalAlignment = Alignment.Top
              ) {
                Text(
                  text = "•",
                  fontSize = 12.sp,
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.padding(end = 6.dp)
                )
                Text(
                  text = tip,
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.weight(1f)
                )
              }
            }
          }
        }
      }
    }
  }
}

// ---------------------- TAB 3: COMMIT LINTER ----------------------

@Composable
fun LinterScreen(onCopy: (String, String) -> Unit) {
  var pastedText by rememberSaveable { mutableStateOf("") }
  var hasAnalyzed by rememberSaveable { mutableStateOf(false) }

  // Lint State
  var lintScore by remember { mutableStateOf(100) }
  val lintResults = remember { mutableStateListOf<LintResult>() }

  // Analyze commit text
  val performAnalysis: () -> Unit = {
    if (pastedText.isBlank()) {
      lintScore = 0
      lintResults.clear()
      lintResults.add(
        LintResult("Input", RuleStatus.FAIL, "Please enter or paste a commit message to analyze.")
      )
      hasAnalyzed = true
    } else {
      val lines = pastedText.lines()
      val firstLine = lines.firstOrNull() ?: ""
      val restLines = lines.drop(1)

      var tempScore = 100
      val tempList = mutableListOf<LintResult>()

      // 1. Check Conventional Prefix
      val prefixRegex = Regex("^(feat|fix|chore|docs|refactor|style|test|build|ci|perf|revert)(\\([^)]+\\))?:\\s.+")
      if (!prefixRegex.matches(firstLine)) {
        tempScore -= 30
        tempList.add(
          LintResult(
            "Conventional Prefix",
            RuleStatus.FAIL,
            "Does not follow conventional format. Use a type prefix (e.g., 'feat(auth): add email login')."
          )
        )
      } else {
        tempList.add(
          LintResult(
            "Conventional Prefix",
            RuleStatus.PASS,
            "Follows the correct conventional commit type naming scheme."
          )
        )
      }

      // 2. Check First Line Length
      if (firstLine.length > 72) {
        tempScore -= 25
        tempList.add(
          LintResult(
            "Line Length",
            RuleStatus.FAIL,
            "Commit title is too long (${firstLine.length} chars). Keep first line under 72 characters max."
          )
        )
      } else if (firstLine.length > 50) {
        tempScore -= 10
        tempList.add(
          LintResult(
            "Line Length",
            RuleStatus.WARN,
            "Commit title is ${firstLine.length} characters. Recommended length is 50 or fewer for terminal views."
          )
        )
      } else {
        tempList.add(
          LintResult(
            "Line Length",
            RuleStatus.PASS,
            "Commit title is perfect length (${firstLine.length} characters)."
          )
        )
      }

      // 3. Check Trailing Period
      if (firstLine.trimEnd().endsWith(".")) {
        tempScore -= 10
        tempList.add(
          LintResult(
            "No Trailing Period",
            RuleStatus.WARN,
            "Avoid putting a period at the end of the commit title."
          )
        )
      } else {
        tempList.add(
          LintResult(
            "No Trailing Period",
            RuleStatus.PASS,
            "Commit title has no trailing period."
          )
        )
      }

      // 4. Check Imperative Mood
      val descriptionText = if (firstLine.contains(":")) {
        firstLine.substringAfter(":").trim()
      } else {
        firstLine.trim()
      }
      val firstWord = descriptionText.split(" ").firstOrNull()?.lowercase() ?: ""

      val nonImperativeSuffixes = listOf("ed", "s", "ing")
      val isNonImperative = (firstWord.endsWith("ed") && firstWord != "red" && firstWord != "feed") || 
                            (firstWord.endsWith("s") && !firstWord.endsWith("ss") && firstWord != "as" && firstWord != "is") ||
                            (firstWord.endsWith("ing"))

      if (isNonImperative && firstWord.length > 3) {
        tempScore -= 15
        tempList.add(
          LintResult(
            "Imperative Mood",
            RuleStatus.WARN,
            "The verb '$firstWord' might not be in imperative mood. Prefer 'add' over 'added', 'fix' over 'fixes', or 'clean' over 'cleaning'."
          )
        )
      } else {
        tempList.add(
          LintResult(
            "Imperative Mood",
            RuleStatus.PASS,
            "Looks like it uses active/imperative mood correctly."
          )
        )
      }

      // 5. Check Body Spacing & Detail
      if (restLines.isEmpty()) {
        tempList.add(
          LintResult(
            "Commit Body",
            RuleStatus.INFO,
            "Consider adding a descriptive commit body separating details with a blank line for complex commits."
          )
        )
      } else {
        val secondLine = restLines.firstOrNull() ?: ""
        if (secondLine.isNotBlank()) {
          tempScore -= 10
          tempList.add(
            LintResult(
              "Commit Body Spacer",
              RuleStatus.FAIL,
              "Missing blank line between commit title and commit body."
            )
          )
        } else {
          tempList.add(
            LintResult(
              "Commit Body Spacer",
              RuleStatus.PASS,
              "Correct blank line exists before the body details."
            )
          )
        }
      }

      lintScore = tempScore.coerceAtLeast(0)
      lintResults.clear()
      lintResults.addAll(tempList)
      hasAnalyzed = true
    }
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
  ) {
    item {
      Text(
        text = "Analyze Local Commit Cleanliness",
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(vertical = 12.dp)
      )

      OutlinedTextField(
        value = pastedText,
        onValueChange = { pastedText = it },
        label = { Text("Paste Commit Message Here") },
        placeholder = { Text("feat(ui): add glowing outline buttons\n\nIntegrate M3 border stroke options to give cards a glowing hover effect.") },
        minLines = 4,
        maxLines = 8,
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = MaterialTheme.colorScheme.primary,
          unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        keyboardOptions = KeyboardOptions(
          capitalization = KeyboardCapitalization.None,
          imeAction = ImeAction.Done
        ),
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 16.dp)
          .testTag("input_linter_text")
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Quick Clear button
        TextButton(
          onClick = {
            pastedText = ""
            hasAnalyzed = false
          },
          colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
          )
        ) {
          Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Clear", fontSize = 13.sp)
        }

        Button(
          onClick = performAnalysis,
          shape = RoundedCornerShape(8.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
          ),
          modifier = Modifier.testTag("btn_analyze_commit")
        ) {
          Icon(imageVector = Icons.Default.Done, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Analyze Hygiene", fontSize = 13.sp)
        }
      }

      Spacer(modifier = Modifier.height(20.dp))
    }

    if (hasAnalyzed) {
      item {
        // Score Result Meter
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .testTag("score_card"),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
          ),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = "HYGIENE RATING",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(bottom = 8.dp)
            )

            // Animated Score Indicator Circle
            val animatedProgress by animateFloatAsState(
              targetValue = lintScore / 100f,
              animationSpec = tween(durationMillis = 800)
            )

            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier.size(100.dp)
            ) {
              CircularProgressIndicator(
                progress = { animatedProgress },
                strokeWidth = 8.dp,
                color = when {
                  lintScore >= 85 -> MaterialTheme.colorScheme.tertiary
                  lintScore >= 60 -> MaterialTheme.colorScheme.secondary
                  else -> MaterialTheme.colorScheme.error
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxSize()
              )

              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                  text = "$lintScore",
                  fontSize = 28.sp,
                  fontWeight = FontWeight.Black,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "points",
                  fontSize = 10.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
              text = when {
                lintScore == 100 -> "Master Level History! Clean and crisp."
                lintScore >= 85 -> "Excellent Hygiene! Ready for PR."
                lintScore >= 60 -> "Acceptable. Consider fixing recommended warnings."
                else -> "Hygiene Issues Detected. Fix format to maintain clean commits."
              },
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              color = when {
                lintScore >= 85 -> MaterialTheme.colorScheme.tertiary
                lintScore >= 60 -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.error
              },
              textAlign = TextAlign.Center
            )
          }
        }

        Text(
          text = "Detailed Analysis Rules:",
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground,
          modifier = Modifier.padding(bottom = 8.dp)
        )
      }

      items(lintResults) { rule ->
        val statusColor = when (rule.status) {
          RuleStatus.PASS -> MaterialTheme.colorScheme.tertiary
          RuleStatus.WARN -> MaterialTheme.colorScheme.secondary
          RuleStatus.FAIL -> MaterialTheme.colorScheme.error
          RuleStatus.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
        }

        val statusIcon = when (rule.status) {
          RuleStatus.PASS -> Icons.Default.CheckCircle
          RuleStatus.WARN -> Icons.Default.Warning
          RuleStatus.FAIL -> Icons.Default.Warning
          RuleStatus.INFO -> Icons.Default.Info
        }

        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
          ),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = statusIcon,
              contentDescription = rule.status.name,
              tint = statusColor,
              modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
              Text(
                text = rule.ruleName,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = rule.message,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    }
  }
}
