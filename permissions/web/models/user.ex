defmodule Permissions.User do
  use Permissions.Web, :model

  schema "users" do
    field :email, :string
    field :is_disabled, :boolean
    field :disabled_by, :integer
    field :is_blacklisted, :boolean
    field :blacklisted_by, :integer
    field :name, :string
    field :phone_number, :string

    belong_to :account, Permission.Account
  end

  @required_fields ~w(email name)
  @optional_fields ~w(is_disabled disabled_by is_blacklisted blacklisted_by phone_number)

  def changeset(model, params \\ :empty) do 
    model 
    |> cast(params, @required_fields, @optional_fields) #Not sure if we should accept ratchet from the endpoint.
  end

  def update_changeset(model, params \\ :empty) do 
    model 
    |> cast(params, @required_fields, @optional_fields)
  end

end
