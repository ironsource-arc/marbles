package marbles

/** A class representing a pair which is allowed by a type mapping [[M]]. */
sealed abstract class HPair[M[_, _]] {

  /** Abstract type of the pair key. */
  type K

  /** Abstract type of the pair value. */
  type V

  /** Evidence that [[K]] and [[V]] are a valid pair under [[M]]. */
  implicit val evidence: M[K, V]

  /** The contained key-value pair. */
  val kv: (K, V)

  /** The contained key. */
  def key: K = kv._1

  /** The contained value. */
  def value: V = kv._2

  override def hashCode(): Int = (evidence, kv).hashCode()

  override def equals(obj: scala.Any): Boolean = obj match {
    case that: HPair[_] => (this.evidence == that.evidence) && (this.kv == that.kv)
    case _ => false
  }

  override def toString: String = s"HPair($evidence, $kv)"

}

object HPair {

  import scala.language.implicitConversions

  implicit def fromPair[M[_, _], KK, VV](pair: (KK, VV))(implicit ev: M[KK, VV]): HPair[M] =
    new HPair[M] {

      type K = KK

      type V = VV
      
      val kv: (K, V) = pair

      implicit val evidence: M[K, V] = ev

    }

}