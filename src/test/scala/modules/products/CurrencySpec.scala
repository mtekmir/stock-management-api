package modules.products
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import com.merit.modules.products.Currency

class CurrencySpec extends Specification {
  "Currency" >> {
    "should instantiate from string" in new Scope {
      val c = Currency.from("999")
      c must beEqualTo(Some(Currency(999.00)))
      c.map(_.value.toString) must beEqualTo(Some("999.00"))
    }

    "should instantiate from string - 2" in new Scope {
      val c = Currency.from(1.toString)
      c must beEqualTo(Some(Currency(1.00)))
      c.map(_.value.toString) must beEqualTo(Some("1.00"))
    }

    "should instantiate from big decimal" in new Scope {
      val c = Currency.fromDb(BigDecimal(10))
      c must beEqualTo(Currency(10.00))
      c.value.toString must beEqualTo("10.00")
    }

    "should validate an input" in new Scope {
      val c = Currency.isValid("1")
      val c1 = Currency.isValid("asd")
      val c2 = Currency.isValid("0.9")
      val c3 = Currency.isValid("900.99")
      val c4 = Currency.isValid(" ")

      c must beTrue
      c1 must beFalse
      c2 must beTrue
      c3 must beTrue
      c4 must beFalse
    }
  }
}