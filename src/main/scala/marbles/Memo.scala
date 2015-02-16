package marbles

/** A base trait for memoization tables.
  *
  * @tparam K Type of key.
  * @tparam V Type of value.
  */
trait Memo[K, V] {

  /** If given key is not in the table, Evaluates the body, stores the result, and returns it.
    * Otherwise, returns the result already stored in the table.
    *
    * Exact semantics of the table is implementation specific.
    * See implementation documentation for more details.
    *
    * @param key The key by which to memoize (function parameters)
    * @param body The body to be evaluated.
    * @return body evaluation result or reviously stored result for the key.
    */
  def memoize(key: K)(body: => V): V

  /** Returns a map of all completed computations currently stored in the memo. */
  def getSnapshot: Map[K, V]

  /** Memoizes all key-value pairs in the given map to the memo.
    *
    * @param m The map to be loaded.
    * @return The memo itself after being updated.
    */
  def loadMap(m: Map[K, V]): this.type = {
    for {
      (key, value) <- m
    } memoize(key) {value}

    this
  }

}

object Memo {

  /** Creates a new empty [[Memo]] for the given types.
    *
    * @tparam K Type of key.
    * @tparam V Type of value.
    */
  def empty[K, V]: Memo[K, V] = PersistentMemo.empty[K, V]

}
