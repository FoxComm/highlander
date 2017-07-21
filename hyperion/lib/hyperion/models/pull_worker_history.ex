defmodule PullWorkerHistory do

  use Ecto.Schema
  use Timex.Ecto.Timestamps
  import Ecto.Changeset
  import Ecto.Query

  @derive {Poison.Encoder, only: [:id, :last_run, :seller_id]}

  schema "pull_worker_history" do
    field :last_run, Timex.Ecto.DateTime
    field :seller_id

    timestamps()
  end

  def changeset(pull_worker_history, params \\ %{}) do
    pull_worker_history
    |> cast(params, [:last_run, :seller_id])
    |> validate_required([:last_run, :seller_id])
  end

  @doc """
  Gets the last run mark from the DB.
  If it's first run using amazon founded at date as the search start
  """
  def last_run_for(seller_id) do

    q = (from h in PullWorkerHistory, where: h.seller_id == ^seller_id, order_by: [desc: h.id], limit: 1)
    case Hyperion.Repo.all(q) do
      [] -> Timex.parse!("1994-07-05", "%Y-%m-%d", :strftime)
      r -> hd(r).last_run
    end
  end

  @doc """
  Stores run mark in the DB.
  If mark for `today' already exists — returns it,
  if not — creates new
  """
  def insert_run_mark(seller_id) do
    date = Timex.beginning_of_day(Timex.now)
    q = from p in PullWorkerHistory, where: p.last_run == ^date
    res = %PullWorkerHistory{seller_id: seller_id, last_run: date}
    Hyperion.Repo.one(q) || Hyperion.Repo.insert!(res)
  end
end
