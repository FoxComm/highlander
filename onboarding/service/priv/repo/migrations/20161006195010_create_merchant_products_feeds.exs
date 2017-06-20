defmodule OnboardingService.Repo.Migrations.CreateMerchantProductsFeeds do
  use Ecto.Migration

  def change do
    create table(:merchant_products_feeds) do
      add :merchant_id, references(:merchants)
      add :products_feed_id, references(:products_feeds)
    end
  end
end
