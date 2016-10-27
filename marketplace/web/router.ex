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

    get "/merchant_applications/by_ref/:ref_num", MerchantApplicationController, :show_by_ref
    post "/merchant_applications_full", MerchantApplicationController, :merchant_application_full
    post "/merchants/activate_application/:application_id", MerchantController, :activate_application
    post "/merchants/:merchant_id/admin_accounts", MerchantAccountController, :create_admin
    
    resources "/merchants", MerchantController do 
      post "/social_profile", MerchantSocialProfileController, :create, as: :social_profile
      get "/social_profile", MerchantSocialProfileController, :show, as: :social_profile
      patch "/social_profile", MerchantSocialProfileController, :update, as: :social_profile
      post "/business_profile", MerchantBusinessProfileController, :create, as: :business_profile
      get "/business_profile", MerchantBusinessProfileController, :show, as: :business_profile
      patch "/business_profile", MerchantBusinessProfileController, :update, as: :business_profile
      post "/legal_profile", MerchantLegalProfileController, :create, as: :legal_profile
      get "/legal_profile", MerchantLegalProfileController, :show, as: :legal_profile
      patch "/legal_profile", MerchantLegalProfileController, :update, as: :legal_profile
      resources "/addresses", MerchantAddressController
      resources "/accounts", MerchantAccountController, as: :account
      get "/accounts/by_solomon_id/:solomon_id", MerchantAccountController, :show_by_solomon_id, as: :account
      post "/products_feed", MerchantProductsFeedController, :create, as: :products_feed
      get "/products_feed", MerchantProductsFeedController, :index, as: :products_feed
      get "/products_feed/:id", MerchantProductsFeedController, :show, as: :products_feed
      patch "/products_feed/:id", MerchantProductsFeedController, :update, as: :products_feed
      post "/products_upload", MerchantProductsUploadController, :create, as: :products_upload
      get "/products_upload", MerchantProductsUploadController, :index, as: :products_upload
      get "/products_upload/:id", MerchantProductsUploadController, :show, as: :products_upload
      patch "/products_upload/:id", MerchantProductsUploadController, :update, as: :products_upload
    end

    get "/", PageController, :index
  end

  # Other scopes may use custom stacks.
  # scope "/api", Marketplace do
  #   pipe_through :api
  # end
end

