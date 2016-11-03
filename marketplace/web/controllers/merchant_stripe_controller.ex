defmodule Marketplace.MerchantStripeController do
  use Marketplace.Web, :controller
  alias Ecto.Multi
  alias Marketplace.Repo
  alias Marketplace.Merchant
  alias Marketplace.Stripe

  def create(conn, %{"legal_profile" => legal_profile_params, "merchant_id" => merchant_id}) do
    merchant = Repo.get(Merchant, merchant_id)

    case Stripe.create_account(legal_profile_params) do
      {:ok, stripe_account_id} ->
        relate_stripe_account_id(conn, merchant, stripe_account_id)
      {:error, error} ->
        conn
          |> put_status(:unprocessable_entity)
          |> render(Marketplace.ErrorView, "error.json", %{errors: %{stripe: error}})
    end



  end

  defp relate_stripe_account_id(conn, merchant, stripe_account_id) do
    changeset = Merchant.update_changeset(merchant, %{stripe_account_id: stripe_account_id})
        case Repo.update(changeset) do
          {:ok, merchant} ->
            conn
            |> put_status(:created)
            |> render(Marketplace.MerchantView, "merchant.json", merchant: merchant)
          {:error, changeset} ->
            conn
            |> put_status(:unprocessable_entity)
            |> render(Marketplace.ChangesetView, "errors.json", changeset: changeset)
        end
  end
end
