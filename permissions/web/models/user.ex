defmodule Permissions.User do
  use Permissions.Web, :model

  schema "users" do
    field :ratchet, :integer

    has_many :user_roles, Permissions.UserRole
    has_many :roles, through: [:user_roles, :role]
  end

  def changeset(model, params \\ :empty) do 
    model 
    |> cast(params, ~w(name), ~w(ratchet)) #Not sure if we should accept ratchet from the endpoint.
  end

  def update_changeset(model, params \\ :empty) do 
    model 
    |> cast(params, ~w(name ratchet), ~w())
  end

end
