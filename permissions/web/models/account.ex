defmodule Permissions.Account do
  use Permissions.Web, :model

  schema "accounts" do
    field :name, :text
    field :ratchet, :integer
  end
end
