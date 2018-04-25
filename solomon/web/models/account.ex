defmodule Solomon.Account do
  use Solomon.Web, :model

  schema "accounts" do
    field(:ratchet, :integer)

    has_many(:account_roles, Solomon.AccountRole)
    has_many(:roles, through: [:account_roles, :role])
    has_many(:account_access_methods, Solomon.AccountAccessMethod)
    has_one(:user, Solomon.User)
  end

  def changeset(model, params \\ :empty) do
    # Not sure if we should accept ratchet from the endpoint.
    model
    |> cast(params, ~w(ratchet)a)
  end

  def update_changeset(model, params \\ :empty) do
    model
    |> cast(params, ~w(ratchet)a)
    |> validate_required(~w(ratchet)a)
  end
end
