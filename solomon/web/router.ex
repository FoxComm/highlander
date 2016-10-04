defmodule Solomon.Router do
  use Solomon.Web, :router

  pipeline :api do
    plug :accepts, ["json"]
  end

  scope "/", Solomon do
    pipe_through :api

    resources "/organizations", OrganizationController
    post "/organizations/:organization_id/admin_role", OrganizationRoleController, :create_admin_role
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
    resources "/users", UserController
    
  end
end
