package utils

import scala.util.Random
import com.merit.modules.products.ProductRow

object TestUtils {
  private val random                       = Random
  private def randomFrom(col: Seq[String]) = col.drop(Random.nextInt(col.size)).head
  def randomBrandName                      = randomFrom(brands)
  def randomCategoryName                   = randomFrom(categories)

  val brands     = Seq("Babolat", "Nike", "Asics", "Head", "Under Armour", "Uniqlo")
  val categories = Seq("Shoes", "Shorts", "Skirts", "T-Shirts", "Pants", "Sweaters")
}
