defmodule Marketplace.MerchantSocialProfileController do 
  use Marketplace.Web, :controller
  alias Ecto.Multi
  alias Marketplace.Repo
  alias Marketplace.SocialProfile
  alias Marketplace.MerchantSocialProfile
  alias Marketplace.SocialProfileView

  def create(conn, %{"social_profile" => social_profile_params, "merchant_id" => merchant_id}) do 
    case Repo.transaction(insert_and_relate(social_profile_params, merchant_id)) do 
      {:ok, %{social_profile: social_profile, merchant_social_profile: masp}} -> 
        conn
        |> put_status(:created)
        |> put_resp_header("location", merchant_application_social_profile_path(conn, :show, merchant_id, social_profile))
        |> render(SocialProfileView, "social_profile.json", social_profile: social_profile)
      {:error, failed_operation, failed_value, changes_completed} -> 
        conn
        |> put_status(:unprocessable_entity)
        |> render(Marketplace.ChangesetView, "errors.json", changeset: failed_value)
    end
  end

  def show(conn, %{"merchant_id" => ma_id}) do
    ma_sp = Repo.get_by!(MerchantSocialProfile, merchant_id: ma_id)
    |> Repo.preload(:social_profile)
    render(SocialProfileView, conn, "show.json", social_profile: ma_sp.social_profile)
  end

  def update(conn, %{"id" => id, "social_profile" => social_profile_params}) do 
    social_profile = Repo.get!(SocialProfile, id)
    changeset = SocialProfile.update_changeset(social_profile, social_profile_params)
    case Repo.update(changeset) do
      {:ok, social_profile} -> 
        conn 
        |> render(SocialProfileView, "social_profile.json", social_profile: social_profile)
      {:error, changeset} ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(Marketplace.ChangesetView, "errors.json", changeset: changeset)
    end
  end

  defp insert_and_relate(social_profile_params, merchant_id) do
    sp_cs = SocialProfile.changeset(%SocialProfile{}, social_profile_params)
    Multi.new
    |> Multi.insert(:social_profile, sp_cs)
    |> Multi.run(:merchant_social_profile, fn %{social_profile: social_profile} -> 
      map_social_profile_to_merchant(social_profile, merchant_id) end
    )
  end

  defp map_social_profile_to_merchant(social_profile, merchant_id) do
    masp_cs = MerchantSocialProfile.changeset(%MerchantSocialProfile{}, %{
        "merchant_id" => merchant_id,
        "social_profile_id" => social_profile.id
      })

    Repo.insert(masp_cs)
  end

end
