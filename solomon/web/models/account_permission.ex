defmodule Solomon.AccountPermission do
  use Solomon.Web, :model

  schema "account_permissions" do 
    belongs_to :account, Solomon.Account
    belongs_to :permission, Solomon.Permission
  end

  @required_fields ~w(account_id permission_id)a
  @optional_fields ~w()a

  def changeset(model, params \\ :empty) do
    model 
    |> cast(params, @required_fields ++ @optional_fields)
    |> validate_required(@required_fields)
  end
end
