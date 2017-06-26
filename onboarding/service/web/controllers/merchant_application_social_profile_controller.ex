defmodule OnboardingService.MerchantApplicationSocialProfileController do 
  use OnboardingService.Web, :controller
  alias Ecto.Multi
  alias OnboardingService.Repo
  alias OnboardingService.SocialProfile
  alias OnboardingService.MerchantApplicationSocialProfile
  alias OnboardingService.SocialProfileView

  def create(conn, %{"social_profile" => social_profile_params, "merchant_application_id" => merchant_application_id}) do 
    case Repo.transaction(insert_and_relate(social_profile_params, merchant_application_id)) do 
      {:ok, %{social_profile: social_profile, merchant_application_social_profile: ma_sp}} -> 
        conn
        |> put_status(:created)
        |> put_resp_header("location", merchant_application_social_profile_path(conn, :show, merchant_application_id))
        |> render(SocialProfileView, "social_profile.json", social_profile: social_profile)
      {:error, failed_operation, failed_value, changes_completed} -> 
        conn
        |> put_status(:unprocessable_entity)
        |> render(OnboardingService.ChangesetView, "errors.json", changeset: failed_value)
    end
  end

  def show(conn, %{"merchant_application_id" => ma_id}) do
    ma_sp = Repo.get_by!(MerchantApplicationSocialProfile, merchant_application_id: ma_id)
    |> Repo.preload(:social_profile)
    render(conn, SocialProfileView, "show.json", social_profile: ma_sp.social_profile)
  end

  def update(conn, %{"merchant_application_id" => ma_id, "social_profile" => social_profile_params}) do 
    ma_sp = Repo.get_by!(MerchantApplicationSocialProfile, merchant_application_id: ma_id)
    |> Repo.preload(:social_profile)

    changeset = SocialProfile.update_changeset(ma_sp.social_profile, social_profile_params)
    case Repo.update(changeset) do
      {:ok, social_profile} -> 
        conn 
        |> render(SocialProfileView, "social_profile.json", social_profile: social_profile)
      {:error, changeset} ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(OnboardingService.ChangesetView, "errors.json", changeset: changeset)
    end
  end

  defp insert_and_relate(social_profile_params, merchant_application_id) do
    sp_cs = SocialProfile.changeset(%SocialProfile{}, social_profile_params)
    Multi.new
    |> Multi.insert(:social_profile, sp_cs)
    |> Multi.run(:merchant_application_social_profile, fn %{social_profile: social_profile} -> 
      map_social_profile_to_merchant_application(social_profile, merchant_application_id) end
    )
  end

  defp map_social_profile_to_merchant_application(social_profile, merchant_application_id) do
    masp_cs = MerchantApplicationSocialProfile.changeset(%MerchantApplicationSocialProfile{}, %{
        "merchant_application_id" => merchant_application_id,
        "social_profile_id" => social_profile.id
      })

    Repo.insert(masp_cs)
  end

end
