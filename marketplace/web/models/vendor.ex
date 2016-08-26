defmodule Marketplace.Vendor do
  use Marketplace.Web, :model

  schema "vendors" do
    field :name, :string
    field :description, :string
    field :state, :string, default: "new"

    timestamps
  end

  @required_fields ~w(name description)
  @optional_fields ~w(state)
  @states ~w(new approved suspended cancelled)a
  
  use EctoStateMachine,
    states: @states,
    events: [
      [
        name: :approve,
        from: [:new, :suspended],
        to: :approved
      ], [
        name: :suspend,
        from: [:new, :approved],
        to: :suspended
      ], [
        name: :cancel, 
        from: [:suspended, :approved, :new],
        to: :cancelled
      ]
    ]

  def changeset(model, params \\ :empty) do
    model 
    |> cast(params, ~w(name description), ~w(state))
  end

  def update_changeset(model, params \\ :empty) do
    model 
    |> cast(params, ~w(id), ~w(name description state))
    |> make_valid_state_change
  end

  def make_valid_state_change(changeset) do
    case changeset.fetch_change(:state) do
      {:error} -> 
        changeset
      {:ok, newValue} -> 
        unless newValue in @states do 
          changeset
          |> add_error(:state, "Not a valid state.")
        end
        changeset.change(%{:state => newValue})
    end
  end
end
