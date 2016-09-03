defmodule Permissions.RolePermissionView do
  use Permissions.Web, :view
  alias Permissions.RolePermissionView
  alias Permissions.PermissionView

  #Throughout this controller, we present the id of the granted_permission as the RolePermission ID
  def render("index.json", %{role_permissions: role_permissions}) do
    %{granted_permissions: render_many(role_permissions, RolePermissionView, "role_permission.json")}
  end

  def render("show.json", %{role_permission: role_permission}) do
    %{granted_permission: render_one(role_permission, PermissionView, "full_permission.json")}
  end

  def render("role_permission.json", %{role_permission: role_permission}) do
    %{id: role_permission.id,
      permission_id: role_permission.permission.id
    }
  end
end
