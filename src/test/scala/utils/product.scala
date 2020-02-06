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
import com.merit.modules.categories.CategoryRow
import com.merit.modules.brands.BrandRow
import com.merit.modules.brands.BrandID
import com.merit.modules.categories.CategoryID

object ProductUtils {
  private val random                       = Random
  private def randomFrom(col: Seq[String]) = col.drop(Random.nextInt(col.size)).head
  def randomBetween(n: Int)                = Random.nextInt(n).abs
  def randomPrice =
    Currency.fromOrZero((Random.nextInt(1000).abs + Random.nextDouble()).toString)
  def randomDiscountPrice =
    Currency.from((Random.nextInt(1000).abs + Random.nextDouble()).toString)

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
      randomDiscountPrice,
      randomQty,
      Some("variation"),
      Some(randomBetween(20)),
      None,
      None
    )

  def rowToDTO(
    row: ProductRow,
    b: Option[BrandRow] = None,
    c: Option[CategoryRow] = None
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
      brand.map(BrandRow(_)),
      category.map(CategoryRow(_))
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
        Some(price.value.toString),
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

  def productRowToSaleDTOProduct(p: ProductRow, soldQty: Int) = {
    import p._
    SaleDTOProduct(id, barcode, sku, name, price, discountPrice, soldQty, variation, taxRate, None, None, true)
  }

  def productRowToStockOrderSummaryProduct(p: ProductRow, ordered: Int) = {
    import p._
    StockOrderSummaryProduct(p.id, p.barcode, p.name, p.variation, p.qty, ordered)
  }

  def sortedWithZeroIdProductDTO(products: Seq[ProductDTO]): Seq[ProductDTO] =
    products.sortBy(_.barcode).map(_.copy(id = ProductID.zero))

    implicit class DTOOPS(
    rows: Seq[ProductDTO]
  ) {
    def withZeroIds: Seq[ProductDTO] =
      rows.map(
        p =>
          p.copy(
            brand = p.brand.map(_.copy(id = BrandID(0))),
            category = p.category.map(_.copy(id = CategoryID(0))),
            id = ProductID.zero
          )
      )
  }
  
}
