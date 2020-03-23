package co.adrianblan.storyfeed

import android.text.Html
import androidx.compose.*
import androidx.lifecycle.LiveData
import androidx.ui.core.*
import androidx.ui.foundation.*
import androidx.ui.foundation.shape.corner.RoundedCornerShape
import androidx.ui.graphics.Color
import androidx.ui.layout.*
import androidx.ui.material.Button
import androidx.ui.material.MaterialTheme
import androidx.ui.material.icons.Icons
import androidx.ui.material.icons.filled.ArrowDropDown
import androidx.ui.material.ripple.Ripple
import androidx.ui.material.surface.Surface
import androidx.ui.res.colorResource
import androidx.ui.res.stringResource
import androidx.ui.text.style.TextAlign
import androidx.ui.text.style.TextOverflow
import androidx.ui.tooling.preview.Preview
import androidx.ui.unit.*
import co.adrianblan.hackernews.StoryType
import co.adrianblan.hackernews.api.Story
import co.adrianblan.hackernews.api.StoryId
import co.adrianblan.hackernews.api.StoryUrl
import co.adrianblan.hackernews.api.dummy
import co.adrianblan.ui.*
import co.adrianblan.ui.InsetsAmbient
import co.adrianblan.webpreview.WebPreviewData

private const val toolbarMinHeightDp = 56
private const val toolbarMaxHeightDp = 128

internal fun StoryType.titleStringResource(): Int =
    when (this) {
        StoryType.TOP -> R.string.stories_top_title
        StoryType.BEST -> R.string.stories_best_title
        StoryType.NEW -> R.string.stories_new_title
        StoryType.ASK -> R.string.stories_ask_title
        StoryType.SHOW -> R.string.stories_show_title
        StoryType.JOB -> R.string.stories_job_title
    }

@Composable
fun StoryFeedScreen(
    viewState: LiveData<StoryFeedViewState>,
    onStoryTypeClick: (StoryType) -> Unit,
    onStoryClick: (StoryId) -> Unit,
    onStoryContentClick: (StoryUrl) -> Unit,
    onPageEndReached: () -> Unit
) {
    StoryFeedView(
        viewState = observe(viewState),
        onStoryTypeClick = onStoryTypeClick,
        onStoryClick = onStoryClick,
        onStoryContentClick = onStoryContentClick,
        onPageEndReached = onPageEndReached
    )
}

@Composable
fun StoryFeedView(
    viewState: StoryFeedViewState,
    onStoryTypeClick: (StoryType) -> Unit,
    onStoryClick: (StoryId) -> Unit,
    onStoryContentClick: (StoryUrl) -> Unit,
    onPageEndReached: () -> Unit
) {
    val scroller = ScrollerPosition()

    with(DensityAmbient.current) {
        onCommit(viewState.storyType) {

            val collapseDistance = (toolbarMaxHeightDp - toolbarMinHeightDp).dp

            // If story type is changed, revert scroll but retain toolbar collapse state
            val scrollReset: Px = min(scroller.value.px, collapseDistance.toPx())
            scroller.scrollTo(scrollReset.value)
        }
    }

    CollapsingScaffold(
        scroller = scroller,
        maxHeight = toolbarMaxHeightDp.dp,
        toolbarContent = { collapseFraction, height ->
            StoryFeedToolbar(
                collapsedFraction = collapseFraction,
                height = height,
                storyType = viewState.storyType,
                onStoryTypeClick = onStoryTypeClick
            )
        },
        bodyContent = {
            StoryFeedBodyContent(
                scroller = scroller,
                viewState = viewState,
                onStoryClick = onStoryClick,
                onStoryContentClick = onStoryContentClick,
                onPageEndReached = onPageEndReached
            )
        }
    )
}

@Composable
fun StoryFeedBodyContent(
    scroller: ScrollerPosition,
    viewState: StoryFeedViewState,
    onStoryClick: (StoryId) -> Unit,
    onStoryContentClick: (StoryUrl) -> Unit,
    onPageEndReached: () -> Unit
) {

    when (viewState.storyFeedState) {
        is StoryFeedState.Loading -> LoadingView()
        is StoryFeedState.Success -> {
            StoryFeedSuccessContentBody(
                scroller = scroller,
                viewState = viewState,
                onStoryClick = onStoryClick,
                onStoryContentClick = onStoryContentClick,
                onPageEndReached = onPageEndReached
            )
        }
        is StoryFeedState.Error -> ErrorView()
    }
}

@Composable
fun StoryFeedSuccessContentBody(
    scroller: ScrollerPosition,
    viewState: StoryFeedViewState,
    onStoryClick: (StoryId) -> Unit,
    onStoryContentClick: (StoryUrl) -> Unit,
    onPageEndReached: () -> Unit
) {
    val storyFeedState = viewState.storyFeedState as StoryFeedState.Success

    val scrollEndZone: Px = with(DensityAmbient.current) { 400.dp.toPx() }

    Observe {
        val isScrolledToEnd: Boolean =
            scroller.value > scroller.maxPosition - scrollEndZone.value

        onCommit(isScrolledToEnd) {
            if (isScrolledToEnd) {

                // Stop scroll fling
                scroller.scrollTo(scroller.value)

                onPageEndReached()
            }
        }
    }

    // TODO change to AdapterList
    VerticalScroller(scrollerPosition = scroller) {

        Column {

            with(DensityAmbient.current) {
                val insets = InsetsAmbient.current
                val topInsets = insets.top.px.toDp()

                Spacer(modifier = LayoutHeight(toolbarMaxHeightDp.dp + topInsets))

                storyFeedState.stories.map { story ->
                    key(story.story.id) {
                        StoryFeedItem(
                            decoratedStory = story,
                            onStoryClick = onStoryClick,
                            onStoryContentClick = onStoryContentClick
                        )
                    }
                }

                when {
                    viewState.isLoadingMorePages -> LoadingMoreStoriesView()
                    !viewState.hasLoadedAllPages -> LoadMoreStoriesButton(onPageEndReached = onPageEndReached)
                    viewState.hasLoadedAllPages -> NoMoreStoriesView()
                }

                Spacer(modifier = LayoutHeight(insets.bottom.px.toDp() + 8.dp))
            }
        }
    }
}

@Composable
fun StoryFeedItem(
    decoratedStory: DecoratedStory,
    onStoryClick: (StoryId) -> Unit,
    onStoryContentClick: (StoryUrl) -> Unit
) {
    val story = decoratedStory.story
    val webPreview = decoratedStory.webPreview

    Ripple(bounded = true) {
        Clickable(onClick = { onStoryClick(story.id) }) {
            Container(
                padding = EdgeInsets(left = 16.dp, right = 16.dp, top = 16.dp, bottom = 12.dp)
            ) {
                Row {
                    Column(
                        arrangement = Arrangement.Begin,
                        modifier = LayoutFlexible(1f)
                    ) {
                        Text(
                            text = story.title,
                            style = MaterialTheme.typography().subtitle1
                        )
                        val description: String? =
                            story.text
                                .takeIf { !it.isNullOrEmpty() }
                                ?.let {
                                    Html.fromHtml(it).toString()
                                        .replace("\n\n", " ")
                                }
                                ?: webPreview?.description

                        if (description != null)
                            Text(
                                text = description,
                                style = MaterialTheme.typography().body2,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                    }

                    story.url?.let { storyUrl ->
                        Spacer(modifier = LayoutWidth(12.dp))
                        StoryFeedImage(
                            storyId = story.id,
                            webPreview = webPreview
                        ) {
                            onStoryContentClick(storyUrl)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StoryFeedImage(
    @Pivotal storyId: StoryId,
    webPreview: WebPreviewData?,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        modifier = LayoutWidth(80.dp) + LayoutHeight(80.dp)
    ) {
        Ripple(bounded = true) {
            Clickable(onClick = onClick) {
                Stack {
                    Surface(
                        color = colorResource(R.color.contentLoading),
                        modifier = LayoutWidth.Fill + LayoutHeight.Fill
                    ) {}

                    val imageUrl =
                        webPreview?.imageUrl ?: webPreview?.iconUrl ?: webPreview?.favIconUrl

                    if (imageUrl != null) {
                        UrlImage(imageUrl)
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingMoreStoriesView() {
    Container(padding = EdgeInsets(8.dp), modifier = LayoutWidth.Fill) {
        Text(
            text = stringResource(id = R.string.loading_title),
            style = MaterialTheme.typography().subtitle1
                .copy(
                    color = MaterialTheme.colors().onPrimary.copy(alpha = textSecondaryAlpha),
                    textAlign = TextAlign.Center
                ),
            modifier = LayoutAlign.Center
        )
    }
}

@Composable
private fun LoadMoreStoriesButton(
    onPageEndReached: () -> Unit
) {
    Container(modifier = LayoutWidth.Fill) {
        Button(
            modifier = LayoutAlign.Center,
            onClick = onPageEndReached
        ) {
            Text(
                stringResource(R.string.stories_load_more_stories),
                modifier = LayoutPadding(
                    left = 16.dp,
                    right = 16.dp,
                    top = 8.dp,
                    bottom = 8.dp
                )
            )
        }
    }
}

@Composable
private fun NoMoreStoriesView() {
    Container(
        expanded = true,
        padding = EdgeInsets(32.dp),
        modifier = LayoutHeight(180.dp)
    ) {
        Text(
            text = stringResource(id = R.string.stories_no_more_stories),
            style = MaterialTheme.typography().subtitle2
                .copy(
                    color = MaterialTheme.colors().onPrimary.copy(alpha = textSecondaryAlpha),
                    textAlign = TextAlign.Center
                ),
            modifier = LayoutAlign.Center
        )
    }
}

@Preview
@Composable
fun StoryFeedPreview() {
    AppTheme {
        val viewState = StoryFeedViewState(
            StoryType.TOP,
            StoryFeedState.Success(List(10) { DecoratedStory(Story.dummy, null) }),
            isLoadingMorePages = true,
            hasLoadedAllPages = false
        )

        StoryFeedView(
            viewState,
            onStoryTypeClick = {},
            onStoryClick = {},
            onStoryContentClick = {},
            onPageEndReached = {}
        )
    }
}