defmodule Marketplace.LegalProfile do 
  use Marketplace.Web, :model

  schema "legal_profiles" do
    field :bank_account_number, :string
    field :bank_routing_number, :string
    field :legal_entity_name, :string
    field :legal_entity_city, :string
    field :legal_entity_state, :string
    field :legal_entity_postal, :string
    field :legal_entity_tax_id, :string
    field :business_founded_day, :string
    field :business_founded_month, :string
    field :business_founded_year, :string
    field :representative_ssn_trailing_four, :string
    field :legal_entity_type, :string
    

    timestamps
  end

  @required_fields ~w(bank_account_number bank_routing_number legal_entity_name)a
  @optional_fields ~w(legal_entity_city legal_entity_state legal_entity_postal legal_entity_tax_id 
  business_founded_day business_founded_month business_founded_year representative_ssn_trailing_four 
  legal_entity_type)a

  def changeset(model, params \\ :empty) do
    model
    |> cast(params, @required_fields ++ @optional_fields)
    |> validate_required(@required_fields)
  end

  def update_changeset(model, params \\ :empty) do
    model
    |> cast(params, @required_fields ++ @optional_fields)
    |> validate_required(@required_fields)
  end
end
