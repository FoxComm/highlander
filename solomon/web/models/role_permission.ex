defmodule Solomon.RolePermission do
  use Solomon.Web, :model

  schema "role_permissions" do
    belongs_to(:role, Solomon.Role)
    belongs_to(:permission, Solomon.Permission)
  end

  @required_fields ~w(role_id permission_id)a
  @optional_fields ~w()a

  def changeset(model, params \\ :empty) do
    model
    |> cast(params, @required_fields ++ @optional_fields)
    |> validate_required(@required_fields)
  end
end
