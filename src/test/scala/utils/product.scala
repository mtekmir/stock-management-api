package utils
import com.merit.modules.products.ProductRow
import scala.util.Random
import com.merit.modules.products.ProductDTO
import com.merit.modules.products.ProductID
import com.merit.modules.excel.ExcelProductRow

object ProductUtils {
  private val random                       = Random
  private def randomFrom(col: Seq[String]) = col.drop(Random.nextInt(col.size)).head
  def randomBetween(n: Int)                = Random.nextInt(n)
  def randomPrice                          = Random.nextDouble()
  def randomQty                            = Random.nextInt(20)
  def randomBarcode                        = randomBetween(100000000).toString
  def randomSku                            = randomBetween(1000000).toString
  def randomProductName                    = randomFrom(products)

  val products = Seq(
    "Nike Air Zoom",
    "Asics Nebousa",
    "Babolat Pure Strike",
    "Under Armour Running Shorts",
    "Uniqlo T-Shirt",
    "Nike T-Shirt",
    "Under Armour T-Shirt",
    "Nike Cage 3",
    "Under Armour Sweatpants"
  )

  def createProduct: ProductRow =
    ProductRow(
      randomBarcode,
      randomSku,
      randomProductName,
      randomPrice,
      randomQty,
      Some("variation"),
      None,
      None
    )

  def rowToDTO(
    row: ProductRow,
    b: Option[String] = None,
    c: Option[String] = None
  ): ProductDTO = {
    import row._
    ProductDTO(ProductID.zero, barcode, sku, name, price, qty, variation, b, c)
  }

  def excelRowToDTO(
    row: ExcelProductRow
  ): ProductDTO = {
    import row._
    ProductDTO(ProductID.zero, barcode, sku, name, price, qty, variation, brand, category)
  }
}
