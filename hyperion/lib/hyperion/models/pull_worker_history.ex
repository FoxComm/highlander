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

  def last_run_for(seller_id) do
    # We're using amazon founded at date for furst run
    q = (from h in PullWorkerHistory, where: h.seller_id == ^seller_id, order_by: [desc: h.id], limit: 1)
    case Hyperion.Repo.all(q) do
      [] -> Timex.parse!("1994-07-05", "%Y-%m-%d", :strftime)
      r -> hd(r).last_run
    end
  end

  def insert_run_mark(seller_id) do
    res = %PullWorkerHistory{seller_id: seller_id, last_run: Timex.beginning_of_day(Timex.now)}
    case Hyperion.Repo.insert(res) do
      {:ok, record} -> record.last_run
      {:error, _ } -> raise "Can not create run mark!"
    end
  end
end
