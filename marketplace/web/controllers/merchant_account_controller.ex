defmodule Marketplace.MerchantAccountController do 
  use Marketplace.Web, :controller
  alias Marketplace.Repo
  alias Marketplace.MerchantAccount
  alias Marketplace.Merchant
  alias Marketplace.PermissionManager

  def index(conn, %{"merchant_id" => merchant_id}) do
    merchant_accounts = Repo.all(merchant_accounts(merchant_id))
    render(conn, "index.json", merchant_accounts: merchant_accounts)
  end

  def create(conn, %{"merchant_id" => merchant_id, "account" => merchant_account_params}) do
    solomon_id = PermissionManager.create_user_from_merchant_account(merchant_account_params)
    changeset = MerchantAccount.changeset(%MerchantAccount{merchant_id: String.to_integer(merchant_id), solomon_id: solomon_id}, merchant_account_params)

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
