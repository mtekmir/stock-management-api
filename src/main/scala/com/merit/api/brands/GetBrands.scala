package com.merit.api.brands
import akka.http.scaladsl.server.Directives
import api.JsonSupport
import com.merit.modules.brands.BrandService
import akka.http.scaladsl.server.Route

object GetBrands extends Directives with JsonSupport {
  def apply(brandService: BrandService): Route = 
    get {
      complete(brandService.getBrands)
    }
}