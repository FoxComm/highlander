defmodule Permissions.OrganizationView do
  use Permissions.Web, :view
  alias Permissions.OrganizationView

  def render("index.json", %{organizations: organizations}) do
    %{organizations: render_many(organizations, OrganizationView, "organization.json")}
  end

  def render("show.json", %{organization: organization}) do
    %{organization: render_one(organization, OrganizationView, "organization.json")}
  end

  def render("organization.json", %{organization: organization}) do
    %{id: organization.id,
      name: organization.name,
      type: organization.type,
      parent_id: organization.parent_id
    }
  end
end
