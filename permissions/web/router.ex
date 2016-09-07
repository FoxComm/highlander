defmodule Permissions.Router do
  use Permissions.Web, :router

  pipeline :api do
    plug :accepts, ["json"]
  end

  scope "/", Permissions do
    pipe_through :api

    resources "/organizations", OrganizationController
    resources "/roles", RoleController do
      resources "/granted_permissions", RolePermissionController
    end
    resources "/scopes", ScopeController, as: :fc_scope
    resources "/systems", SystemController
    resources "/permissions", PermissionController
    resources "/accounts", AccountController do 
      resources "/granted_roles", AccountRoleController
      resources "/granted_permissions", AccountPermissionController
    end
  end
end
