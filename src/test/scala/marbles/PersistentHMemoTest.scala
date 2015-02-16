package marbles

import org.scalatest.{FunSpec, Matchers}

/** This test only checks heterogeneous-related issues, not memoization issues. */
class PersistentHMemoTest extends FunSpec with Matchers {

  // Notice definitions at the package object

  it("memoize then getSnapshot") {
    val hmemo = PersistentHMemo.empty[M]

    hmemo.memoize("foo") {1}
    hmemo.memoize(3) {"hello"}
    hmemo.memoize(3) {"world"}

    hmemo.getSnapshot shouldBe emptyHMap + foo1 + i3Hello

  }

}
