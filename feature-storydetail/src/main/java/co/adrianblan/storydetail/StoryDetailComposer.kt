package co.adrianblan.storydetail

import androidx.compose.Composable
import co.adrianblan.ui.Composer
import kotlinx.coroutines.cancel
import javax.inject.Inject


class StoryDetailComposer
@Inject constructor(
    private val storyDetailInteractor: StoryDetailInteractor,
    @StoryDetailInternal private val listener: Listener
): Composer {

    interface Listener {
        fun onStoryContentClicked(storyContentUrl: String)
        fun onStoryDetailFinished()
    }

    override val composeView = @Composable {
        StoryDetailScreen(
            viewState = storyDetailInteractor.viewState,
            onStoryContentClicked = { listener.onStoryContentClicked(it) },
            onBackPressed = { listener.onStoryDetailFinished() }
        )
    }

    override fun detach() =
        storyDetailInteractor.scope.cancel()
}