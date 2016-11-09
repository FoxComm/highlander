defmodule Marketplace.MerchantStripeController do
  use Marketplace.Web, :controller
  alias Ecto.Multi
  alias Marketplace.Repo
  alias Marketplace.MerchantAccount
  alias Marketplace.Stripe

  def create(conn, params), do: secured_route(conn, params, &create/3)
  defp create(conn, %{"legal_profile" => legal_profile_params, "merchant_id" => merchant_id}, claims) do
    ma = Repo.get_by!(MerchantAccount, merchant_id: merchant_id)

    case Stripe.create_account(ma, legal_profile_params) do
      {:ok, stripe_account_id} ->
        relate_stripe_account_id(conn, ma, stripe_account_id)
      {:error, error} ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(Marketplace.ErrorView, "error.json", %{errors: %{stripe: error}})
    end
  end

  defp relate_stripe_account_id(conn, ma, stripe_account_id) do
    changeset = MerchantAccount.update_changeset(ma, %{stripe_account_id: stripe_account_id})

    case Repo.update(changeset) do
      {:ok, merchant_account} ->
        conn
        |> put_status(:created)
        |> render(Marketplace.MerchantAccountView, "merchant_account.json", merchant_account: merchant_account)
      {:error, changeset} ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(Marketplace.ChangesetView, "errors.json", changeset: changeset)
    end
  end
end
