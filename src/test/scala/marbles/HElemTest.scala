package marbles

import org.scalatest.{Matchers, FunSpec}

class HElemTest extends FunSpec with Matchers {

  val helem: HElem[TC] = "foo"

  it("HPair should have the correct element") {
    helem.element shouldBe "foo"
  }

  it("HPair should have the correct evidence") {
    helem.evidence shouldBe TCS
  }

}
