package utils

import com.merit.modules.products.ProductRow
import scala.util.Random
import com.merit.modules.products.ProductDTO
import com.merit.modules.products.ProductID
import com.merit.modules.excel.ExcelProductRow
import scala.math.BigDecimal.RoundingMode
import com.merit.modules.products.Currency
import com.merit.modules.sales.SaleDTOProduct
import com.merit.modules.sales.SaleSummaryProduct
import com.merit.modules.stockOrders.StockOrderSummaryProduct
import com.merit.modules.products.CreateProductRequest
import com.merit.modules.products.EditProductRequest

object ProductUtils {
  private val random                       = Random
  private def randomFrom(col: Seq[String]) = col.drop(Random.nextInt(col.size)).head
  def randomBetween(n: Int)                = Random.nextInt(n).abs
  def randomPrice                          = Currency.from((Random.nextInt(1000).abs + Random.nextDouble()).toString)

  def randomQty         = Random.nextInt(20)
  def randomBarcode     = randomBetween(100000000).toString
  def randomSku         = randomBetween(1000000).toString
  def randomProductName = randomFrom(products)

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
      randomPrice,
      randomQty,
      Some("variation"),
      Some(randomBetween(20)),
      None,
      None
    )

  def rowToDTO(
    row: ProductRow,
    b: Option[String] = None,
    c: Option[String] = None
  ): ProductDTO = {
    import row._
    ProductDTO(
      id,
      barcode,
      sku,
      name,
      price,
      discountPrice,
      qty,
      variation,
      taxRate,
      b,
      c
    )
  }

  def excelRowToDTO(
    row: ExcelProductRow
  ): ProductDTO = {
    import row._
    ProductDTO(
      ProductID.zero,
      barcode,
      sku,
      name,
      price,
      discountPrice,
      qty,
      variation,
      taxRate,
      brand,
      category
    )
  }

  def excelRowToSaleDTOProduct(row: ExcelProductRow): SaleDTOProduct = {
    import row._
    SaleDTOProduct(
      ProductID.zero,
      barcode,
      sku,
      name,
      price,
      discountPrice,
      qty,
      variation,
      taxRate,
      brand,
      category,
      false
    )
  }

  implicit class C1(
    val r: ProductRow
  ) {
    def toCreateProductReq(): CreateProductRequest = {
      import r._
      CreateProductRequest(
        barcode,
        sku,
        name,
        price,
        discountPrice,
        qty,
        variation,
        taxRate,
        brandId,
        categoryId
      )
    }

    def toEditProductReq(): EditProductRequest = {
      import r._
      EditProductRequest(
        Some(barcode),
        Some(sku),
        Some(name),
        price.map(_.value.toString),
        discountPrice.map(_.value.toString),
        Some(qty.toString),
        variation,
        taxRate.map(_.toString),
        brandId.map(_.toString),
        categoryId.map(_.toString)
      )
    }
  }

  def productRowToSaleSummaryProduct(p: ProductRow, soldQty: Int) = {
    import p._
    SaleSummaryProduct(p.id, p.barcode, p.name, p.variation, p.qty, soldQty)
  }

  def productRowToStockOrderSummaryProduct(p: ProductRow, ordered: Int) = {
    import p._
    StockOrderSummaryProduct(p.id, p.barcode, p.name, p.variation, p.qty, ordered)
  }

  def sortedWithZeroIdProductDTO(products: Seq[ProductDTO]): Seq[ProductDTO] =
    products.sortBy(_.barcode).map(_.copy(id = ProductID.zero))
}
