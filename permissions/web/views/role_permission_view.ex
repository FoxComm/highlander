defmodule Permissions.RolePermissionView do
  use Permissions.Web, :view
  alias Permissions.RolePermissionView
  alias Permissions.PermissionView

  #Throughout this controller, we present the id of the granted_permission as the RolePermission ID
  def render("index.json", %{role_permissions: role_permissions}) do
    %{granted_permissions: render_many(role_permissions, RolePermissionView, "role_permission.json")}
  end

  def render("show.json", %{role_permission: role_permission, role_id: role_id}) do
    %{granted_permission: render_one(role_permission, PermissionView, "full_permission.json")}
  end

  def render("role_permission.json", %{role_permission: role_permission}) do
    %{grant_id: role_permission.id,
      permission_id: role_permission.permission_id
    }
  end

  def render("deleted.json", _) do
    %{deleted: "success"}
  end
end
