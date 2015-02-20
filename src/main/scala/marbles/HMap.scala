package marbles

/** A heterogeneous immutable map which allows different key-value type combinations to be stored
  * together.
  *
  * The type combinations are specified and statically checked at compile time by parameterizing
  * the [[HMap]] with a type mapping.
  *
  * @tparam M The type mapping for this [[HMap]].
  *           Instances of this class are required and implicitly used when retrieving,
  *           adding or removing items from the map.
  *           Note: All instances must have sensible equals and hashCode implementations as they are
  *           used as part of the compound key of the inner map used to
  *           store the items in this [[HMap]]. Failing this will bring doom and much sadness.
  */
abstract class HMap[M[_, _]] extends Serializable {
  outer =>

  /** Internal map used to store the items for this [[HMap]].
    *
    * Type mapping evidence is used as a key for maps of specific type combinations.
    * The maps have existential types, but because we use type mapping evidence for all operations,
    * we can safely use type casting on them.
    */
  protected val unsafeMap: Map[M[_, _], Map[_, _]]

  override def hashCode(): Int = unsafeMap.hashCode()

  override def equals(obj: Any): Boolean =
    obj match {
      case that: HMap[_] => this.unsafeMap == that.unsafeMap
      case _ => false
    }

  override def toString: String = "H" + unsafeMap.toString

  /** Get a map between two specific types. Returned map may be empty. */
  def getMap[K, V](implicit ev: M[K, V]): Map[K, V] = {
    val result: Map[_, _] = unsafeMap.getOrElse(ev, Map.empty)

    result.asInstanceOf[Map[K, V]]
  }

  /** Optionally returns the value associated with a key. */
  def get[K, V](key: K)(implicit ev: M[K, V]): Option[V] = getMap[K, V].get(key)

  /** Retrieves the value which is associated with the given key.
    *
    * @throws NoSuchElementException if no value is stored for the given key.
    */
  def apply[K, V](key: K)(implicit ev: M[K, V]): V = getMap[K, V].apply(key)

  /** Get a new [[HMap]] with an updated mapping between two types.
    *
    * @param m the map which represents the new mapping between keys of type
    *          [[K]] and values of type [[V]].
    */
  def updatedWithMap[K, V](m: Map[K, V])(implicit ev: M[K, V]): HMap[M] =
    new HMap[M] {
      override protected val unsafeMap: Map[M[_, _], Map[_, _]] = {
        if (m.isEmpty)
          outer.unsafeMap - ev
        else
          outer.unsafeMap + (ev -> m)
      }
    }

  /** Returns a set of [[HMapSection]] objects, each containing a map between two specific types
    * together with the type mapping evidence.
    * The types themselves are anonymous and unknown to the consumer, but the evidence is enough
    * to allow re-insertion to other [[HMap]]s or [[HMemo]]s with the same type mapping [[M]].
    */
  def sections: Set[HMapSection[M]] = {

    unsafeMap.toSet[(M[_, _], Map[_, _])] map {
      case (ev, m) =>
        new HMapSection[M] {
          override val mapping: Map[K, V] = m.asInstanceOf[Map[K, V]]
          override val evidence: M[K, V] = ev.asInstanceOf[M[K, V]]
        }
    }
  }

  def hpairs: Set[HPair[M]] =
    for {
      section <- sections
      kv <- section.mapping
    } yield {
      import section.{evidence => ev}
      /*_*/ kv : HPair[M] /*_*/
    }

  /** Add a key/value pair to this [[HMap]], returning a new [[HMap]]. */
  def +[K, V](pair: (K, V))(implicit ev: M[K, V]): HMap[M] =
    updatedWithMap[K, V](getMap[K, V] + pair)

  /** Adds a collection of key-value pairs of a given key-value type pair, returning a new [[HMap]].
    */
  def ++[K, V](pairs: TraversableOnce[(K, V)])(implicit ev: M[K, V]): HMap[M] =
    updatedWithMap[K, V](getMap[K, V] ++ pairs)

  /** Adds two [[HMap]]s to produce a new one. Key-value pairs from the right operand override pairs
    * from the left operand
    */
  def ++(that: HMap[M]): HMap[M] = {
    that.sections.foldLeft(this) {
      case (acc, section) =>
        import section.evidence
        acc ++ section.mapping
    }
  }

  /** Removes a key from  this [[HMap]], returning a new [[HMap]]. */
  def -[K, V](key: K)(implicit ev: M[K, V]): HMap[M] =
    updatedWithMap(getMap[K, V] - key)

  /** Removes a collection of keys of a given key-value type pair, returning a new [[HMap]]. */
  def --[K, V](keys: TraversableOnce[K])(implicit ev: M[K, V]): HMap[M] =
    updatedWithMap(getMap[K, V] -- keys)

  /** Removes the keys form the given [[HMap]] from this [[HMap]]to produce a new one. */
  def --(that: HMap[M]): HMap[M] = {
    that.sections.foldLeft(this) {
      case (acc, section) =>
        import section.evidence
        acc -- section.mapping.keys
    }
  }

  /** Creates a new [[HMemo]] of the same type mapping [[M]], and populate it with
    * all key-value pairs from this [[HMap]].
    */
  def toHMemo: HMemo[M] =
    HMemo.empty[M].loadHMap(this)

}

object HMap {

  /** Creates a new empty [[HMap]] for the given type mapping.
    *
    * @tparam M The type mapping for the [[HMap]].
    */
  def empty[M[_, _]]: HMap[M] = new HMap[M] {
    override protected val unsafeMap: Map[M[_, _], Map[_, _]] = Map.empty
  }

  def apply[M[_, _]](pairs: HPair[M]*): HMap[M] =
    pairs.foldLeft(HMap.empty[M]) {
      case (acc, pair) =>
        import pair.evidence
        acc + pair.kv
    }

}
