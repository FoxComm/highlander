defmodule OnboardingService.MerchantStripeController do
  use OnboardingService.Web, :controller
  alias Ecto.Multi
  alias OnboardingService.Repo
  alias OnboardingService.MerchantAccount
  alias OnboardingService.Stripe

  def create(conn, params), do: secured_route(conn, params, &create/3)
  defp create(conn, %{"legal_profile" => legal_profile_params, "merchant_id" => merchant_id}, claims) do
    ma = Repo.get_by!(MerchantAccount, merchant_id: merchant_id)

    case Stripe.create_account(ma, legal_profile_params) do
      {:ok, stripe_account_id} ->
        relate_stripe_account_id(conn, ma, stripe_account_id)
      {:error, error} ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(OnboardingService.ErrorView, "error.json", %{errors: %{stripe: error}})
    end
  end

  defp relate_stripe_account_id(conn, ma, stripe_account_id) do
    changeset = MerchantAccount.update_changeset(ma, %{stripe_account_id: stripe_account_id})

    case Repo.update(changeset) do
      {:ok, merchant_account} ->
        conn
        |> put_status(:created)
        |> render(OnboardingService.MerchantAccountView, "merchant_account.json", merchant_account: merchant_account)
      {:error, changeset} ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(OnboardingService.ChangesetView, "errors.json", changeset: changeset)
    end
  end
end
