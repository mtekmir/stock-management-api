package com.merit.api.categories
import akka.http.scaladsl.server.Directives
import api.JsonSupport
import com.merit.modules.categories.CategoryService
import akka.http.scaladsl.server.Route

object GetCategories extends Directives with JsonSupport {
  def apply(categoryService: CategoryService): Route = 
    get {
      complete(categoryService.getAll)
    }
}