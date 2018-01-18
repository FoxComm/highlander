defmodule Solomon.Role do
  use Solomon.Web, :model

  schema "roles" do
    field(:name, :string)

    belongs_to(:scope, Solomon.Scope)

    has_many(:role_permissions, Solomon.RolePermission)
    has_many(:permissions, through: [:role_permissions, :permission])
  end

  @required_fields ~w(name scope_id)a
  @optional_fields ~w()a

  def changeset(model, params \\ :empty) do
    model
    |> cast(params, @required_fields ++ @optional_fields)
    |> validate_required(@required_fields)
  end

  def update_changeset(model, params \\ :empty) do
    model
    |> cast(params, @required_fields ++ @optional_fields)
    |> validate_required(@required_fields)
  end
end
