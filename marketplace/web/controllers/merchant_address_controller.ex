defmodule Marketplace.MerchantAddressController do 
  use Marketplace.Web, :controller
  alias Marketplace.Repo
  alias Marketplace.MerchantAddress
  alias Marketplace.Merchant

  def index(conn, %{"merchant_id" => merchant_id}) do
    merchant_addresses = Repo.all(merchant_addresses(merchant_id))
    render(conn, "index.json", merchant_addresses: merchant_addresses)
  end

  defp merchant_addresses(merchant_id) do
    merchant = Repo.get!(Merchant, merchant_id)
    assoc(merchant, :merchant_addresses)
  end
end
