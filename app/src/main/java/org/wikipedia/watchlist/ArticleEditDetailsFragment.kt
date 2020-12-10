package org.wikipedia.watchlist

import android.os.Bundle
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_article_edit_details.*
import org.apache.commons.lang3.StringUtils
import org.wikipedia.R
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryPage.Revision
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.log.L
import org.wikipedia.watchlist.ArticleEditDetailsActivity.Companion.EXTRA_SOURCE_ARTICLE_TITLE
import org.wikipedia.watchlist.ArticleEditDetailsActivity.Companion.EXTRA_SOURCE_EDIT_LANGUAGE_CODE
import org.wikipedia.watchlist.ArticleEditDetailsActivity.Companion.EXTRA_SOURCE_EDIT_REVISION_ID


class ArticleEditDetailsFragment : Fragment() {
    private lateinit var articleTitle: String
    private var revisionId: Long = 0
    private var currentRevision: Revision? = null
    private var newerRevisionId: Long = 0
    private var olderRevisionId: Long = 0
    private lateinit var languageCode: String
    private val disposables = CompositeDisposable()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_article_edit_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        articleTitle = StringUtils.defaultString(requireActivity().intent
                .getStringExtra(EXTRA_SOURCE_ARTICLE_TITLE), "Earthworm")
        revisionId = requireActivity().intent
                .getLongExtra(EXTRA_SOURCE_EDIT_REVISION_ID, 0)
        languageCode = StringUtils.defaultString(requireActivity().intent
                .getStringExtra(EXTRA_SOURCE_EDIT_LANGUAGE_CODE), "en")
        newerButton.setOnClickListener {
            currentRevision = null
            revisionId = newerRevisionId
            fetchNeighborEdits()
        }
        olderButton.setOnClickListener {
            currentRevision = null
            revisionId = olderRevisionId
            fetchNeighborEdits()
        }
        fetchNeighborEdits()
        highlightDiffText()
    }

    private fun fetchNeighborEdits() {
        fetchEditDetails(DIRECTION_NEWER)
        fetchEditDetails(DIRECTION_OLDER)
    }

    private fun updateUI() {
        userIdButton.text = currentRevision?.user
        editTimestamp.text = currentRevision?.timeStamp()
    }

    private fun fetchEditDetails(direction: String) {
        disposables.add(ServiceFactory.get(WikiSite.forLanguageCode(languageCode)).getRevisionDetails(articleTitle, revisionId, direction)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ response ->
                    if (currentRevision == null) {
                        currentRevision = response.query()!!.firstPage()!!.revisions()[0]
                        updateUI()
                    }
                    if (direction === DIRECTION_NEWER) {
                        newerRevisionId = response.query()!!.firstPage()!!.revisions()[0].parentRevId
                    } else {
                        olderRevisionId = response.query()!!.firstPage()!!.revisions()[0].parentRevId
                    }
                }) { t: Throwable? -> L.e(t) })

    }

    private fun highlightDiffText() {
        val spannableString = SpannableString(diffText.text.toString())
        spannableString.setSpan(BackgroundColorSpan(ResourceUtil.getThemedColor(requireContext(), R.attr.color_group_57)), 0, diffText.text.length - 1, 0)
        diffText.text = spannableString
    }

    companion object {
        const val DIRECTION_NEWER = "newer"
        const val DIRECTION_OLDER = "older"

        fun newInstance(): ArticleEditDetailsFragment {
            return ArticleEditDetailsFragment()
        }
    }
}
