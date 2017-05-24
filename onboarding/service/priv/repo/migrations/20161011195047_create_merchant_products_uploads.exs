defmodule OnboardingService.Repo.Migrations.CreateMerchantProductsUploads do
  use Ecto.Migration

  def change do
    create table(:merchant_products_uploads) do
      add :merchant_id, references(:merchants)
      add :products_upload_id, references(:products_uploads)
    end
  end
end
