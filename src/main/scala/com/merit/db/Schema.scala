package db

import slick.lifted._
import com.merit.modules.products.{ProductID, ProductRow, SoldProductRow, OrderedProductRow, Currency}
import com.merit.modules.brands.{BrandID, BrandRow}
import slick.driver.JdbcProfile
import scala.concurrent.ExecutionContext
import java.sql.Timestamp
import com.merit.modules.stockOrders.{StockOrderID, StockOrderRow}
import org.joda.time.DateTime
import com.merit.modules.sales.{SaleID, SaleRow, SaleOutlet, SaleStatus}
import com.merit.modules.users.{UserID, UserRow}
import com.merit.modules.categories.{CategoryRow, CategoryID}
import com.merit.modules.inventoryCount.{InventoryCountBatchRow, InventoryCountProductRow, InventoryCountBatchID, InventoryCountProductID, InventoryCountStatus}
import com.merit.modules.salesEvents.{SaleEventRow, SaleEventID, SaleEventType}
import com.merit.db.DbMappers

class Schema(val profile: JdbcProfile) extends DbMappers {
  import profile.api._

  class ProductTable(t: Tag) extends Table[ProductRow](t, "products") {
    def id            = column[ProductID]("id", O.PrimaryKey, O.AutoInc)
    def barcode       = column[String]("barcode", O.Unique)
    def sku           = column[String]("sku")
    def name          = column[String]("name")
    def price         = column[Currency]("price", O.SqlType("NUMERIC(10, 2)"))
    def discountPrice = column[Option[Currency]]("discountPrice", O.SqlType("NUMERIC(10, 2)"))
    def qty           = column[Int]("qty")
    def variation     = column[Option[String]]("variation")
    def taxRate       = column[Option[Int]]("taxRate")
    def brandId       = column[Option[BrandID]]("brandId")
    def categoryId    = column[Option[CategoryID]]("categoryId")
    def deleted       = column[Boolean]("deleted")

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
        deleted,
        id
      ).mapTo[ProductRow]

    def brand =
      foreignKey("product_brand_pk", brandId, brands)(
        _.id,
        onDelete = ForeignKeyAction.SetNull
      )
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
    def id        = column[SaleID]("id", O.PrimaryKey, O.AutoInc)
    def createdAt = column[DateTime]("created")
    def total     = column[Currency]("total")
    def discount  = column[Currency]("discount")
    def outlet    = column[SaleOutlet.Value]("outlet")
    def status    = column[SaleStatus.Value]("status")
    def orderNo   = column[Option[String]]("order_no")

    def * = (createdAt, total, discount, outlet, status, orderNo, id).mapTo[SaleRow]
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
    def id         = column[InventoryCountBatchID]("id", O.PrimaryKey, O.AutoInc)
    def status     = column[InventoryCountStatus]("status")
    def started    = column[DateTime]("started")
    def finished   = column[Option[DateTime]]("finished")
    def name       = column[Option[String]]("name")
    def categoryId = column[Option[CategoryID]]("categoryId")
    def brandId    = column[Option[BrandID]]("brandId")

    def categoryFk = foreignKey("inventory_count_category_fk", categoryId, categories)(_.id)
    def brandFk    = foreignKey("inventory_count_brand_fk", brandId, brands)(_.id)

    def * =
      (started, finished, name, categoryId, brandId, status, id).mapTo[InventoryCountBatchRow]
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

  class SalesEventsTable(t: Tag) extends Table[SaleEventRow](t, "sales_events") {
    def id      = column[SaleEventID]("id", O.PrimaryKey, O.AutoInc)
    def event   = column[SaleEventType.Value]("event")
    def message = column[String]("message")
    def saleId  = column[SaleID]("saleId")
    def userId  = column[Option[UserID]]("userId")
    def created = column[DateTime]("created")

    def saleFk = foreignKey("sales_events_sale_fk", saleId, sales)(_.id)
    def userFk = foreignKey("sales_events_user_fk", userId, users)(_.id)

    def * = (event, message, saleId, userId, created, id).mapTo[SaleEventRow]
  }

  lazy val salesEvents = TableQuery[SalesEventsTable]
}

object Schema {
  def apply(profile: JdbcProfile) = new Schema(profile)
}
