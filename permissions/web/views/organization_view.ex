defmodule Permissions.OrganizationView do
  use Permissions.Web, :view
  alias Permissions.OrganizationView

  def render("index.json", %{organizations: organizations}) do
    %{organizations: render_many(organizations, OrganizationView, "organization.json")}
  end

  def render("organization.json", %{organization: organization}) do
    %{id: organization.id,
      name: organization.name,
      parent_id: organization.parent.id
    }
  end
end
