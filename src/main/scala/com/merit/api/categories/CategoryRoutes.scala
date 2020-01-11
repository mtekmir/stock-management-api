package com.merit.api.categories
import akka.http.scaladsl.server.Directives
import com.merit.modules.categories.CategoryService
import akka.http.scaladsl.server.Route

object CategoryRoutes extends Directives {
  def apply(categoryService: CategoryService): Route = {
    pathPrefix("categories") {
      GetCategories(categoryService)
    }
  }
}