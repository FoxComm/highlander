package com.foxcommerce.endpoints.admin

import com.foxcommerce.common.Config
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.action.AddCookieBuilder

object StoreAdminActivity {

  def asStoreAdmin(): AddCookieBuilder = addCookie(Cookie(Config.jwtCookie, "${jwtTokenAdmin}"))
}
