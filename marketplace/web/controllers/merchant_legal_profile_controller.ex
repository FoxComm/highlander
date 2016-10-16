defmodule Marketplace.MerchantLegalProfileController do 
  use Marketplace.Web, :controller
  alias Ecto.Multi
  alias Marketplace.Repo
  alias Marketplace.LegalProfile
  alias Marketplace.MerchantLegalProfile
  alias Marketplace.LegalProfileView
  alias Marketplace.Stripe

  def create(conn, %{"legal_profile" => legal_profile_params, "merchant_id" => merchant_id}) do 
    Stripe.verify_account()
    case Repo.transaction(insert_and_relate(legal_profile_params, merchant_id)) do 
      {:ok, %{legal_profile: legal_profile, merchant_legal_profile: m_lp}} -> 
        conn
        |> put_status(:created)
        |> put_resp_header("location", merchant_legal_profile_path(conn, :show, merchant_id))
        |> render(LegalProfileView, "show.json", legal_profile: legal_profile)
      {:error, failed_operation, failed_value, changes_completed} -> 
        conn
        |> put_status(:unprocessable_entity)
        |> render(Marketplace.ChangesetView, "errors.json", changeset: failed_value)
    end
  end

  def show(conn, %{"merchant_id" => m_id}) do
    m_lp = Repo.get_by!(MerchantLegalProfile, merchant_id: m_id)
    |> Repo.preload(:legal_profile)

    conn
    |> render(LegalProfileView, "show.json", legal_profile: m_lp.legal_profile)
  end

  def update(conn, %{"merchant_id" => m_id, "legal_profile" => legal_profile_params}) do 
    m_lp = Repo.get_by!(MerchantLegalProfile, merchant_id: m_id)
    |> Repo.preload(:legal_profile)
    
    changeset = LegalProfile.update_changeset(m_lp.legal_profile, legal_profile_params)
    case Repo.update(changeset) do
      {:ok, legal_profile} -> 
        conn 
        |> render(LegalProfileView, "show.json", legal_profile: legal_profile)
      {:error, changeset} ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(Marketplace.ChangesetView, "errors.json", changeset: changeset)
    end
  end

  defp insert_and_relate(legal_profile_params, merchant_id) do
    lp_cs = LegalProfile.changeset(%LegalProfile{}, legal_profile_params)
    Multi.new
    |> Multi.insert(:legal_profile, lp_cs)
    |> Multi.run(:merchant_legal_profile, fn %{legal_profile: legal_profile} -> 
      map_legal_profile_to_merchant(legal_profile, merchant_id) end
    )
  end

  defp map_legal_profile_to_merchant(legal_profile, merchant_id) do
    mlp_cs = MerchantLegalProfile.changeset(%MerchantLegalProfile{}, %{
        "merchant_id" => merchant_id,
        "legal_profile_id" => legal_profile.id
      })

    Repo.insert(mlp_cs)
  end

end
