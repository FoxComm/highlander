defmodule Permissions.Scope do
  use Permissions.Web, :model

  schema "scopes" do 
    field :source, :string
    
    #Implementing without ltree for now.  Custom relation will be needed
    belongs_to :parent, Permissions.Scope
    has_many :children, Permissions.Scope, foreign_key: :parent_id
  end

end
