package net.mullvad.mullvadvpn.ui.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.AttributeSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import net.mullvad.mullvadvpn.R
import net.mullvad.mullvadvpn.service.MullvadDaemon
import net.mullvad.mullvadvpn.util.JobTracker

class UrlButton : Button {
    private lateinit var daemon: MullvadDaemon

    private var shouldEnable = true

    var url: String? = null
    var withToken = false

    constructor(context: Context) : super(context) {}

    constructor(context: Context, attributes: AttributeSet) : super(context, attributes) {
        loadAttributes(attributes)
    }

    constructor(context: Context, attributes: AttributeSet, defaultStyleAttribute: Int) :
        super(context, attributes, defaultStyleAttribute) {
        loadAttributes(attributes)
    }

    constructor(
        context: Context,
        attributes: AttributeSet,
        defaultStyleAttribute: Int,
        defaultStyleResource: Int
    ) : super(context, attributes, defaultStyleAttribute, defaultStyleResource) {
        loadAttributes(attributes)
    }

    init {
        super.setEnabled(false)
    }

    fun prepare(daemon: MullvadDaemon, jobTracker: JobTracker, jobName: String = "fetchUrl") {
        synchronized(this) {
            super.setEnabled(shouldEnable)

            this.daemon = daemon

            setOnClickAction(jobName, jobTracker) {
                super.setEnabled(false)

                context.startActivity(buildIntent(jobTracker))

                super.setEnabled(true)
            }
        }
    }

    override fun setEnabled(enabled: Boolean) {
        synchronized(this) {
            shouldEnable = enabled

            if (!withToken || this::daemon.isInitialized) {
                super.setEnabled(enabled)
            }
        }
    }

    private fun loadAttributes(attributes: AttributeSet) {
        val styleableId = R.styleable.UrlButton

        context.theme.obtainStyledAttributes(attributes, styleableId, 0, 0).apply {
            try {
                url = getString(R.styleable.UrlButton_url)
                withToken = getBoolean(R.styleable.UrlButton_withToken, false)
            } finally {
                recycle()
            }
        }
    }

    private suspend fun buildIntent(jobTracker: JobTracker): Intent {
        val buildIntent = GlobalScope.async(Dispatchers.Default) {
            val uri = if (withToken) {
                Uri.parse(url + "?token=" + daemon.getWwwAuthToken())
            } else {
                Uri.parse(url)
            }

            Intent(Intent.ACTION_VIEW, uri)
        }

        jobTracker.newJob(buildIntent)

        return buildIntent.await()
    }
}
