defmodule Marketplace.MerchantStripeController do 
  use Marketplace.Web, :controller
  alias Ecto.Multi
  alias Marketplace.Repo
  alias Marketplace.MerchantAccount
  alias Marketplace.Stripe

  def create(conn, %{"legal_profile" => legal_profile_params, "merchant_id" => merchant_id}) do 
    ma = Repo.get_by!(MerchantAccount, merchant_id: merchant_id)
    stripe_account_id = Stripe.create_account(ma, legal_profile_params)

    changeset = MerchantAccount.update_changeset(ma, %{stripe_account_id => stripe_account_id})
    case Repo.update(changeset) do
      {:ok, merchant_account} ->
        send_resp(conn, :no_content, "")
      {:error, changeset} ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(Marketplace.ChangesetView, "errors.json", changeset: changeset)
    end
  end
end
