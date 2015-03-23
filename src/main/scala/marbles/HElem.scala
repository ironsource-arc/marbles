package marbles

/** A class representing an element of type adhering to the [[M]] typeclass. */
sealed abstract class HElem[M[_]] {

  /** Abstract type of the element. */
  type T

  /** Evidence that [[T]] is a valid type under [[M]]. */
  implicit val evidence: M[T]

  /** The contained element. **/
  val element: T

  override def hashCode(): Int = (evidence, element).hashCode()

  override def equals(obj: scala.Any): Boolean = obj match {
    case that: HElem[_] => (this.evidence == that.evidence) && (this.element == that.element)
    case _ => false
  }

  override def toString: String = s"HPair($evidence, $element)"

}

object HElem {

  import scala.language.implicitConversions

  implicit def fromElem[M[_], TT](elem: TT)(implicit ev: M[TT]): HElem[M] =
    new HElem[M] {

      type T = TT

      val element: T = elem

      implicit val evidence: M[T] = ev

    }

}
