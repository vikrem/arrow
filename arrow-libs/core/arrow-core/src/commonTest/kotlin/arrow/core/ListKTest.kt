package arrow.core

import arrow.typeclasses.Monoid
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arrow.core.MonoidLaws
import io.kotest.property.arrow.laws.testLaws
import io.kotest.property.checkAll

class ListKTest : StringSpec() {

  init {

    testLaws(MonoidLaws.laws(Monoid.list(), Arb.list(Arb.int())))

    "mapNotNull() should map list and filter out null values" {
      checkAll(Arb.list(Arb.int())) { listk ->
        listk.mapNotNull {
          when (it % 2 == 0) {
            true -> it.toString()
            else -> null
          }
        } shouldBe listk.toList().filter { it % 2 == 0 }.map { it.toString() }
      }
    }
  }
}
