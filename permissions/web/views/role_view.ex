defmodule Permissions.RoleView do
  use Permissions.Web, :view
  alias Permissions.RoleView
  alias Permissions.PermissionView

  def render("index.json", %{roles: roles}) do
    %{roles: render_many(roles, RoleView, "role.json")}
  end

  def render("show.json", %{role: role}) do
    %{role: render_one(role, RoleView, "role.json")}
  end

  def render("role.json", %{role: role}) do
    %{id: role.id,
      name: role.name,
      scope_id: role.scope_id, 
      permissions: render_many(role.permissions, PermissionView, "permission.json")
    }
  end
end
