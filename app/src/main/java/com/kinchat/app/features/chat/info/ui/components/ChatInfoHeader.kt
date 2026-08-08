package com.kinchat.app.features.chat.info.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kinchat.app.features.chat.info.domain.model.UserProfile

@Composable
fun ChatInfoHeader(
    profile: UserProfile?,
    isLoading: Boolean,
    onAudioCallClick: () -> Unit,
    onVideoCallClick: () -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isLoading) {
        ChatInfoHeaderSkeleton(modifier)
        return
    }

    if (profile == null) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "User not found", color = MaterialTheme.colorScheme.error)
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(top = 32.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ChatInfoAvatar(avatarUrl = profile.avatar_url)
        
        Spacer(modifier = Modifier.height(16.dp))

        ChatInfoDetails(profile = profile)

        ChatInfoActionButtons(
            onAudioCallClick = onAudioCallClick,
            onVideoCallClick = onVideoCallClick,
            onSearchClick = onSearchClick,
            modifier = Modifier.padding(top = 24.dp)
        )

        ChatInfoBio(
            bio = profile.bio,
            modifier = Modifier.padding(top = 24.dp, start = 24.dp, end = 24.dp)
        )
    }
}
