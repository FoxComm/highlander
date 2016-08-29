defmodule Marketplace.MerchantAddressController do 
  use Marketplace.Web, :controller
  alias Marketplace.Repo
  alias Marketplace.MerchantAddress

  def index(conn, _params, merchant) do
    merchant_addresses = Repo.all(merchant_addresses(merchant))
    render(conn, "index.json", merchant_addresses: merchant_addresses)
  end

  defp merchant_addresses(merchant) do
    assoc(merchant, :merchant_addresses)
  end
end
