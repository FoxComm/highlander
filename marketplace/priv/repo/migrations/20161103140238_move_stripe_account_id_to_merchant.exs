defmodule Marketplace.Repo.Migrations.MoveStripeAccountIdToMerchant do
  use Ecto.Migration

  def change do
    alter table(:merchants) do
      add :stripe_account_id, :string
    end
  end
end
