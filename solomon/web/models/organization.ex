defmodule Solomon.Organization do
  use Solomon.Web, :model
  alias Solomon.Organization
  alias Solomon.OrganizationType
  alias Solomon.Scope

  schema "organizations" do
    field(:name, :string)
    field(:kind, :string)

    belongs_to(:parent, Organization)
    belongs_to(:scope, Scope)
    has_many(:children, Organization, foreign_key: :parent_id)
  end

  @required_fields ~w(name kind parent_id scope_id)a
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
