defmodule Permissions.Account do
  use Permissions.Web, :model

  schema "accounts" do
    field :ratchet, :integer

    has_many :account_roles, Permissions.AccountRole
    has_many :roles, through: [:account_roles, :role]
  end

  def changeset(model, params \\ :empty) do 
    model 
    |> cast(params, ~w(), ~w(ratchet)) #Not sure if we should accept ratchet from the endpoint.
  end

  def update_changeset(model, params \\ :empty) do 
    model 
    |> cast(params, ~w(ratchet), ~w())
  end

end
