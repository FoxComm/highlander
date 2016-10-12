defmodule Permissions.Scope do
  use Permissions.Web, :model

  schema "scopes" do 
    field :source, :string
    
    #Implementing without ltree for now.  Custom relation will be needed
    belongs_to :parent, Permissions.Scope
    has_many :children, Permissions.Scope, foreign_key: :parent_id
  end

  @required_fields ~w()a
  @optional_fields ~w(source parent_id)a

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
