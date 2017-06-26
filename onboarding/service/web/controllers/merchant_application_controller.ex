defmodule OnboardingService.MerchantApplicationController do 
  use OnboardingService.Web, :controller
  alias Ecto.Multi
  alias OnboardingService.Repo
  alias OnboardingService.MerchantApplication
  alias OnboardingService.MerchantApplicationBusinessProfile
  alias OnboardingService.MerchantApplicationSocialProfile
  alias OnboardingService.BusinessProfile
  alias OnboardingService.SocialProfile

  def index(conn, _params) do
    merchant_applications = Repo.all(MerchantApplication)
    render(conn, "index.json", merchant_applications: merchant_applications)
  end

  def create(conn, %{"merchant_application" => merchant_application_params}) do
    changeset = MerchantApplication.changeset(%MerchantApplication{}, merchant_application_params)

    case Repo.insert(changeset) do
      {:ok, merchant_application} ->
        conn
        |> put_status(:created)
        |> put_resp_header("location", merchant_application_path(conn, :show, merchant_application))
        |> render("merchant_application.json", merchant_application: merchant_application)
      {:error, changeset} ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(OnboardingService.ChangesetView, "errors.json", changeset: changeset)
    end
  end

  def show(conn, %{"id" => id}) do
    merchant_application = Repo.get!(MerchantApplication, id)
    render(conn, "show.json", merchant_application: merchant_application)
  end
  
  def show_by_ref(conn, %{"ref_num" => ref_num}) do
    merchant_application = Repo.get_by!(MerchantApplication, reference_number: ref_num)
    |> Repo.preload(:merchant)

    if merchant_application.state == "new" do
      render(conn, "show.json", merchant_application: merchant_application)
    else 
      render(conn, "ma_with_merchant.json", merchant_application: merchant_application)     
    end
  end

  def update(conn, %{"id" => id, "merchant_application" => merchant_application_params}) do 
    merchant_application = Repo.get!(MerchantApplication, id)
    changeset = MerchantApplication.update_changeset(merchant_application, merchant_application_params)
    case Repo.update(changeset) do
      {:ok, merchant_application} -> 
        conn 
        |> render("merchant_application.json", merchant_application: merchant_application)
      {:error, changeset} ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(OnboardingService.ChangesetView, "errors.json", changeset: changeset)
    end
  end

  defp business_profile_changeset(merchant_application_params) do
    case Map.fetch(merchant_application_params, "business_profile") do
      {:ok, bp_params} ->
        BusinessProfile.changeset(%BusinessProfile{}, bp_params)
      :error ->
        BusinessProfile.changeset(%BusinessProfile{}, %{})
    end
  end

  defp social_profile_changeset(merchant_application_params) do
    case Map.fetch(merchant_application_params, "social_profile") do
      {:ok, sp_params} ->
        SocialProfile.changeset(%SocialProfile{}, sp_params)
      :error ->
        SocialProfile.changeset(%SocialProfile{}, %{})
    end
  end

  defp map_business_profile_to_merchant_application(business_profile_id, merchant_application_id) do
    masp_cs = MerchantApplicationBusinessProfile.changeset(%MerchantApplicationBusinessProfile{}, %{
        "merchant_application_id" => merchant_application_id,
        "business_profile_id" => business_profile_id
      })

    Repo.insert(masp_cs)
  end

  defp map_social_profile_to_merchant_application(social_profile_id, merchant_application_id) do
    masp_cs = MerchantApplicationSocialProfile.changeset(%MerchantApplicationSocialProfile{}, %{
        "merchant_application_id" => merchant_application_id,
        "social_profile_id" => social_profile_id
      })

    Repo.insert(masp_cs)
  end

  def merchant_application_full(conn, %{"merchant_application" => merchant_application_params}) do
    bp_cs = business_profile_changeset(merchant_application_params)
    sp_cs = social_profile_changeset(merchant_application_params)
    changeset = MerchantApplication.changeset(%MerchantApplication{}, merchant_application_params)
    errs =  [changeset, bp_cs, sp_cs]
            |> Enum.find(fn x -> !x.valid? end)
    case errs do
      nil ->
        multi_txn = Multi.new
                    |> Multi.insert(:merchant_application, changeset)
                    |> Multi.insert(:business_profile, bp_cs)
                    |> Multi.insert(:social_profile, sp_cs)
        case Repo.transaction(multi_txn) do
          {:ok, %{business_profile: bp, merchant_application: ma, social_profile: sp}} ->
            map_business_profile_to_merchant_application(bp.id, ma.id)
            map_social_profile_to_merchant_application(sp.id, ma.id)
            conn
            |> put_status(:created)
            |> put_resp_header("location", merchant_application_path(conn, :show, ma))
            |> render("merchant_application.json", merchant_application: ma)
          {:error, changeset} ->
            conn
            |> put_status(:unprocessable_entity)
            |> render(OnboardingService.ChangesetView, "errors.json", changeset: changeset)
        end
      errors ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(OnboardingService.ChangesetView, "errors.json", changeset: errors)
    end
  end
end
