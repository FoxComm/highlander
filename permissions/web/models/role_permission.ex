defmodule Permissions.RolePermission do
  use Permissions.Web, :model

  schema "role_permissions" do 
    belongs_to :role, Permissions.Role
    belongs_to :permission, Permissions.Permission
  end

end
