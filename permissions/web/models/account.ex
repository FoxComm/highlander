defmodule Permissions.Account do
  use Permissions.Web, :model

  schema "accounts" do
    field :name, :string
    field :ratchet, :integer
  end
end
