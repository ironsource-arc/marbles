package marbles

import org.scalatest.{Matchers, FunSpec}

class HPairTest extends FunSpec with Matchers {

  val hpair: HPair[M] = foo1

  it("HPair should have the correct kv") {
    hpair.kv shouldBe foo1
  }

  it("HPair should have the correct key") {
    hpair.key shouldBe "foo"
  }

  it("HPair should have the correct value") {
    hpair.value shouldBe 1
  }

  it("HPair should have the correct evidence") {
    hpair.evidence shouldBe SI
  }

}
