defmodule Hyperion.Amazon.Workers.PushCheckerWorker do
  use GenServer
  require Logger
  import Ecto.Query

  def start_link do
    GenServer.start_link(__MODULE__, %{})
  end

  def init(state) do
    schedule_work()
    {:ok, state}
  end

  def handle_info(:work, state) do
    do_work()
    schedule_work()
    {:noreply, state}
  end

  defp do_work() do
    case get_uncomplete_pushes() do
      [] -> Logger.info "No incomplete pushes found"
      pushes -> check_feeds_status(pushes)
    end
  end

  defp get_uncomplete_pushes do
     (from r in SubmissionResult, where: r.completed == false
      and not is_nil(r.product_feed)
      and not is_nil(r.price_feed)
      and not is_nil(r.inventory_feed)
      and not is_nil(r.variations_feed)
      and not is_nil(r.images_feed))
    |> Hyperion.Repo.all
  end

  # loop for each push
  # checks feed submission result from amazon for each feed in push
  defp check_feeds_status(pushes) do
    cfg = Hyperion.Amazon.fetch_config
    feed_names = [:images_feed, :inventory_feed, :price_feed, :product_feed, :variations_feed]
    Enum.each(pushes, fn(push) ->
      Enum.each(feed_names, fn(name)->
        check_feed(push, name, cfg)
      end)
    end)
  end

  defp check_feed(push, feed_name, cfg) do
    case MWSClient.get_feed_submission_result(Map.get(push, feed_name)["FeedSubmissionId"], cfg) do
      {:error, error} -> Logger.error "Push ID: #{push.id} checking error: #{inspect(error)}"
      {_, resp} ->
        feed_result_name = String.to_atom("#{Atom.to_string(feed_name)}_result")
        data = resp["AmazonEnvelope"]["Message"]["ProcessingReport"]
        store_result(push.id, feed_result_name, data)
    end
  end

  defp store_result(push_id, feed_result_name, result) do
    row = Hyperion.Repo.get(SubmissionResult, push_id)
    row = Ecto.Changeset.change(row, %{feed_result_name => result})
    case Hyperion.Repo.update(row) do
      {:ok, struct} ->
        Logger.info("Push ID: #{push_id}. #{feed_result_name}: #{inspect(result)}")
        mark_as_complete(struct)
      {:error, changeset} -> Logger.error("#{push_id}: Error while updating push with results: #{inspect(changeset)}")
    end
  end

  defp mark_as_complete(push) do
    res = Map.take(push, [:images_feed_result, :inventory_feed_result,
                          :price_feed_result, :product_feed_result, :variations_feed_result])
    steps_count = Map.keys(res) |> Enum.count
    results_count = Map.values(res) |> Enum.uniq |> Enum.count
    if (steps_count == results_count), do: SubmissionResult.mark_as_complete(push)
  end

  defp schedule_work do
    mins = Application.fetch_env!(:hyperion, :push_check_interval) |> String.to_integer
    Process.send_after(self(), :work, mins * 60 * 1000)
  end
end
