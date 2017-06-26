defmodule OnboardingService.MerchantApplicationBusinessProfileController do 
  use OnboardingService.Web, :controller
  alias Ecto.Multi
  alias OnboardingService.Repo
  alias OnboardingService.BusinessProfile
  alias OnboardingService.MerchantApplicationBusinessProfile
  alias OnboardingService.BusinessProfileView

  def create(conn, %{"business_profile" => business_profile_params, "merchant_application_id" => merchant_application_id}) do 
    case Repo.transaction(insert_and_relate(business_profile_params, merchant_application_id)) do 
      {:ok, %{business_profile: business_profile, merchant_application_business_profile: ma_bp}} -> 
        conn
        |> put_status(:created)
        |> put_resp_header("location", merchant_application_business_profile_path(conn, :show, merchant_application_id))
        |> render(BusinessProfileView, "business_profile.json", business_profile: business_profile)
      {:error, failed_operation, failed_value, changes_completed} -> 
        conn
        |> put_status(:unprocessable_entity)
        |> render(OnboardingService.ChangesetView, "errors.json", changeset: failed_value)
    end
  end

  def show(conn, %{"merchant_application_id" => ma_id}) do
    ma_bp = Repo.get_by!(MerchantApplicationBusinessProfile, merchant_application_id: ma_id)
    |> Repo.preload(:business_profile)
    render(conn, BusinessProfileView, "show.json", business_profile: ma_bp.business_profile)
  end

  def update(conn, %{"merchant_application_id" => ma_id, "business_profile" => business_profile_params}) do 
    ma_bp = Repo.get_by!(MerchantApplicationBusinessProfile, merchant_application_id: ma_id)
    |> Repo.preload(:business_profile)

    changeset = BusinessProfile.update_changeset(ma_bp.business_profile, business_profile_params)
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

  defp insert_and_relate(business_profile_params, merchant_application_id) do
    sp_cs = BusinessProfile.changeset(%BusinessProfile{}, business_profile_params)
    Multi.new
    |> Multi.insert(:business_profile, sp_cs)
    |> Multi.run(:merchant_application_business_profile, fn %{business_profile: business_profile} -> 
      map_business_profile_to_merchant_application(business_profile, merchant_application_id) end
    )
  end

  defp map_business_profile_to_merchant_application(business_profile, merchant_application_id) do
    masp_cs = MerchantApplicationBusinessProfile.changeset(%MerchantApplicationBusinessProfile{}, %{
        "merchant_application_id" => merchant_application_id,
        "business_profile_id" => business_profile.id
      })

    Repo.insert(masp_cs)
  end

end
