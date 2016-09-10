defmodule Marketplace.Repo.Migrations.AddMerchantBusinessProfiles do
  use Ecto.Migration

  def change do
    create table (:merchant_business_profiles) do
      add :merchant_id, references(:merchant_application)
      add :business_profile_id, references(:business_profile)
    end
  end
end
