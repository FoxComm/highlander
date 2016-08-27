defmodule Marketplace.Vendor do
  use Marketplace.Web, :model

  schema "vendors" do
    field :name, :string
    field :description, :string
    field :state, :string, default: "new"

    timestamps
  end

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
    |> cast(params, ~w(name description state), ~w())
  end

  def update_changeset(model, params \\ :empty) do
    model 
    |> cast(params,  ~w(name description state), ~w())
    |> make_valid_state_change
  end

  def make_valid_state_change(changeset) do
    IO.inspect(changeset)
    case Ecto.Changeset.fetch_change(changeset, :state) do
      :error -> 
        changeset
      {:ok, newValue} -> 
        if String.to_atom(newValue) in @states do 
          Ecto.Changeset.change(changeset, %{:state => newValue})
        else 
          Ecto.Changeset.add_error(changeset, :state, "Not a valid state.")
        end
    end
  end
end
