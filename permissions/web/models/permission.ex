defmodule Permissions.Permission do
  use Permissions.Web, :model

  schema "permissions" do 
    belongs_to :resource, Permissions.Resource
    belongs_to :scope, Permissions.Scope
    field :frn, :string #Fox Resource Name
    field :actions, {:array, :string}

    has_many :role_permissions, Permissions.RolePermission
    has_many :roles, through: [:role_permissions, :role]
  end

  def changeset(model, params \\ :empty) do
    model
    |> cast(params, ~w(resource_id scope_id), ~w())
    |> construct_frn
  end

  def update_changeset(model, params \\ :empty) do
    model
    |> cast(params, ~w(resource_id scope_id), ~w())
  end
  
  defp construct_frn(full_permission) do
    fp = List.first(full_permission)
    "Fox/#{fp.scope_id}/#{fp.resource_name}/#{fp.actions}"
  end
end
