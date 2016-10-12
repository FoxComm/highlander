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

  @required_fields ~w(resource_id scope_id)a
  @optional_fields ~w(frn actions)a

  def changeset(model, params \\ :empty) do
    model
    |> cast(params, @required_fields ++ @optional_fields)
    |> validate_required(@required_fields)
  end

  def update_changeset(model, params \\ :empty) do
    model
    |> cast(params, @required_fields)
    |> validate_required(@required_fields)
  end
  
  end
