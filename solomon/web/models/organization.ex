defmodule Permissions.Organization do
  use Permissions.Web, :model
  alias Permissions.Organization
  alias Permissions.OrganizationType
  alias Permissions.Scope

  schema "organizations" do 
    field :name, :string
    field :kind, :string

    belongs_to :parent, Organization
    belongs_to :scope, Scope
    has_many :children, Organization, foreign_key: :parent_id
  end
  
  @required_fields ~w(name kind)a
  @optional_fields ~w(parent_id scope_id)a

  def changeset(model, params \\ :empty) do
    model 
    |> cast(params, @required_fields ++ @optional_fields)
    |> validate_required(@required_fields)
  end
  
  def update_changeset(model, params \\ :empty) do
    model 
    |> cast(params, @required_fields ++ @optional_fields)
    |> validate_required(@required_fields)
  end
end
