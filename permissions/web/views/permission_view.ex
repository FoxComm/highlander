defmodule Permissions.PermissionView do
  use Permissions.Web, :view
  alias Permissions.PermissionView

  def render("index.json", %{permissions: permissions}) do
    %{permissions: render_many(permissions, PermissionView, "permission.json")}
  end

  def render("show.json", %{permission: permission}) do
    %{permission: render_one(permission, PermissionView, "full_permission.json")}
  end

  def render("permission.json", %{permission: permission}) do
    %{id: permission.id,
      resource_id: permission.resource_id,
      action_id: permission.action_id,
      scope_id: permission.scope_id
    }
  end

  def render("full_permission.json", %{permission: permission}) do
    %{id: permission.id,
      resource_id: permission.resource_id,
      resource_name: permission.resource.name,
      action_id: permission.action_id,
      action_name: permission.action.name,
      scope_id: permission.scope_id,
      scope_source: permission.scope.source
    }
  end

end
