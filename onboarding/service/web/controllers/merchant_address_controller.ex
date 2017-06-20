defmodule OnboardingService.MerchantAddressController do 
  use OnboardingService.Web, :controller
  alias OnboardingService.Repo
  alias OnboardingService.MerchantAddress
  alias OnboardingService.Merchant

  def index(conn, %{"merchant_id" => merchant_id}) do
    merchant_addresses = Repo.all(merchant_addresses(merchant_id))
    render(conn, "index.json", merchant_addresses: merchant_addresses)
  end

  def create(conn, %{"merchant_id" => merchant_id, "merchant_address" => merchant_address_params}) do
    changeset = MerchantAddress.changeset(%MerchantAddress{merchant_id: String.to_integer(merchant_id)}, merchant_address_params)

    case Repo.insert(changeset) do 
      {:ok, merchant_address} -> 
        conn
        |> put_status(:created)
        |> put_resp_header("location", merchant_merchant_address_path(conn, :show, merchant_id, merchant_address))
        |> render("merchant_address.json", merchant_address: merchant_address)
      {:error, changeset} -> 
        conn
        |> put_status(:unprocessable_entity)
        |> render(OnboardingService.ChangesetView, "errors.json", changeset: changeset)

    end
  end

  defp merchant_addresses(merchant_id) do
    merchant = Repo.get!(Merchant, merchant_id)
    assoc(merchant, :merchant_addresses)
  end
end
