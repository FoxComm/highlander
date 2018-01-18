defmodule Solomon.AccountAccessMethod do
  use Solomon.Web, :model

  schema "account_access_methods" do
    field(:name, :string)
    field(:hashed_password, :string)
    field(:algorithm, :integer)

    belongs_to(:account, Permission.Account)
  end

  @required_fields ~w(name hashed_password algorithm)a
  @optional_fields ~w(account_id)a

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
