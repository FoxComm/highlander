defmodule Marketplace.Repo.Migrations.AddMerchantBusinessProfiles do
  use Ecto.Migration

  def change do
    create table (:merchant_business_profiles) do
      add :merchant_id, references(:merchant_applications)
      add :business_profile_id, references(:business_profiles)
    end

    create unique_index(:merchant_business_profiles, [:merchant_id])        
  end
end
