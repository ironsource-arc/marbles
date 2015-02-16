package marbles

/** Represents a section of an [[HMap]] which maps two specific types.
  *
  * These sections are created by [[HMap.sections]] which gives all sections of a map.
  *
  * The consumer of the section will not know what are the types for the key and value of the contained map,
  * but will know that they are a valid pair under [[M]].
  *
  * @tparam M The type mapping for the [[HMap]]
  */
abstract class HMapSection[M[_, _]] extends Serializable {

  /** Abstract type of the contained mapping's keys. */
  type K

  /** Abstract type of the contained mapping's values. */
  type V

  /** Evidence that [[K]] and [[V]] are a valid pair under [[M]].
    *
    * This can be used to re-insert values in [[mapping]] to another [[HMap]] or [[HMemo]].
    */
  implicit val evidence: M[K, V]

  /** A Map from keys of type [[K]] to values of type [[V]]. */
  val mapping: Map[K, V]

  override def hashCode(): Int = (evidence, mapping).hashCode()

  override def equals(obj: scala.Any): Boolean = obj match {
    case that: HMapSection[_] => (this.evidence == that.evidence) && (this.mapping == that.mapping)
    case _ => false
  }

  override def toString = s"HMapSection($evidence, $mapping)"
}
