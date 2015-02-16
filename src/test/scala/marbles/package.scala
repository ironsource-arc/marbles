package object marbles {

  trait M[K, V]

  implicit case object SI extends M[String, Int]

  implicit case object IS extends M[Int, String]

  val emptyHMap = HMap.empty[M]

  val foo1 = "foo" -> 1
  val bar2 = "bar" -> 2

  val i3Hello = 3 -> "hello"
  val i4World = 4 -> "world"

}
