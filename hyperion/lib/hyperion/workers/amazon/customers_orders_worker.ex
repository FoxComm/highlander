defmodule Hyperion.Amazon.Workers.CustomersOrdersWorker do
  use GenServer
  require Logger

  alias Hyperion.PhoenixScala.Client
  alias Hyperion.Amazon

  def start_link do
    GenServer.start_link(__MODULE__, %{})
  end

  def init(state) do
    case Mix.env do
      :test -> {:ok, state}
      _ ->
        schedule_work()
        {:ok, state}
    end
  end

  def handle_info(:work, state) do
    do_work()
    schedule_work()
    {:noreply, state}
  end

  defp do_work() do
    try do
      get_credentials()
      |> fetch_amazon_orders
      |> store_customers_and_orders()
    rescue e in RuntimeError ->
      Logger.error "Error while fetching orders from Amazon: #{e.message}"
    end
  end

  def get_credentials() do
    cfg = Amazon.safe_fetch_config()
    if String.strip(cfg.seller_id) != "" do
      cfg
    else
      schedule_work()
      raise "Credentials not set. Exiting."
    end
  end

  defp fetch_amazon_orders(cfg) do
    date = PullWorkerHistory.last_run_for(cfg.seller_id)
           |>Timex.format!("%Y-%m-%dT%H:%M:%SZ", :strftime)
    list = [fulfillment_channel: ["MFN", "AFN"],
            created_after: [date]]
    Logger.info("Fetching order with params: #{inspect(list)}")

    case MWSClient.list_orders(list, cfg) do
      {:error, error} -> raise inspect(error)
      {:warn, warn} -> raise warn["ErrorResponse"]["Error"]["Message"]
      {_, resp} ->
        Logger.info("Orders fetched: #{inspect(resp)}")
        PullWorkerHistory.insert_run_mark(cfg.seller_id)
        resp["ListOrdersResponse"]["ListOrdersResult"]
    end
  end

  defp store_customers_and_orders(orders) do
    case orders["Orders"]["Order"] do
      list when is_list(list) -> Enum.each(list, fn order ->
                                  Client.create_order_and_customer(order)
                                 end)
      map when is_map(map) -> Client.create_order_and_customer(map)
      nil -> Logger.info "No orders present: #{inspect(orders)}"
      _ -> Logger.error "Some error occured! #{inspect(orders)}"
    end
  end

  defp schedule_work() do
    mins = Application.fetch_env!(:hyperion, :orders_fetch_interval) |> String.to_integer
    next_run = Timex.shift(Timex.now, minutes: mins) |> Timex.format!("{ISO:Extended}")
    Logger.info "Scheduling #{__MODULE__} for next run at: #{next_run}. Run Interval is set to #{mins} minute(s)"
    Process.send_after(self(), :work, mins * 60 * 1000)
  end
end
