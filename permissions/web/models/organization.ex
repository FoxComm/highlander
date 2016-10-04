defmodule Permissions.Organization do
  use Permissions.Web, :model
  alias Permissions.Organization
  alias Permissions.OrganizationType

  schema "organizations" do 
    field :name, :string
    field :kind, :string

    belongs_to :parent, Organization
    
    has_many :children, Organization, foreign_key: :parent_id
  end
  
  @required_params ~w(name kind)
  @optional_params ~w(parent_id)

  def changeset(model, params \\ :empty) do
    model 
    |> cast(params, @required_params, @optional_params)
  end
  
  def update_changeset(model, params \\ :empty) do
    model 
    |> cast(params, @required_params, @optional_params)
  end
end
