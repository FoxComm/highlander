defmodule Solomon.RoleView do
  use Solomon.Web, :view
  alias Solomon.RoleView
  alias Solomon.PermissionView

  def render("index.json", %{roles: roles}) do
    %{roles: render_many(roles, RoleView, "role_with_permissions.json")}
  end

  def render("show_with_permissions.json", %{role: role}) do
    %{role: render_one(role, RoleView, "role_with_permissions.json")}
  end

  def render("show.json", %{role: role}) do
    %{role: render_one(role, RoleView, "role.json")}
  end

  def render("role_with_permissions.json", %{role: role}) do
    %{
      id: role.id,
      name: role.name,
      scope_id: role.scope_id,
      granted_permissions: render_many(role.permissions, PermissionView, "permission.json")
    }
  end

  def render("role.json", %{role: role}) do
    %{id: role.id, name: role.name, scope_id: role.scope_id}
  end
end
