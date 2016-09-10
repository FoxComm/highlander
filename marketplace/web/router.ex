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
      resources "/social_profile", SocialProfileController, only: [:show, :create, :update]
      resources "/business_profile", BusinessProfileController, only: [:show, :create, :update]
    end
    post "/merchants/activate_application/:application_id", MerchantController, :activate_application
    resources "/merchants", MerchantController do 
      resources "/addresses", MerchantAddressController
    end

    get "/", PageController, :index
  end

  # Other scopes may use custom stacks.
  # scope "/api", Marketplace do
  #   pipe_through :api
  # end
end

