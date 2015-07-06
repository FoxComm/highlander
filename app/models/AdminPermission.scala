package models

final case class AdminPermission (id: Int, storeId: Int, resource: String, grantedAccess: String, revokedAccess: String) {
  // The strings might be arrays of access.. such as "[create, read, update]"
}

class AdminPermissions {}

object AdminPermissions {}
