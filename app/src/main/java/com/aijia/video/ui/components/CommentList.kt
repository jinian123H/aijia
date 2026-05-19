package com.aijia.video.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aijia.video.data.model.Comment

@Composable
fun CommentList(
    comments: List<Comment>,
    onSendComment: (String) -> Unit,
    onLikeComment: (Long) -> Unit,
    onLoadMore: () -> Unit,
    errorMessage: String?
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn {
            items(
                items = comments,
                key = { comment -> comment.id }
            ) {
                Text(text = it.content)
            }
        }
    }
}
