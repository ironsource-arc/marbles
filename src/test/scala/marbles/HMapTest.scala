package marbles

import org.scalatest.{FunSpec, Matchers}

class HMapTest extends FunSpec with Matchers {

  // Notice definitions at the package object

  describe("Usage examples") {

    it("Extensible Record") {
      trait PersonAttributes[K, V]

      case object Name {
        implicit case object NameEv extends PersonAttributes[Name.type , String]
      }


      case object Age {
        implicit case object HeightEv extends PersonAttributes[Age.type , Int]
      }

      case object Height {
        implicit case object HeightEv extends PersonAttributes[Height.type , Double]
      }

      // We can keep on adding attributes as software evolves.

      val person = HMap[PersonAttributes](Name -> "Jimmy", Age -> 4)

      person.get(Name) shouldBe Some("Jimmy")

      person.get(Height) shouldBe None

      val afterBithday = person + (Age -> (person(Age) + 1))

      afterBithday(Age) shouldBe 5

      }

    describe("Dealing with generic types") {

      trait SpecialType[T]

      object SpecialType {

        implicit case object IntIsSpecial extends SpecialType[Int]

        implicit case object StringIsSpecial extends SpecialType[String]

      }

      it("Mapping") {

        trait SpecialMapping[K, V]

        object SpecialMapping {

          private case class ConcreteSpecialMapping[K](stEv: SpecialType[K])
            extends SpecialMapping[K, Seq[K]]

          implicit def genEv[T: SpecialType]: SpecialMapping[T, Seq[T]] =
            ConcreteSpecialMapping[T](implicitly[SpecialType[T]])

        }

        val mapping =
          HMap[SpecialMapping](1 -> Seq(1, 2), 2 -> Seq(3, 4), "foo" -> Seq("bar", "baz"))

        "HMap[SpecialMapping](1 -> Seq(\"foo\"))" shouldNot compile

        mapping.getMap[Int, Seq[Int]].values.flatten.sum shouldBe 10
      }

      it("type switch") {

        object SpecialFunction {

          private case object Key

          private trait SpecialMapping[K, V]

          private object SpecialMapping {

            private case class ConcreteSpecialMapping[T](stEv: SpecialType[T])
              extends SpecialMapping[Key.type , T => T]

            implicit def genEv[T: SpecialType]: SpecialMapping[Key.type, T => T] =
              ConcreteSpecialMapping[T](implicitly[SpecialType[T]])

          }

          private def intFunction(i: Int): Int = i * 3

          private def stringFunction(s: String): String = s + s

          private val functionMap =
            HMap[SpecialMapping](
              Key -> intFunction _,
              Key -> stringFunction _
            )

          // Note how we only need to know that T is a SpecialType, not which type it is!
          // This is stronger than shapeless Poly where resolution happens at compile time.
          def apply[T : SpecialType](x : T): T = functionMap.apply(Key).apply(x)

        }

        SpecialFunction(5) shouldBe 15
        SpecialFunction("foo") shouldBe "foofoo"


      }
    }

    it("Annoying subtyping issue") {

      sealed abstract class Animal

      case object Cow extends Animal

      case object Horse extends Animal

      trait AnimalMapping[K, V]

      object AnimalMapping {

        implicit case object Ev extends AnimalMapping[Animal, Animal]

      }

      // note how we need to provide a type annotation each time we use a subtype of animal.
      // Ideally, we should not need to write them.
      // Related Stack Overflow question: http://tinyurl.com/hmap-so-question

      val mapping = HMap[AnimalMapping]((Cow : Animal) -> (Horse : Animal))

      mapping(Cow : Animal) shouldBe Horse

      mapping + ((Cow : Animal) -> (Cow : Animal)) shouldBe
        HMap[AnimalMapping]((Cow : Animal) -> (Cow : Animal))

    }
  }



  def check(left: HMap[M], right: HMap[M], shouldEqual: Boolean) = {
    (left == right) shouldBe shouldEqual
    (left.hashCode == right.hashCode) shouldBe shouldEqual
  }

  describe("hashcode & equals") {

    it("empty & empty") {
      check(emptyHMap, HMap.empty[M], shouldEqual = true)
    }

    it("empty and one element") {
      check(emptyHMap, emptyHMap + foo1, shouldEqual = false)
    }

    it("same one element") {
      check(emptyHMap + foo1, emptyHMap + foo1, shouldEqual = true)
    }

    it("different one element") {
      check(emptyHMap + foo1, emptyHMap + bar2, shouldEqual = false)
      check(emptyHMap + foo1, emptyHMap + i3Hello, shouldEqual = false)
    }

    it("insertion order should not matter") {
      check(emptyHMap + foo1 + bar2, emptyHMap + bar2 + foo1, shouldEqual = true)
      check(emptyHMap + foo1 + i3Hello, emptyHMap + i3Hello + foo1, shouldEqual = true)
    }

  }

  describe("toString") {
    it("no elements") {
      emptyHMap.toString shouldBe "HMap()"
    }

    it("one element") {
      (emptyHMap + foo1).toString shouldBe """HMap(SI -> Map(foo -> 1))"""
    }
  }

  describe("getMap, get, apply") {
    it("compilation errors") {
      "emptyHMap.getMap[String, String]" shouldNot compile
      "emptyHMap.get(4.5)" shouldNot compile
      "emptyHMap(4.5)" shouldNot compile
    }

    it("empty map") {
      emptyHMap.getMap[String, Int] shouldBe Map.empty
      emptyHMap.get(1) shouldBe None
      intercept[NoSuchElementException](emptyHMap(1))
    }

    it("one element") {
      val oneElem = emptyHMap + foo1
      oneElem.getMap[Int, String] shouldBe Map.empty
      oneElem.getMap[String, Int] shouldBe Map(foo1)
      oneElem.get(1) shouldBe None
      oneElem.get("foo") shouldBe Some(1)
      intercept[NoSuchElementException](oneElem(1))
      oneElem("foo") shouldBe 1
    }
  }

  describe("updatedWithMap") {
    val oneElem = emptyHMap + foo1

    it("non existent to existent") {
      emptyHMap.updatedWithMap(Map(foo1)) shouldBe oneElem
    }

    it("change existent map") {
      oneElem.updatedWithMap(Map(bar2)).get("foo") shouldBe None
      oneElem.updatedWithMap(Map(bar2)).get("bar") shouldBe Some(2)
    }

    it("existent to non existent") {
      oneElem.updatedWithMap(Map.empty[String, Int]).get("foo") shouldBe None
      oneElem.updatedWithMap(Map.empty[String, Int]).getMap[String, Int] shouldBe Map.empty
      oneElem.updatedWithMap(Map.empty[String, Int]) shouldBe emptyHMap
    }

    it("should not interfere with unrelated type mapping") {
      oneElem.updatedWithMap(Map(i3Hello)).getMap[String, Int] shouldBe Map(foo1)
      oneElem.updatedWithMap(Map(i3Hello)).getMap[Int, String] shouldBe Map(i3Hello)
    }
  }

  describe("sections and hpairs") {
    it("empty HMap") {
      emptyHMap.sections shouldBe Set.empty
      emptyHMap.hpairs shouldBe Set.empty
    }

    it("one subsection") {
      val oneElem = emptyHMap + foo1

      oneElem.sections.map {_.mapping} shouldBe Set(Map(foo1))
      oneElem.sections.map {_.evidence} shouldBe Set(SI)

      oneElem.hpairs shouldBe Set(foo1 : HPair[M])
    }

    it("two subsections") {
      val twoElems = emptyHMap + foo1 + i3Hello

      twoElems.sections.map {_.mapping} shouldBe Set(Map(foo1), Map(i3Hello))
      twoElems.sections.map {_.evidence} shouldBe Set(SI, IS)

      twoElems.hpairs shouldBe Set(foo1 : HPair[M], i3Hello : HPair[M])

    }

  }

  describe("insertion") {
    it("+ overwrite") {
      (emptyHMap + foo1 + bar2 + i3Hello + ("foo" -> 2)) shouldBe
        emptyHMap + bar2 + i3Hello + ("foo" -> 2)
    }

    it("++ (Map)") {
      (emptyHMap + foo1 + bar2 + i3Hello ++ Map("foo" -> 2, "baz" -> 66)) shouldBe
        emptyHMap + bar2 + i3Hello + ("foo" -> 2) + ("baz" -> 66)
    }

    it("++ (HMap)") {
      ((emptyHMap + foo1 + bar2 + i3Hello) ++ (emptyHMap + ("foo" -> 2) + i4World)) shouldBe
        emptyHMap + ("foo" -> 2) + bar2 + i3Hello + i4World
    }
  }

  describe("removal") {
    it("-") {
      (emptyHMap + foo1 - "foo") shouldBe emptyHMap
      (emptyHMap - "foo") shouldBe emptyHMap
      (emptyHMap + foo1 + i3Hello - "foo") shouldBe emptyHMap + i3Hello
      (emptyHMap + bar2 + i3Hello - "foo") shouldBe emptyHMap + bar2 + i3Hello
    }

    it("-- (Map)") {
      (emptyHMap + foo1 -- Seq("foo")) shouldBe emptyHMap
      (emptyHMap -- Seq("foo")) shouldBe emptyHMap
      (emptyHMap + foo1 + bar2 + i3Hello -- Seq("foo")) shouldBe emptyHMap + bar2 + i3Hello
    }

    it("-- (HMap)") {
      (emptyHMap + foo1 -- (emptyHMap + foo1)) shouldBe emptyHMap
      (emptyHMap -- (emptyHMap + foo1)) shouldBe emptyHMap
      (emptyHMap + foo1 + bar2 + i3Hello + i4World -- (emptyHMap + foo1 + i3Hello)) shouldBe
        emptyHMap + bar2 + i4World
    }
  }

  describe("HMap creation") {
    it("using HMap.apply") {
      HMap[M]("foo" -> 1, 3 -> "hello") shouldBe emptyHMap + foo1 + i3Hello
    }
  }

  describe("toHMemo") {
    it("ot memo & back") {
      val hMap = emptyHMap + foo1 + i3Hello

      hMap.toHMemo.getSnapshot shouldBe hMap

      val hmemo = hMap.toHMemo

      hmemo.memoize("foo") {fail(); 99} shouldBe 1
      hmemo.memoize(3) {fail(); "lala"} shouldBe "hello"
    }
  }

}
