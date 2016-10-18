defmodule Permissions.AccountPermission do
  use Permissions.Web, :model

  schema "account_permissions" do 
    belongs_to :account, Permissions.Account
    belongs_to :permission, Permissions.Permission
  end

  def changeset(model, params \\ :empty) do
    model 
    |> cast(params, ~w(account_id permission_id), ~w())
  end
end
