package marbles

/** A heterogeneous memo, which allows many key-value type combinations to be stored inside one memo.
  *
  * @see [[Memo]] for a general description of memoization.
  *
  * [[HMemo]] is a heterogeneous counterpart to [[Memo]],
  * supporting similar semantics and guarantees.
  *
  * The type combinations are specified and statically checked at compile time by parameterizing
  * the [[HMemo]] with a type mapping.
  *
  * @tparam M The type mapping for this [[HMemo]].
  *           Instances of this class are required and implicitly used for each invocation of the
  *           memoize method.
  *           Note: All instances must have sensible equals and hashcode implementations as they are
  *           used as part of the key of the inner map of the memo.
  *           Failing this will bring doom and much sadness.
  */
trait HMemo[M[_, _]] {

  /** If given key is not in the table, Evaluates the body, stores the result, and returns it.
    * Otherwise, returns the result already stored in the table.
    *
    * Exact semantics of the table is implementation specific.
    * See implementation documentation for more details.
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
  def memoize[K, V](key: K)(body: => V)(implicit ev: M[K, V]): V

  /** Optionally returns the value associated with a key. */
  def get[K, V](key: K)(implicit ev: M[K, V]): Option[V]

  /** Returns an [[HMap]] of all completed computations currently stored in the memo. */
  def getSnapshot: HMap[M]

  /** Memoizes all key-value pairs in the given map to the memo.
    *
    * @param m The map to be loaded.
    * @param ev Evidence that type [[K]] and type [[V]] are a valid pair for usage in this [[HMemo]]
    *           as defined by [[M]]. Note that the evidence is used together with the key parameter
    *           to form the compound key used for the underlying
    *           [[Memo]] instance.
    * @tparam K Type of the key of the map.
    * @tparam V Type of the value of the map.
    * @return The memo itself after being updated.
    */
  def loadMap[K, V](m: Map[K, V])(implicit ev: M[K, V]): this.type = {
    for {
      (key, value) <- m
    } memoize(key) {value}

    this
  }

  /** Memoizes all key-value pairs in the given map to the memo.
    *
    * @param hmap The [[HMap]] to be loaded
    * @return The memo itself after being updated.
    */
  def loadHMap(hmap: HMap[M]): this.type = {
    hmap.sections foreach {
      section =>
        loadMap(section.mapping)(section.evidence)
    }

    this
  }

}

object HMemo {

  /** Creates a new empty [[HMemo]] for the given type mapping.
    *
    * @tparam M The type mapping for the [[HMemo]].
    */
  def empty[M[_, _]]: HMemo[M] = PersistentHMemo.empty[M]

}
