defmodule Solomon.Router do
  use Solomon.Web, :router

  pipeline :api do
    plug :accepts, ["json"]
    plug Solomon.Plug.JWTScope
  end

  scope "/", Solomon do
    pipe_through :api

    resources "/organizations", OrganizationController
    resources "/roles", RoleController do
      resources "/granted_permissions", RolePermissionController
    end
    resources "/scopes", ScopeController, as: :fc_scope
    post "/scopes/:id/admin_role", ScopeController, :create_admin_role
    resources "/systems", SystemController
    resources "/permissions", PermissionController
    resources "/accounts", AccountController do 
      resources "/granted_roles", AccountRoleController
    end
    post "/sign_in", UserController, :sign_in
    resources "/users", UserController
    get "/ping", Ping, :ping
  end
end
