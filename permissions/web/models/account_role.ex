defmodule Permissions.AccountRole do
  use Permissions.Web, :model

  schema "account_roles" do
    belongs_to :account, Permissions.Account
    belongs_to :role, Permissions.Role
  end

  @required_fields ~w(account_id role_id)a
  @optional_fields ~w()a

  def changeset(model, params \\ :empty) do
    model
    |> cast(params, @required_fields ++ @optional_fields)
    |> validate_required(@required_fields)
  end
end
