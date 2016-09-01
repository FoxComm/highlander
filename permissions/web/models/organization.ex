defmodule Permissions.Organization do
  use Permissions.Web, :model
  alias Permissions.Organization
  alias Permissions.OrganizationType

  schema "organizations" do 
    field :name, :string
    field :type, :string

    belongs_to :parent, Organization
    
    has_many :children, Organization, foreign_key: :parent_id
  end

  def changeset(model, params \\ :empty) do
    model 
    |> cast(params, ~w(name type), ~w(parent_id))
  end
end
