package marbles

/** A heterogeneous thread safe memoization table holding values forever. */
class PersistentHMemo[M[_, _]] extends HMemo[M] {

  private val unsafeMemo = Memo.empty[(M[_, _], Any), Any]

  /** Evaluates the body for the given key exactly once (unless an exception is thrown) and returns
    * the evaluation result for the key (thread safe).
    *
    * If the body evaluates successfully (no exceptions are thrown) all subsequent invocations will
    * return the same evaluation result. Results are never removed from the memoization table.
    *
    * If an exception is thrown inside the body, no result will be stored and the next invocation
    * will be treated as if the given key has never been processed before. The exception will be
    * thrown to exactly one invoker.
    *
    * @param key The key by which to memoize (function parameters).
    * @param body The body to be evaluated.
    * @param ev Evidence that type [[K]] and type [[V]] are a valid pair for usage in this [[HMemo]]
    *           as defined by [[M]]. Note that the evidence is used together with the key parameter
    *           to form the compound key used for the underlying
    *           [[Memo]] instance.
    * @tparam K type of the key
    * @tparam V type of the value
    * @return The result of the one successful evaluation of body for the given key (and evidence).
    */
  def memoize[K, V](key: K)(body: => V)(implicit ev: M[K, V]): V = {
    val result: Any = unsafeMemo.memoize((ev, key))(body)
    result.asInstanceOf[V]
  }

  /** Returns an [[HMap]] of all completed computations currently stored in the memo.
    *
    * If a key-value pair is successfully memoized causally before invoking [[getSnapshot]] then
    * it is guaranteed to be in the returned map. No guarantee is provided for pairs inserted
    * concurrently with getSnapshot.
    */
  def getSnapshot: HMap[M] = new HMap[M] {

    override protected val unsafeMap: Map[M[_, _], Map[_, _]] = {

      val evAndKeyToValueMap: Map[(M[_, _], Any), Any] = unsafeMemo.getSnapshot

      evAndKeyToValueMap groupBy {
        case ((ev, key), value) => ev
      } mapValues {
        m => m map { case ((ev, key), value) => key -> value}
      }
    }

  }

}

object PersistentHMemo {

  /** Creates a new empty [[PersistentHMemo]] for the given type mapping.
    *
    * @tparam M The type mapping for the [[PersistentHMemo]].
    */
  def empty[M[_, _]] = new PersistentHMemo[M]

}
