package com.castivio.domain

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * In a load's coroutine context: this section is being brought in on its own -- see [AutoLoads] -- and nobody is waiting
 * for it. The importer reads it to give way: its writer yields the processor to the screens instead of taking more of it,
 * and tells them of its rows once in a long while instead of every second, so a list being scrolled meanwhile is not read
 * again under the viewer's thumb.
 */
class BackgroundLoad : AbstractCoroutineContextElement(BackgroundLoad) {
    companion object Key : CoroutineContext.Key<BackgroundLoad>
}
