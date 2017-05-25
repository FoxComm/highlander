defmodule OnboardingService.MerchantBusinessProfileController do 
  use OnboardingService.Web, :controller
  alias Ecto.Multi
  alias OnboardingService.Repo
  alias OnboardingService.BusinessProfile
  alias OnboardingService.MerchantBusinessProfile
  alias OnboardingService.BusinessProfileView

  def create(conn, %{"business_profile" => business_profile_params, "merchant_id" => merchant_id}) do 
    case Repo.transaction(insert_and_relate(business_profile_params, merchant_id)) do 
      {:ok, %{business_profile: business_profile, merchant_business_profile: m_bp}} -> 
        conn
        |> put_status(:created)
        |> put_resp_header("location", merchant_business_profile_path(conn, :show, merchant_id))
        |> render(BusinessProfileView, "business_profile.json", business_profile: business_profile)
      {:error, failed_operation, failed_value, changes_completed} -> 
        conn
        |> put_status(:unprocessable_entity)
        |> render(OnboardingService.ChangesetView, "errors.json", changeset: failed_value)
    end
  end

  def show(conn, %{"merchant_id" => m_id}) do
    m_bp = Repo.get_by!(MerchantBusinessProfile, merchant_id: m_id)
    |> Repo.preload(:business_profile)
    
    conn
    |> render(BusinessProfileView, "show.json", business_profile: m_bp.business_profile)
  end

  def update(conn, %{"merchant_id" => m_id, "business_profile" => business_profile_params}) do 
    m_bp = Repo.get_by!(MerchantBusinessProfile, merchant_id: m_id)
    |> Repo.preload(:business_profile)
    
    changeset = BusinessProfile.update_changeset(m_bp.business_profile, business_profile_params)
    case Repo.update(changeset) do
      {:ok, business_profile} -> 
        conn 
        |> render(BusinessProfileView, "business_profile.json", business_profile: business_profile)
      {:error, changeset} ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(OnboardingService.ChangesetView, "errors.json", changeset: changeset)
    end
  end

  defp insert_and_relate(business_profile_params, merchant_id) do
    sp_cs = BusinessProfile.changeset(%BusinessProfile{}, business_profile_params)
    Multi.new
    |> Multi.insert(:business_profile, sp_cs)
    |> Multi.run(:merchant_business_profile, fn %{business_profile: business_profile} -> 
      map_business_profile_to_merchant(business_profile, merchant_id) end
    )
  end

  defp map_business_profile_to_merchant(business_profile, merchant_id) do
    masp_cs = MerchantBusinessProfile.changeset(%MerchantBusinessProfile{}, %{
        "merchant_id" => merchant_id,
        "business_profile_id" => business_profile.id
      })

    Repo.insert(masp_cs)
  end

end
