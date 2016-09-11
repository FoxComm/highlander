defmodule Marketplace.Router do
  use Marketplace.Web, :router

  pipeline :browser do
    plug :accepts, ["html"]
    plug :fetch_session
    plug :fetch_flash
    plug :protect_from_forgery
    plug :put_secure_browser_headers
  end

  pipeline :api do
    plug :accepts, ["json"]
  end

  scope "/", Marketplace do
    pipe_through :api # Use the default browser stack

    resources "/merchant_applications", MerchantApplicationController do
      post "/social_profile", MerchantApplicationSocialProfileController, :create, as: :social_profile
      get "/social_profile", MerchantApplicationSocialProfileController, :show, as: :social_profile
      patch "/social_profile", MerchantApplicationSocialProfileController, :update, as: :social_profile
      post "/business_profile", MerchantApplicationBusinessProfileController, :create, as: :business_profile
      get "/business_profile", MerchantApplicationBusinessProfileController, :show, as: :business_profile
      patch "/business_profile", MerchantApplicationBusinessProfileController, :update, as: :business_profile
    end

    post "/merchants/activate_application/:application_id", MerchantController, :activate_application
    
    resources "/merchants", MerchantController do 
      post "/social_profile", MerchantSocialProfileController, :create, as: :social_profile
      get "/social_profile", MerchantSocialProfileController, :show, as: :social_profile
      patch "/social_profile", MerchantSocialProfileController, :update, as: :social_profile
      post "/business_profile", MerchantBusinessProfileController, :create, as: :business_profile
      get "/business_profile", MerchantBusinessProfileController, :show, as: :business_profile
      patch "/business_profile", MerchantBusinessProfileController, :update, as: :business_profile
      resources "/addresses", MerchantAddressController
    end

    get "/", PageController, :index
  end

  # Other scopes may use custom stacks.
  # scope "/api", Marketplace do
  #   pipe_through :api
  # end
end

