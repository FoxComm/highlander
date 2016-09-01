defmodule Permissions.Router do
  use Permissions.Web, :router

  pipeline :api do
    plug :accepts, ["json"]
  end

  scope "/api", Permissions do
    pipe_through :api
  end
end
