defmodule Permissions.Permission do
  use Permissions.Web, :model

  schema "permissions" do 
    belongs_to :resource, Permissions.Resource
    belongs_to :action, Permissions.Action
    belongs_to :scope, Permissions.Scope

    has_many :role_permissions, Permissions.RolePermission
    has_many :roles, through: [:role_permissions, :role]
  end

  def changeset(model, params \\ :empty) do
    model
    |> cast(params, ~w(resource_id action_id scope_id), ~w())
  end

  def update_changeset(model, params \\ :empty) do
    model
    |> cast(params, ~w(resource_id action_id scope_id), ~w())
  end
end
