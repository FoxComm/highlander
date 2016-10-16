defmodule Marketplace.MerchantAccountController do 
  use Marketplace.Web, :controller
  alias Marketplace.Repo
  alias Marketplace.MerchantAccount
  alias Marketplace.Merchant
  alias Marketplace.PermissionManager
  alias Marketplace.Stripe

  def index(conn, %{"merchant_id" => merchant_id}) do
    merchant_accounts = Repo.all(merchant_accounts(merchant_id))
    render(conn, "index.json", merchant_accounts: merchant_accounts)
  end

  def show_by_solomon_id(conn, %{"solomon_id" => solomon_id}) do
    merchant_account = Repo.get_by!(MerchantAccount, solomon_id: solomon_id)
    render(conn, "show.json", merchant_account: merchant_account)
  end

  def create(conn, %{"merchant_id" => merchant_id, "account" => merchant_account_params}) do
    solomon_id = PermissionManager.create_user_from_merchant_account(merchant_account_params)
    stripe_id = Stripe.create_account()
    account = %MerchantAccount{merchant_id: String.to_integer(merchant_id), stripe_account_id: stripe_id, solomon_id: solomon_id}
    changeset = MerchantAccount.changeset(account, merchant_account_params)

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

  def create_admin(conn, %{"merchant_id" => merchant_id, "account" => merchant_account_params}) do
    solomon_id = PermissionManager.create_user_from_merchant_account(merchant_account_params)
    scope_id = Repo.one(from merchant in Merchant,
                        where: merchant.id == ^merchant_id,
                        select: merchant.scope_id)
    changeset = MerchantAccount.changeset(%MerchantAccount{merchant_id: String.to_integer(merchant_id), solomon_id: solomon_id}, merchant_account_params)
                |> validate_scope_id(scope_id)

    case Repo.insert(changeset) do 
      {:ok, merchant_account} -> 
        role_id = PermissionManager.create_admin_role_from_scope_id(scope_id)
        PermissionManager.grant_account_id_role_id(solomon_id, role_id)
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

  defp validate_scope_id(changeset, scope_id) do
    case scope_id do
      nil -> Ecto.Changeset.add_error(changeset, :scope_id, "validate.required")
      scope_id -> changeset
    end
  end
end
