package com.kenny.openimgur.ui.adapters;

import android.content.Context;
import android.support.v4.util.LongSparseArray;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.text.util.Linkify;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ImageButton;

import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.CustomLinkMovement;
import com.kenny.openimgur.classes.ImgurBaseObject;
import com.kenny.openimgur.classes.ImgurComment;
import com.kenny.openimgur.classes.ImgurListener;
import com.kenny.openimgur.util.LinkUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.BindView;

/**
 * Created by kcampagna on 6/10/15.
 */
public class CommentAdapter extends BaseRecyclerAdapter<ImgurComment> {
    private static final float EXPANDED = 135.0f;

    private static final float COLLAPSED = 0.0f;

    ImgurListener mListener;

    private int mSelectedIndex = -1;

    private String mOP;

    private final Set<ImgurComment> mExpandedComments = new HashSet<>();

    private final LongSparseArray<Integer> mIndicatorMultiples = new LongSparseArray<>();

    private int mGreenTextColor;

    private int mRedTextColor;

    private int mCommentIndent;

    private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S+");

    private static final Pattern DIRECT_IMAGE_PATTERN = Pattern.compile(".*\\.(jpg|jpeg|png|gif|webp)(?:$|[?&#/_-].*)", Pattern.CASE_INSENSITIVE);

    private static final Pattern DIRECT_VIDEO_PATTERN = Pattern.compile(".*\\.(gifv|mp4|webm)(?:$|[?&#/_-].*)", Pattern.CASE_INSENSITIVE);

    public CommentAdapter(Context context, List<ImgurComment> comments, ImgurListener listener) {
        super(context, comments, true);
        mListener = listener;
        mGreenTextColor = getColor(R.color.notoriety_positive);
        mRedTextColor = getColor(R.color.notoriety_negative);
        mCommentIndent = getDimension(R.dimen.comment_padding);
    }

    /**
     * Removes all items from list and ImgurListener is removed
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        clear();
        mListener = null;
    }

    public void clearExpansionInfo() {
        mExpandedComments.clear();
        mIndicatorMultiples.clear();
    }

    @Override
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final CommentViewHolder holder = new CommentViewHolder(inflateView(R.layout.comment_item, parent));
        holder.comment.setMovementMethod(CustomLinkMovement.getInstance(mListener));
        holder.replies.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) mListener.onViewRepliesTap(holder.itemView);
            }
        });
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // This will trigger the callback for list item click
                if (mListener != null) mListener.onLinkTap(holder.itemView, null);
            }
        });

        holder.mediaPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null && holder.mediaPlay.getTag() instanceof String) {
                    mListener.onLinkTap(holder.itemView, (String) holder.mediaPlay.getTag());
                }
            }
        });

        holder.mediaPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null && holder.mediaPlay.getTag() instanceof String) {
                    mListener.onLinkTap(holder.itemView, (String) holder.mediaPlay.getTag());
                }
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(BaseViewHolder holder, int position) {
        holder.itemView.setTag(holder);
        CommentViewHolder commentHolder = (CommentViewHolder) holder;
        final ImgurComment comment = getItem(position);
        String commentText = comment.getComment();
        String mediaUrl = findEmbeddableMediaUrl(commentText);
        String displayComment = getDisplayCommentText(commentText, mediaUrl);

        commentHolder.comment.setText(displayComment);
        commentHolder.author.setText(constructSpan(comment));
        commentHolder.comment.setVisibility(TextUtils.isEmpty(displayComment) ? View.GONE : View.VISIBLE);

        if (!TextUtils.isEmpty(displayComment)) {
            Linkify.addLinks(commentHolder.comment, Linkify.WEB_URLS);
            Linkify.addLinks(commentHolder.comment, LinkUtils.USER_CALLOUT_PATTERN, null);
        }

        commentHolder.replies.setVisibility(comment.getReplyCount() > 0 ? View.VISIBLE : View.GONE);
        boolean isExpanded = mExpandedComments.contains(comment);
        commentHolder.replies.setRotation(isExpanded ? EXPANDED : COLLAPSED);
        commentHolder.indicator.setVisibility(comment.getParentId() > 0 ? View.VISIBLE : View.GONE);

        Integer multiple = mIndicatorMultiples.get(comment.getParentId());
        RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) commentHolder.itemView.getLayoutParams();

        if (multiple != null) {
            lp.setMargins(multiple * mCommentIndent, 0, 0, 0);
        } else {
            lp.setMargins(0, 0, 0, 0);
        }

        if (ImgurBaseObject.VOTE_UP.equals(comment.getVote())) {
            commentHolder.author.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_upvote_green_12dp, 0);
        } else if (ImgurBaseObject.VOTE_DOWN.equals(comment.getVote())) {
            commentHolder.author.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_downvote_red_12dp, 0);
        } else {
            commentHolder.author.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
        }

        int bgColor = position == mSelectedIndex ? getColor(R.color.comment_bg_selected) : getColor(android.R.color.transparent);
        commentHolder.itemView.setBackgroundColor(bgColor);

        if (!TextUtils.isEmpty(mediaUrl)) {
            String previewUrl = getPreviewUrl(mediaUrl);
            boolean showPlayButton = LinkUtils.isLinkAnimated(mediaUrl);
            commentHolder.mediaContainer.setVisibility(View.VISIBLE);
            commentHolder.mediaPlay.setVisibility(showPlayButton ? View.VISIBLE : View.GONE);
            commentHolder.mediaPlay.setTag(mediaUrl);

            if (!TextUtils.isEmpty(previewUrl)) {
                displayImage(commentHolder.mediaPreview, previewUrl);
            } else {
                commentHolder.mediaPreview.setImageDrawable(null);
            }
        } else {
            commentHolder.mediaContainer.setVisibility(View.GONE);
            commentHolder.mediaPlay.setTag(null);
            commentHolder.mediaPreview.setImageDrawable(null);
        }
    }

    private String findEmbeddableMediaUrl(String commentText) {
        if (TextUtils.isEmpty(commentText)) {
            return null;
        }

        Matcher matcher = URL_PATTERN.matcher(commentText);

        while (matcher.find()) {
            String url = sanitizeUrl(matcher.group());

            if (isEmbeddableMediaUrl(url)) {
                return url;
            }
        }

        return null;
    }

    private boolean isEmbeddableMediaUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            return false;
        }

        return DIRECT_IMAGE_PATTERN.matcher(url).matches() || DIRECT_VIDEO_PATTERN.matcher(url).matches();
    }

    private String getDisplayCommentText(String commentText, String mediaUrl) {
        if (TextUtils.isEmpty(commentText) || TextUtils.isEmpty(mediaUrl)) {
            return commentText;
        }

        String cleaned = commentText.replaceFirst(Pattern.quote(mediaUrl) + "[\\)\\]\\}\\.,]*", "");
        cleaned = cleaned.replaceAll("\\s{2,}", " ").trim();
        return cleaned;
    }

    private String sanitizeUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            return url;
        }

        while (!TextUtils.isEmpty(url)) {
            char c = url.charAt(url.length() - 1);

            if (c == ')' || c == ']' || c == '}' || c == ',' || c == '.') {
                url = url.substring(0, url.length() - 1);
            } else {
                break;
            }
        }

        return url;
    }

    private String getPreviewUrl(String mediaUrl) {
        if (TextUtils.isEmpty(mediaUrl)) {
            return null;
        }

        int queryIndex = mediaUrl.indexOf('?');
        String cleanUrl = queryIndex >= 0 ? mediaUrl.substring(0, queryIndex) : mediaUrl;

        if (LinkUtils.isVideoLink(cleanUrl)) {
            if (!cleanUrl.contains("imgur.com")) {
                return null;
            }

            int extIndex = cleanUrl.lastIndexOf('.');

            if (extIndex > 0) {
                return cleanUrl.substring(0, extIndex) + ".jpg";
            }

            return null;
        }

        return cleanUrl;
    }

    /**
     * Creates the spannable object for the authors name, points, and time
     *
     * @param comment
     * @return
     */
    private Spannable constructSpan(ImgurComment comment) {
        CharSequence date = getDateFormattedTime(comment.getDate() * DateUtils.SECOND_IN_MILLIS);
        String author = comment.getAuthor();
        StringBuilder sb = new StringBuilder(author);
        boolean isOp = isOP(author);
        int spanLength = author.length();
        String points = getResources().getQuantityString(R.plurals.points, (int) comment.getPoints(), comment.getPoints());
        int scoreTextLength = points.length();

        if (isOp) {
            sb.append(" OP");
            spanLength += 3;
        }

        sb.append(": ").append(points).append(" ").append(date);
        Spannable span = new SpannableString(sb.toString());
        int scoreColor = comment.getPoints() >= 0 ? mGreenTextColor : mRedTextColor;

        if (isOp) {
            span.setSpan(new ForegroundColorSpan(mGreenTextColor), author.length() + 1, spanLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        int scoreStart = author.length() + 2;
        if (isOp) scoreStart += +3;
        span.setSpan(new ForegroundColorSpan(scoreColor), scoreStart, scoreStart + scoreTextLength, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return span;
    }

    private CharSequence getDateFormattedTime(long commentDate) {
        long now = System.currentTimeMillis();
        long difference = System.currentTimeMillis() - commentDate;

        return (difference >= 0 && difference <= DateUtils.MINUTE_IN_MILLIS) ?
                getString(R.string.moments_ago) :
                DateUtils.getRelativeTimeSpanString(
                        commentDate,
                        now,
                        DateUtils.MINUTE_IN_MILLIS,
                        DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_ABBREV_RELATIVE
                                | DateUtils.FORMAT_ABBREV_ALL);
    }

    public void setOP(String op) {
        mOP = op;
    }

    public void expandComments(View view, int position) {
        ImgurComment comment = getItem(position);
        if (comment.getReplyCount() <= 0) return;

        // Should always be the case
        if (view.getTag() instanceof CommentViewHolder) {
            ((CommentViewHolder) view.getTag()).replies.animate().rotation(EXPANDED);
        }

        mExpandedComments.add(comment);
        Integer multiple = mIndicatorMultiples.get(comment.getParentId());

        if (multiple == null) {
            if (comment.getParentId() > 0) mIndicatorMultiples.put(comment.getParentId(), 1);
            mIndicatorMultiples.put(Long.valueOf(comment.getId()), 1);
        } else {
            mIndicatorMultiples.put(Long.valueOf(comment.getId()), multiple + 1);
        }

        addItems(comment.getReplies(), position + 1);
    }

    public void collapseComments(View view, int position) {
        ImgurComment comment = getItem(position);
        if (comment.getReplyCount() <= 0) return;

        // Should always be the case
        if (view.getTag() instanceof CommentViewHolder) {
            ((CommentViewHolder) view.getTag()).replies.animate().rotation(COLLAPSED);
        }

        position++;
        mExpandedComments.remove(comment);
        int endPosition = -1;

        for (int i = position; i < getItemCount(); i++) {
            ImgurComment c = getItem(i);

            if (c.getParentId() == comment.getParentId()) {
                endPosition = i;
                break;
            } else if (mExpandedComments.contains(c)) {
                // Remove any expanded comments
                mExpandedComments.remove(c);
            }
        }


        // Didn't find a comment parent to find the ending position, need to traverse the list and find comments to close
        if (endPosition == -1) {
            // Find the best parent to collapse to
            for (int i = position - 2; i >= 0; i--) {
                ImgurComment possibleParent = getItem(i);
                // Now do the same steps as above to get a matching parent
                for (int x = position; x < getItemCount(); x++) {
                    ImgurComment c = getItem(x);
                    if (c.getParentId() == possibleParent.getParentId()) {
                        endPosition = x;
                        break;
                    } else if (mExpandedComments.contains(c)) {
                        // Remove any expanded comments
                        mExpandedComments.remove(c);
                    }
                }

                if (endPosition != -1) break;
            }
        }

        // Still didn't find anything, just collapse the number of replies it has as a fail safe
        if (endPosition == -1) endPosition = position + comment.getReplyCount();
        removeItems(position, endPosition);
    }

    /**
     * Sets the currently selected item. If the item selected is the one that is already selected, it is deselected
     *
     * @param index
     * @return If the selected item was already selected
     */
    public boolean setSelectedIndex(int index) {
        int positionToUpdate;
        boolean wasSelected;

        if (index >= 0) {
            wasSelected = mSelectedIndex == index;

            if (wasSelected) {
                positionToUpdate = mSelectedIndex;
                mSelectedIndex = -1;
            } else {
                positionToUpdate = index;
                mSelectedIndex = index;
            }
        } else {
            positionToUpdate = mSelectedIndex;
            mSelectedIndex = -1;
            wasSelected = false;
        }

        notifyItemChanged(positionToUpdate);
        return wasSelected;
    }

    /**
     * Returns if the current comment is expanded
     *
     * @param position
     * @return
     */
    public boolean isExpanded(int position) {
        return mExpandedComments.contains(getItem(position));
    }

    private boolean isOP(String user) {
        return !TextUtils.isEmpty(mOP) && mOP.equals(user);
    }

    public static class CommentViewHolder extends BaseViewHolder {
        @BindView(R.id.author)
        TextView author;

        @BindView(R.id.comment)
        TextView comment;

        @BindView(R.id.replies)
        ImageButton replies;

        @BindView(R.id.indicator)
        View indicator;

        @BindView(R.id.mediaContainer)
        View mediaContainer;

        @BindView(R.id.mediaPreview)
        ImageView mediaPreview;

        @BindView(R.id.mediaPlay)
        FloatingActionButton mediaPlay;

        public CommentViewHolder(View view) {
            super(view);
        }
    }
}
