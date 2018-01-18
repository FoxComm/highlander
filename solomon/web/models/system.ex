defmodule Solomon.System do
  use Solomon.Web, :model

  schema "systems" do
    field(:name, :string)
    field(:description, :string)
  end

  @required_fields ~w(name)a
  @optional_fields ~w(description)a

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
