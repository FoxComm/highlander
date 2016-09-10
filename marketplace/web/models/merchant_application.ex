defmodule Marketplace.MerchantApplication do
  use Marketplace.Web, :model

  schema "merchant_applications" do
    field :reference_number, :string
    field :name, :string
    field :business_name, :string
    field :email_address, :string
    field :description, :string
    field :state, :string, default: "new"

    timestamps
   
    has_one :merchant_application_social_profile, Permissions.MerchantApplicationSocialProfile
    has_one :merchant_application_business_profile, Permissions.MerchantApplicationBusinessProfile
    has_one :social_profile, through: [:merchant_application_social_profile, :social_profile]
    has_one :business_profile, through: [:merchant_application_business_profile, :business_profile]
  end

  @states ~w(new approved rejected abandoned)a

  def changeset(model, params \\ :empty) do
    model 
    |> cast(params, ~w(name description state), ~w())
  end

  def update_changeset(model, params \\ :empty) do
    model 
    |> cast(params,  ~w(name description state), ~w())
    |> make_valid_state_change
  end

  def make_valid_state_change(changeset) do
    IO.inspect(changeset)
    case Ecto.Changeset.fetch_change(changeset, :state) do
      :error -> 
        changeset
      {:ok, newValue} -> 
        if String.to_atom(newValue) in @states do 
          Ecto.Changeset.change(changeset, %{:state => newValue})
        else 
          Ecto.Changeset.add_error(changeset, :state, "Not a valid state.")
        end
    end
  end
end
