defmodule Marketplace.MerchantAccountController do
  use Marketplace.Web, :controller
  alias Ecto.Multi
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
    solomon_id = PermissionManager.create_user_from_merchant_account(conn, merchant_account_params)
    account = %MerchantAccount{merchant_id: String.to_integer(merchant_id), solomon_id: solomon_id}
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
    org = Repo.get!(Merchant, merchant_id)
          |> Map.fetch!(:business_name)
    email = Map.fetch!(merchant_account_params, "email_address")
    passwd = Map.fetch!(merchant_account_params, "password")
    scope_id = Repo.one(from merchant in Merchant,
                        where: merchant.id == ^merchant_id,
                        select: merchant.scope_id)
    ma = %MerchantAccount{merchant_id: String.to_integer(merchant_id)}
    changeset = MerchantAccount.changeset(ma, merchant_account_params)
                |> validate_scope_id(scope_id)

    txn = Multi.new
          |> Multi.insert(:merchant_account, changeset)
          |> Multi.run(:stripe_account_id, fn %{merchant_account: merchant_account} ->
            Stripe.create_account(merchant_account, merchant_account_params) end)
          |> Multi.run(:ma_with_stripe, fn %{merchant_account: ma, stripe_account_id: stripe} ->
            relate_stripe_account_id(ma, stripe) end)
          |> Multi.run(:full_ma, fn %{ma_with_stripe: ma} ->
            solomon_id = PermissionManager.create_user_from_merchant_account(conn, merchant_account_params)
            relate_solomon_id(ma, solomon_id) end)

    case Repo.transaction(txn) do
      {:ok, %{full_ma: merchant_account}} ->
        role_id = PermissionManager.create_admin_role_from_scope_id(conn, scope_id)
        PermissionManager.grant_account_id_role_id(conn, merchant_account.solomon_id, role_id)
        conn
        |> put_status(:created)
        |> put_resp_header("location", merchant_account_path(conn, :show, merchant_id, merchant_account))
        |> PermissionManager.sign_in_user(org, email, passwd)
        |> render("merchant_account.json", merchant_account: merchant_account)
      {:error, _, changeset, _} ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(Marketplace.ChangesetView, "errors.json", changeset: changeset)
    end
  end

  defp validate_scope_id(changeset, scope_id) do
    case scope_id do
      nil -> Ecto.Changeset.add_error(changeset, :scope_id, "validate.required")
      _ -> changeset
    end
  end

  defp relate_stripe_account_id(ma, stripe_account_id) do
    changeset = MerchantAccount.update_changeset(ma, %{stripe_account_id: stripe_account_id})
    Repo.update(changeset)
  end

  defp relate_solomon_id(ma, solomon_id) do
    changeset = MerchantAccount.update_changeset(ma, %{solomon_id: solomon_id})
    Repo.update(changeset)
  end
end
