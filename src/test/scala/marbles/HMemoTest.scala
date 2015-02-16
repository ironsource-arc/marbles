package marbles

import org.scalatest.{FunSpec, Matchers}

class HMemoTest extends FunSpec with Matchers {
  // Notice definitions at the package object

  val siMap = Map(foo1, bar2)
  val hmap = emptyHMap + ("foo" -> 5) + i3Hello

  it("loadMap then getSnapshot") {
    val hmemo = PersistentHMemo.empty[M]

    hmemo.loadMap(siMap)

    hmemo.memoize("foo") {5}

    hmemo.getSnapshot shouldBe emptyHMap ++ siMap
  }

  it("loadHMap then getSnapshot") {
    val hmemo = PersistentHMemo.empty[M]

    hmemo.memoize("foo") {1}

    hmemo.loadHMap(hmap)

    hmemo.getSnapshot shouldBe emptyHMap + foo1 + i3Hello
  }

}
