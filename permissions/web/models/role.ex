defmodule Permissions.Role do
  use Permissions.Web, :model

  schema "roles" do
    field :name, :string

    belongs_to :scope, Permissions.Scope

    has_many :role_permissions, Permissions.RolePermission
    has_many :permissions, through: [:role_permissions, :permission]
  end

  def changeset(model, params \\ :empty) do
    model 
    |> cast(params, ~w(name scope_id), ~w())
  end

  def update_changeset(model, params \\ :empty) do
    model 
    |> cast(params, ~w(name scope_id), ~w())
  end

end
