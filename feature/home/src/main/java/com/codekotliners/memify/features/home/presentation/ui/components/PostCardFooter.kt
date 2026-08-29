package com.codekotliners.memify.features.home.presentation.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import com.codekotliners.memify.features.home.R
import com.codekotliners.memify.features.home.presentation.model.HomePostUiModel

@Composable
internal fun PostCardFooter(
    post: HomePostUiModel,
    isLikePending: Boolean,
    onLikeClick: () -> Unit,
) {
    val actionDescription =
        stringResource(
            if (post.isLiked) R.string.unlike_post else R.string.like_post,
        )

    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = onLikeClick,
            enabled = !isLikePending,
        ) {
            Icon(
                imageVector =
                    if (post.isLiked) {
                        Icons.Filled.Favorite
                    } else {
                        Icons.Outlined.FavoriteBorder
                    },
                contentDescription = actionDescription,
                tint =
                    if (post.isLiked) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
        }

        Text(
            text = post.likesCount.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
