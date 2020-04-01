package com.merit.modules

package object excel {

 import com.merit.modules.inventoryCount.InventoryCountProductDTO
 implicit class C1(
   val p: InventoryCountProductDTO
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
