defmodule Solomon.PermissionView do
  use Solomon.Web, :view
  alias Solomon.PermissionView

  def render("index.json", %{permissions: permissions}) do
    %{permissions: render_many(permissions, PermissionView, "permission.json")}
  end

  def render("show_full.json", %{permission: permission}) do
    %{permission: render_one(permission, PermissionView, "full_permission.json")}
  end

  def render("show.json", %{permission: permission}) do
    %{permission: render_one(permission, PermissionView, "permission.json")}
  end

  def render("permission.json", %{permission: permission}) do
    %{
      id: permission.id,
      resource_id: permission.resource_id,
      actions: permission.actions,
      frn: permission.frn,
      scope_id: permission.scope_id
    }
  end

  def render("full_permission.json", %{permission: permission}) do
    %{
      id: permission.id,
      resource_id: permission.resource_id,
      resource_name: permission.resource.name,
      actions: permission.actions,
      frn: permission.frn,
      scope_id: permission.scope_id,
      scope_source: permission.scope.source
    }
  end

  def render("deleted.json", _) do
    %{deleted: "success"}
  end
end
