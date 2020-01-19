package db

import slick.lifted._
import com.merit.modules.products.{ProductID, ProductRow, SoldProductRow, OrderedProductRow, Currency}
import com.merit.modules.brands.{BrandID, BrandRow}
import slick.driver.JdbcProfile
import slick.jdbc.meta.MTable

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext
import java.sql.Timestamp

import com.merit.modules.stockOrders.{StockOrderID, StockOrderRow}
import org.joda.time.DateTime
import com.merit.modules.sales.{SaleID, SaleRow}
import com.merit.modules.users.{UserID, UserRow}
import com.merit.modules.categories.{CategoryRow, CategoryID}
import com.merit.modules.inventoryCount.{InventoryCountBatchRow,InventoryCountProductRow,InventoryCountBatchID,InventoryCountProductID,InventoryCountStatus}

class Schema(val profile: JdbcProfile) {
  import profile.api._

  object CustomColumnTypes {
    implicit val jodaDateTimeType =
      MappedColumnType.base[DateTime, Timestamp](
        dt => new Timestamp(dt.getMillis),
        ts => new DateTime(ts.getTime)
      )

    implicit val currencyType = MappedColumnType.base[Currency, BigDecimal](
      c => c.value,
      bd => Currency.fromDb(bd)
    )

    implicit val myEnumMapper = MappedColumnType.base[InventoryCountStatus.Value, String](
      e => e.toString,
      s => InventoryCountStatus.withName(s)
    )
  }

  class ProductTable(t: Tag) extends Table[ProductRow](t, "products") {
    import CustomColumnTypes._
    def id            = column[ProductID]("id", O.PrimaryKey, O.AutoInc)
    def barcode       = column[String]("barcode")
    def sku           = column[String]("sku")
    def name          = column[String]("name")
    def price         = column[Option[Currency]]("price", O.SqlType("NUMERIC(10, 2)"))
    def discountPrice = column[Option[Currency]]("discountPrice", O.SqlType("NUMERIC(10, 2)"))
    def qty           = column[Int]("qty")
    def variation     = column[Option[String]]("variation")
    def taxRate       = column[Option[Int]]("taxRate")
    def brandId       = column[Option[BrandID]]("brandId")
    def categoryId    = column[Option[CategoryID]]("categoryId")

    def * =
      (
        barcode,
        sku,
        name,
        price,
        discountPrice,
        qty,
        variation,
        taxRate,
        brandId,
        categoryId,
        id
      ).mapTo[ProductRow]

    def brand =
      foreignKey("product_brand_pk", brandId, brands)(_.id, onDelete = ForeignKeyAction.SetNull)
    def category =
      foreignKey("product_category_pk", categoryId, categories)(
        _.id,
        onDelete = ForeignKeyAction.SetNull
      )
  }

  lazy val products = TableQuery[ProductTable]

  class CategoryTable(t: Tag) extends Table[CategoryRow](t, "categories") {
    def id   = column[CategoryID]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name", O.Unique)

    def * = (name, id).mapTo[CategoryRow]
  }

  lazy val categories = TableQuery[CategoryTable]

  class BrandTable(t: Tag) extends Table[BrandRow](t, "brands") {
    def id   = column[BrandID]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name", O.Unique)

    def * = (name, id).mapTo[BrandRow]
  }

  lazy val brands = TableQuery[BrandTable]

  class SaleTable(t: Tag) extends Table[SaleRow](t, "sales") {
    import CustomColumnTypes._
    def id        = column[SaleID]("id", O.PrimaryKey, O.AutoInc)
    def createdAt = column[DateTime]("created")
    def total     = column[Currency]("total")
    def discount  = column[Currency]("discount")

    def * = (createdAt, total, discount, id).mapTo[SaleRow]
  }

  lazy val sales = TableQuery[SaleTable]

  class SoldProductTable(t: Tag) extends Table[SoldProductRow](t, "sold_products") {
    def id        = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def productId = column[ProductID]("productId")
    def saleId    = column[SaleID]("saleId")
    def qty       = column[Int]("qty")
    def synced    = column[Boolean]("synced")

    def productFk = foreignKey("sold_product_fk", productId, products)(_.id)
    def saleFk    = foreignKey("sale_fk", saleId, sales)(_.id)

    def * = (productId, saleId, qty, synced, id).mapTo[SoldProductRow]
  }

  lazy val soldProducts = TableQuery[SoldProductTable]

  class UserTable(t: Tag) extends Table[UserRow](t, "users") {
    def id       = column[UserID]("id", O.PrimaryKey)
    def email    = column[String]("email")
    def name     = column[String]("name")
    def password = column[String]("password")

    def * = (email, name, password, id).mapTo[UserRow]
  }

  lazy val users = TableQuery[UserTable]

  class StockOrderTable(t: Tag) extends Table[StockOrderRow](t, "stock_orders") {
    import CustomColumnTypes._
    def id      = column[StockOrderID]("id", O.PrimaryKey, O.AutoInc)
    def created = column[DateTime]("created")

    def * = (created, id).mapTo[StockOrderRow]
  }

  lazy val stockOrders = TableQuery[StockOrderTable]

  class OrderedProductsTable(t: Tag) extends Table[OrderedProductRow](t, "ordered_products") {
    def id           = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def productId    = column[ProductID]("productId")
    def stockOrderId = column[StockOrderID]("stockOrderId")
    def qty          = column[Int]("qty")
    def synced       = column[Boolean]("synced")

    def productFk    = foreignKey("ordered_product_fk", productId, products)(_.id)
    def stockOrderFk = foreignKey("stock_order_fk", stockOrderId, stockOrders)(_.id)

    def * = (productId, stockOrderId, qty, synced, id).mapTo[OrderedProductRow]
  }

  lazy val orderedProducts = TableQuery[OrderedProductsTable]

  class InventoryCountBatchTable(t: Tag)
      extends Table[InventoryCountBatchRow](t, "inventory_count_batches") {
    import CustomColumnTypes._

    def id         = column[InventoryCountBatchID]("id", O.PrimaryKey, O.AutoInc)
    def status     = column[InventoryCountStatus.Value]("status")
    def started    = column[DateTime]("started")
    def finished   = column[Option[DateTime]]("finished")
    def name       = column[Option[String]]("name")
    def categoryId = column[Option[CategoryID]]("categoryId")
    def brandId    = column[Option[BrandID]]("brandId")

    def categoryFk = foreignKey("inventory_count_category_fk", categoryId, categories)(_.id)
    def brandFk    = foreignKey("inventory_count_brand_fk", brandId, brands)(_.id)

    def * = (started, finished, name, categoryId, brandId, status, id).mapTo[InventoryCountBatchRow]
  }

  lazy val inventoryCountBatches = TableQuery[InventoryCountBatchTable]

  class InventoryCountProductsTable(t: Tag)
      extends Table[InventoryCountProductRow](t, "inventory_count_products") {
    def id        = column[InventoryCountProductID]("id", O.PrimaryKey, O.AutoInc)
    def batchId   = column[InventoryCountBatchID]("batchId")
    def productId = column[ProductID]("productId")
    def expected  = column[Int]("expected")
    def counted   = column[Option[Int]]("counted")
    def synced    = column[Boolean]("synced")

    def batchFk   = foreignKey("inventory_count_batch_fk", batchId, inventoryCountBatches)(_.id)
    def productFk = foreignKey("inventory_count_product_fk", productId, products)(_.id)

    def * = (batchId, productId, expected, counted, synced, id).mapTo[InventoryCountProductRow]
  }

  lazy val inventoryCountProducts = TableQuery[InventoryCountProductsTable]

  def createTables(db: Database)(implicit ec: ExecutionContext): Seq[Unit] = {
    val tables =
      Seq(
        brands,
        categories,
        products,
        sales,
        soldProducts,
        users,
        stockOrders,
        orderedProducts,
        inventoryCountBatches,
        inventoryCountProducts
      )
    val existing = db.run(MTable.getTables)

    val f = existing.flatMap(ts => {
      val existingTableNames = ts.map(_.name.name)
      val createIfNotExists = tables
        .filterNot(t => existingTableNames.contains(t.baseTableRow.tableName))
        .map(_.schema.create)
      db.run(DBIO.sequence(createIfNotExists))
    })

    Await.result(f, Duration.Inf)
  }
}

object Schema {
  def apply(profile: JdbcProfile) = new Schema(profile)
}
