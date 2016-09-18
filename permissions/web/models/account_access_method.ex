defmodule Permissions.AccountAccessMethod do
  use Permissions.Web, :model

  schema "account_access_methods" do
    field :name, :string
    field :hashed_password, :string
    field :algorithm, :integer

    belong_to :account, Permission.Account
  end

  @required_fields ~w(name hashed_password algorithm)
  @optional_fields ~w()

  def changeset(model, params \\ :empty) do 
    model 
    |> cast(params, @required_fields, @optional_fields) 
  end

  def update_changeset(model, params \\ :empty) do 
    model 
    |> cast(params, @required_fields, @optional_fields)
  end

end
