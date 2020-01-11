package com.merit.api.brands
import akka.http.scaladsl.server.Directives
import com.merit.modules.brands.BrandService
import akka.http.scaladsl.server.Route

object BrandRoutes extends Directives {
  def apply(brandService: BrandService): Route = 
  pathPrefix("brands") {
    GetBrands(brandService)
  }
}