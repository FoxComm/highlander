defmodule Marketplace.MerchantAccountController do 
  use Marketplace.Web, :controller
  alias Marketplace.Repo
  alias Marketplace.MerchantAccount
  alias Marketplace.Merchant

  def index(conn, %{"merchant_id" => merchant_id}) do
    merchant_accounts = Repo.all(merchant_accounts(merchant_id))
    render(conn, "index.json", merchant_accounts: merchant_accounts)
  end

  def create(conn, %{"merchant_id" => merchant_id, "account" => merchant_account_params}) do
    changeset = MerchantAccount.changeset(%MerchantAccount{merchant_id: String.to_integer(merchant_id)}, merchant_account_params)

    phoenix_url = Application.get_env(:marketplace, Marketplace.MerchantAccount)[:phoenix_url]
    phoenix_port = Application.get_env(:marketplace, Marketplace.MerchantAccount)[:phoenix_port]
    full_phx_path = "#{phoenix_url}:#{phoenix_port}"

    IO.inspect("full path: #{full_phx_path}")
    case Repo.insert(changeset) do 
      {:ok, merchant_account} -> 
        conn
        |> put_status(:created)
        |> put_resp_header("location", merchant_account_path(conn, :show, merchant_id, merchant_account))
        |> render("merchant_account.json", merchant_account: merchant_account)
      {:error, changeset} -> 
        conn
        |> put_status(:unprocessable_entity)
        |> render(Marketplace.ChangesetView, "errors.json", changeset: changeset)

    end
  end

  defp merchant_accounts(merchant_id) do
    merchant = Repo.get!(Merchant, merchant_id)
    assoc(merchant, :merchant_accounts)
  end
end
