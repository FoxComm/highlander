defmodule Marketplace.Repo.Migrations.AddMerchantApplicationBusinessProfiles do
  use Ecto.Migration

  def change do
    create table (:merchant_application_business_profiles) do
      add :merchant_application_id, references(:merchant_application)
      add :business_profile_id, references(:business_profile)
    end
  end
end
