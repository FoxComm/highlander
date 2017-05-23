defmodule OnboardingService.Repo.Migrations.AddLegalProfiles do
  use Ecto.Migration

  def change do
    create table (:legal_profiles) do
      add :bank_account_number, :string
      add :bank_routing_number, :string
      add :legal_entity_name, :string
      add :legal_entity_city, :string
      add :legal_entity_state, :string
      add :legal_entity_postal, :string
      add :legal_entity_tax_id, :string
      add :business_founded_day, :string
      add :business_founded_month, :string
      add :business_founded_year, :string
      add :representative_ssn_trailing_four, :string
      add :legal_entity_type, :string
    

      timestamps
    end 
  end
end
