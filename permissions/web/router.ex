defmodule Permissions.Router do
  use Permissions.Web, :router

  pipeline :api do
    plug :accepts, ["json"]
  end

  scope "/", Permissions do
    pipe_through :api

    resources "/organizations", OrganizationController
    resources "/roles", RoleController
  end
end
