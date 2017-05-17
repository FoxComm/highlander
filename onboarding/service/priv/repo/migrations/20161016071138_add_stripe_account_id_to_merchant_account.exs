defmodule OnboardingService.Repo.Migrations.AddStripeAccountIdToMerchantAccount do
  use Ecto.Migration

  def change do
    alter table(:merchant_accounts) do
      add :stripe_account_id, :string
    end
  end
end
