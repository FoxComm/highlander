defmodule Solomon.User do
  use Solomon.Web, :model

  schema "users" do
    field :email, :string
    field :is_disabled, :boolean
    field :disabled_by, :integer
    field :is_blacklisted, :boolean
    field :blacklisted_by, :integer
    field :name, :string
    field :phone_number, :string

    belongs_to :account, Solomon.Account
  end

  @required_fields ~w(email name)a
  @optional_fields ~w(is_disabled disabled_by is_blacklisted blacklisted_by phone_number account_id)a

  def changeset(model, params \\ :empty) do 
    model 
    |> cast(params, @required_fields ++ @optional_fields) #Not sure if we should accept ratchet from the endpoint.
    |> validate_required(@required_fields)
  end

  def update_changeset(model, params \\ :empty) do 
    model 
    |> cast(params, @required_fields ++ @optional_fields)
    |> validate_required(@required_fields)
  end

end
