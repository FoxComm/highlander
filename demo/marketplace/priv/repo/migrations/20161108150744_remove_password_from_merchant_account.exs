defmodule Marketplace.Repo.Migrations.RemovePasswordFromMerchantAccount do
  use Ecto.Migration

  def change do
    alter table(:merchant_accounts) do
      remove :password
    end
  end
end
