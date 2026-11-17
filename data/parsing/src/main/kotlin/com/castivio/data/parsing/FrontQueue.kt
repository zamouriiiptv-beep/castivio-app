package com.castivio.data.parsing

/**
 * What a walk of the categories has still to fetch: the provider's own order, with the ones a viewer asks for taken first.
 *
 * ## Why it exists
 *
 * A walk is a fixed number of workers pulling from one list. A viewer who opens a category that is far down it is asking for
 * it *now*, and the list had no way to say so: it would be fetched when the workers reached it. This is the list with that
 * way. [bump] puts a category at the front, and [next] hands the workers the front first -- so an asked category costs the
 * walk no request of its own, and the walk never has more requests open than it has workers.
 *
 * ## What it guarantees
 *
 *  - **Each item is handed out once.** One that was asked for and is also further down the list is handed out when it was
 *    asked for and skipped when the list reaches it, so no category is fetched twice and none is counted twice.
 *  - **The asks are served in the order they were made**, ahead of everything the provider's order has left.
 *  - **An ask that means nothing is nothing:** a key that is not in the list -- another section's category, a stale id -- an
 *    item already handed out, and one already asked for.
 *
 * Safe from any thread, which it needs to be: the workers take from it and the one that listens for the viewer's asks puts
 * into it. Plain Kotlin, like the engine that holds it.
 */
internal class FrontQueue<T : Any>(items: List<T>, private val keyOf: (T) -> String) {

    private val inOrder = ArrayDeque(items)
    private val byKey = HashMap<String, T>(items.size * 2).also { map -> for (item in items) map[keyOf(item)] = item }
    private val asked = ArrayDeque<T>()
    private val taken = HashSet<String>()

    /** The item named [key] goes to the front, behind the ones asked for before it. Nothing is done for one that cannot. */
    @Synchronized
    fun bump(key: String) {
        val item = byKey[key] ?: return
        if (key in taken || asked.any { keyOf(it) == key }) return
        asked.addLast(item)
    }

    /** The next item to fetch -- the first asked for that is still to be had, else the next in order -- or null when none is left. */
    @Synchronized
    fun next(): T? {
        while (true) {
            val item = asked.removeFirstOrNull() ?: inOrder.removeFirstOrNull() ?: return null
            if (taken.add(keyOf(item))) return item
        }
    }
}
