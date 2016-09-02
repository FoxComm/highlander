defmodule Permissions.AccountRole do
  use Permissions.Web, :model

  schema "account_roles" do
    belongs_to :account, Permissions.Account
    belongs_to :role, Permissions.Role
  end
end
