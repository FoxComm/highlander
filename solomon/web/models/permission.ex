defmodule Solomon.Permission do
  use Solomon.Web, :model

  schema "permissions" do
    belongs_to(:resource, Solomon.Resource)
    belongs_to(:scope, Solomon.Scope)
    # Fox Resource Name
    field(:frn, :string)
    field(:actions, {:array, :string})

    has_many(:role_permissions, Solomon.RolePermission)
    has_many(:roles, through: [:role_permissions, :role])
  end

  @required_fields ~w(resource_id scope_id actions)a
  @optional_fields ~w(frn)a

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
