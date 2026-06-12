package com.teslasync.modalsdialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/** Data readiness branch rendered by [FeedbackModal]. */
enum class FeedbackModalState { CONTENT, LOADING, EMPTY, ERROR, STALE, OFFLINE }

/** Feedback category mirrored from the web form schema. */
enum class FeedbackCategory { BUG, FEATURE, OTHER }

/** Selectable category option rendered in the feedback form. */
data class FeedbackCategoryOption(
    val category: FeedbackCategory,
    val label: String,
)

/** Payload emitted when the feedback form validates and is submitted. */
data class FeedbackSubmitPayload(
    val category: FeedbackCategory,
    val title: String,
    val body: String,
    val pageRoute: String,
    val userAgent: String,
    val appVersion: String,
    val includeRecentErrors: Boolean,
    val includeConsoleTail: Boolean,
)

/**
 * Native Android parity for `web/src/components/feedback/FeedbackModal.tsx`.
 *
 * Captures category, title, details, visible auto-attached context, two consent
 * toggles, validation, submit error state, disabled submit while invalid or
 * pending, and reset-on-close behavior. The caller owns submission and data.
 */
@Composable
fun FeedbackModal(
    open: Boolean,
    onClose: () -> Unit,
    onSubmit: (FeedbackSubmitPayload) -> Unit,
    modifier: Modifier = Modifier,
    state: FeedbackModalState = FeedbackModalState.CONTENT,
    pageRoute: String = "",
    appVersion: String = "",
    userAgent: String = "",
    recentErrorCount: Int = 0,
    isSubmitting: Boolean = false,
    submitError: Boolean = false,
    title: String = "Report a bug / Send feedback",
    categoryLabel: String = "What kind of feedback?",
    categories: List<FeedbackCategoryOption> = defaultFeedbackCategoryOptions(),
    titleLabel: String = "Title",
    titleHint: String = "Short summary (e.g. \"Battery widget shows NaN\")",
    detailsLabel: String = "Details",
    detailsHint: String = "What happened? What did you expect to happen? Steps to reproduce help a lot.",
    contextTitle: String = "Auto-attached context",
    pageLabel: String = "Page",
    appVersionLabel: String = "App version",
    browserLabel: String = "Browser",
    unknownLabel: String = "unknown",
    includeErrorsLabel: String = "Attach recent errors ($recentErrorCount)",
    includeErrorsHint: String = "Includes the most recent uncaught errors from this session. Helps reproduce the bug.",
    includeConsoleLabel: String = "Attach recent console messages",
    includeConsoleHint: String = "Privacy: console output may include URLs and data you saw. Off by default.",
    submitErrorLabel: String = "Failed to submit feedback. Please try again.",
    cancelLabel: String = "Cancel",
    submittingLabel: String = "Submitting…",
    submitLabel: String = "Send feedback",
    loadingLabel: String = "Loading feedback form…",
    emptyLabel: String = "Feedback form is unavailable.",
    errorLabel: String? = null,
    retryLabel: String = "Retry",
    staleLabel: String = "Feedback context may be out of date.",
    offlineLabel: String = "Offline; feedback can be sent when connectivity returns.",
    titleMinError: String = "Title must be at least 5 characters.",
    bodyMinError: String = "Details must be at least 20 characters.",
    dialogContentDescription: String = title,
    onRetry: (() -> Unit)? = null,
) {
    if (!open) return

    var selectedCategory by remember { mutableStateOf(FeedbackCategory.BUG) }
    var feedbackTitle by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var includeRecentErrors by remember { mutableStateOf(true) }
    var includeConsoleTail by remember { mutableStateOf(false) }
    var titleTouched by remember { mutableStateOf(false) }
    var bodyTouched by remember { mutableStateOf(false) }

    LaunchedEffect(open) {
        if (!open) {
            selectedCategory = FeedbackCategory.BUG
            feedbackTitle = ""
            body = ""
            includeRecentErrors = true
            includeConsoleTail = false
            titleTouched = false
            bodyTouched = false
        }
    }

    fun closeAndReset() {
        selectedCategory = FeedbackCategory.BUG
        feedbackTitle = ""
        body = ""
        includeRecentErrors = true
        includeConsoleTail = false
        titleTouched = false
        bodyTouched = false
        onClose()
    }

    val titleValid = feedbackTitle.trim().length in FEEDBACK_TITLE_MIN..FEEDBACK_TITLE_MAX
    val bodyValid = body.trim().length in FEEDBACK_BODY_MIN..FEEDBACK_BODY_MAX
    val canSubmit = !isSubmitting && titleValid && bodyValid

    Dialog(onDismissRequest = { if (!isSubmitting) closeAndReset() }) {
        Surface(
            modifier = modifier
                .widthIn(max = 640.dp)
                .semantics { contentDescription = dialogContentDescription },
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(text = title, style = MaterialTheme.typography.headlineSmall)

                when (state) {
                    FeedbackModalState.LOADING -> FeedbackStatusMessage(loadingLabel, showSpinner = true)
                    FeedbackModalState.EMPTY -> FeedbackStatusMessage(emptyLabel)
                    FeedbackModalState.ERROR -> FeedbackStatusMessage(
                        message = errorLabel ?: emptyLabel,
                        isError = true,
                        actionLabel = retryLabel,
                        onAction = onRetry,
                    )
                    FeedbackModalState.STALE -> {
                        FeedbackStatusChip(staleLabel)
                        FeedbackFormContent(
                            categoryLabel = categoryLabel,
                            categories = categories,
                            selectedCategory = selectedCategory,
                            onCategoryChange = { selectedCategory = it },
                            titleLabel = titleLabel,
                            titleHint = titleHint,
                            feedbackTitle = feedbackTitle,
                            onTitleChange = { feedbackTitle = it.take(FEEDBACK_TITLE_MAX) },
                            titleError = if (titleTouched && !titleValid) titleMinError else null,
                            onTitleBlur = { titleTouched = true },
                            detailsLabel = detailsLabel,
                            detailsHint = detailsHint,
                            body = body,
                            onBodyChange = { body = it.take(FEEDBACK_BODY_MAX) },
                            bodyError = if (bodyTouched && !bodyValid) bodyMinError else null,
                            onBodyBlur = { bodyTouched = true },
                            contextTitle = contextTitle,
                            pageLabel = pageLabel,
                            appVersionLabel = appVersionLabel,
                            browserLabel = browserLabel,
                            unknownLabel = unknownLabel,
                            pageRoute = pageRoute,
                            appVersion = appVersion,
                            userAgent = userAgent,
                            includeRecentErrors = includeRecentErrors,
                            onIncludeRecentErrorsChange = { includeRecentErrors = it },
                            includeErrorsLabel = includeErrorsLabel,
                            includeErrorsHint = includeErrorsHint,
                            includeConsoleTail = includeConsoleTail,
                            onIncludeConsoleTailChange = { includeConsoleTail = it },
                            includeConsoleLabel = includeConsoleLabel,
                            includeConsoleHint = includeConsoleHint,
                        )
                    }
                    FeedbackModalState.OFFLINE -> {
                        FeedbackStatusChip(offlineLabel)
                        FeedbackFormContent(
                            categoryLabel = categoryLabel,
                            categories = categories,
                            selectedCategory = selectedCategory,
                            onCategoryChange = { selectedCategory = it },
                            titleLabel = titleLabel,
                            titleHint = titleHint,
                            feedbackTitle = feedbackTitle,
                            onTitleChange = { feedbackTitle = it.take(FEEDBACK_TITLE_MAX) },
                            titleError = if (titleTouched && !titleValid) titleMinError else null,
                            onTitleBlur = { titleTouched = true },
                            detailsLabel = detailsLabel,
                            detailsHint = detailsHint,
                            body = body,
                            onBodyChange = { body = it.take(FEEDBACK_BODY_MAX) },
                            bodyError = if (bodyTouched && !bodyValid) bodyMinError else null,
                            onBodyBlur = { bodyTouched = true },
                            contextTitle = contextTitle,
                            pageLabel = pageLabel,
                            appVersionLabel = appVersionLabel,
                            browserLabel = browserLabel,
                            unknownLabel = unknownLabel,
                            pageRoute = pageRoute,
                            appVersion = appVersion,
                            userAgent = userAgent,
                            includeRecentErrors = includeRecentErrors,
                            onIncludeRecentErrorsChange = { includeRecentErrors = it },
                            includeErrorsLabel = includeErrorsLabel,
                            includeErrorsHint = includeErrorsHint,
                            includeConsoleTail = includeConsoleTail,
                            onIncludeConsoleTailChange = { includeConsoleTail = it },
                            includeConsoleLabel = includeConsoleLabel,
                            includeConsoleHint = includeConsoleHint,
                        )
                    }
                    FeedbackModalState.CONTENT -> FeedbackFormContent(
                        categoryLabel = categoryLabel,
                        categories = categories,
                        selectedCategory = selectedCategory,
                        onCategoryChange = { selectedCategory = it },
                        titleLabel = titleLabel,
                        titleHint = titleHint,
                        feedbackTitle = feedbackTitle,
                        onTitleChange = { feedbackTitle = it.take(FEEDBACK_TITLE_MAX) },
                        titleError = if (titleTouched && !titleValid) titleMinError else null,
                        onTitleBlur = { titleTouched = true },
                        detailsLabel = detailsLabel,
                        detailsHint = detailsHint,
                        body = body,
                        onBodyChange = { body = it.take(FEEDBACK_BODY_MAX) },
                        bodyError = if (bodyTouched && !bodyValid) bodyMinError else null,
                        onBodyBlur = { bodyTouched = true },
                        contextTitle = contextTitle,
                        pageLabel = pageLabel,
                        appVersionLabel = appVersionLabel,
                        browserLabel = browserLabel,
                        unknownLabel = unknownLabel,
                        pageRoute = pageRoute,
                        appVersion = appVersion,
                        userAgent = userAgent,
                        includeRecentErrors = includeRecentErrors,
                        onIncludeRecentErrorsChange = { includeRecentErrors = it },
                        includeErrorsLabel = includeErrorsLabel,
                        includeErrorsHint = includeErrorsHint,
                        includeConsoleTail = includeConsoleTail,
                        onIncludeConsoleTailChange = { includeConsoleTail = it },
                        includeConsoleLabel = includeConsoleLabel,
                        includeConsoleHint = includeConsoleHint,
                    )
                }

                if (submitError) {
                    Text(text = submitErrorLabel, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { closeAndReset() }, enabled = !isSubmitting) { Text(cancelLabel) }
                    Button(
                        modifier = Modifier.padding(start = 8.dp),
                        enabled = canSubmit && state != FeedbackModalState.LOADING,
                        onClick = {
                            titleTouched = true
                            bodyTouched = true
                            if (canSubmit) {
                                onSubmit(
                                    FeedbackSubmitPayload(
                                        category = selectedCategory,
                                        title = feedbackTitle.trim(),
                                        body = body.trim(),
                                        pageRoute = pageRoute,
                                        userAgent = userAgent,
                                        appVersion = appVersion,
                                        includeRecentErrors = includeRecentErrors,
                                        includeConsoleTail = includeConsoleTail,
                                    ),
                                )
                            }
                        },
                    ) { Text(if (isSubmitting) submittingLabel else submitLabel) }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FeedbackFormContent(
    categoryLabel: String,
    categories: List<FeedbackCategoryOption>,
    selectedCategory: FeedbackCategory,
    onCategoryChange: (FeedbackCategory) -> Unit,
    titleLabel: String,
    titleHint: String,
    feedbackTitle: String,
    onTitleChange: (String) -> Unit,
    titleError: String?,
    onTitleBlur: () -> Unit,
    detailsLabel: String,
    detailsHint: String,
    body: String,
    onBodyChange: (String) -> Unit,
    bodyError: String?,
    onBodyBlur: () -> Unit,
    contextTitle: String,
    pageLabel: String,
    appVersionLabel: String,
    browserLabel: String,
    unknownLabel: String,
    pageRoute: String,
    appVersion: String,
    userAgent: String,
    includeRecentErrors: Boolean,
    onIncludeRecentErrorsChange: (Boolean) -> Unit,
    includeErrorsLabel: String,
    includeErrorsHint: String,
    includeConsoleTail: Boolean,
    onIncludeConsoleTailChange: (Boolean) -> Unit,
    includeConsoleLabel: String,
    includeConsoleHint: String,
) {
    Column(
        modifier = Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = categoryLabel, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { option ->
                    FilterChip(
                        selected = selectedCategory == option.category,
                        onClick = { onCategoryChange(option.category) },
                        label = { Text(option.label) },
                    )
                }
            }
        }
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = feedbackTitle,
            onValueChange = onTitleChange,
            label = { Text(titleLabel) },
            supportingText = { Text(titleError ?: titleHint) },
            isError = titleError != null,
            singleLine = true,
        )
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = body,
            onValueChange = onBodyChange,
            label = { Text(detailsLabel) },
            supportingText = { Text(bodyError ?: detailsHint) },
            isError = bodyError != null,
            minLines = 6,
            maxLines = 8,
        )
        Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 2.dp, color = MaterialTheme.colorScheme.surfaceVariant) {
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = contextTitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ContextRow(pageLabel, pageRoute.ifBlank { unknownLabel })
                ContextRow(appVersionLabel, appVersion.ifBlank { unknownLabel })
                ContextRow(browserLabel, userAgent.ifBlank { unknownLabel })
                ToggleRow(includeErrorsLabel, includeErrorsHint, includeRecentErrors, onIncludeRecentErrorsChange)
                ToggleRow(includeConsoleLabel, includeConsoleHint, includeConsoleTail, onIncludeConsoleTailChange)
            }
        }
    }
}

@Composable
private fun ContextRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ToggleRow(label: String, hint: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = label, style = MaterialTheme.typography.bodySmall)
            Text(text = hint, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = Modifier.semantics { contentDescription = label })
    }
}

@Composable
private fun FeedbackStatusChip(text: String) {
    Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
        Text(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), text = text, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun FeedbackStatusMessage(
    message: String,
    showSpinner: Boolean = false,
    isError: Boolean = false,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (showSpinner) CircularProgressIndicator()
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (actionLabel != null && onAction != null) TextButton(onClick = onAction) { Text(actionLabel) }
    }
}

private const val FEEDBACK_TITLE_MIN = 5
private const val FEEDBACK_TITLE_MAX = 120
private const val FEEDBACK_BODY_MIN = 20
private const val FEEDBACK_BODY_MAX = 4000

private fun defaultFeedbackCategoryOptions(): List<FeedbackCategoryOption> = listOf(
    FeedbackCategoryOption(FeedbackCategory.BUG, "Bug report"),
    FeedbackCategoryOption(FeedbackCategory.FEATURE, "Feature request"),
    FeedbackCategoryOption(FeedbackCategory.OTHER, "Other / question"),
)
