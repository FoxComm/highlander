defmodule Permissions.RolePermission do
  use Permissions.Web, :model

  schema "role_permissions" do 
    belongs_to :role, Permissions.Role
    has_many :permission, Permissions.Permission
  end

end
