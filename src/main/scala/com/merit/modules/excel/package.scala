package com.merit.modules

package object excel {

 import com.merit.modules.inventoryCount.InventoryCountDTOProduct
 implicit class C1(
   val p: InventoryCountDTOProduct
 ) {
   def toExcelRow: ExcelInventoryCountRow = {
     import p._
     ExcelInventoryCountRow(
       sku,
       barcode,
       name,
       variation,
       expected,
       counted
     )
   }
 }
}
