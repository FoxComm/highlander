defmodule Marketplace.LegalProfileView do 
  use Marketplace.Web, :view

  def render("index.json", %{legal_profiles: legal_profiles}) do
    %{legal_profiles: render_many(legal_profiles, Marketplace.LegalProfileView, "legal_profile.json")}
  end

  def render("legal_profile.json", %{legal_profile: legal_profile}) do
    %{id: legal_profile.id,
      bank_account_number: legal_profile.bank_account_number,
      bank_routing_number: legal_profile.bank_routing_number,
      legal_entity_name: legal_profile.legal_entity_name,
      legal_entity_city: legal_profile.legal_entity_city,
      legal_entity_state: legal_profile.legal_entity_state,
      legal_entity_postal: legal_profile.legal_entity_postal,
      legal_entity_tax_id: legal_profile.legal_entity_tax_id,
      business_founded_day: legal_profile.business_founded_day,
      business_founded_month: legal_profile.business_founded_month,
      business_founded_year: legal_profile.business_founded_year,
      representative_ssn_trailing_four: legal_profile.representative_ssn_trailing_four,
      legal_entity_type: legal_profile.legal_entity_type
    }
  end

  def render("show.json", %{legal_profile: legal_profile}) do 
    %{legal_profile: render_one(legal_profile, Marketplace.LegalProfileView, "legal_profile.json")}
  end
end
