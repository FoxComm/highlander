defmodule OnboardingService.Ping do
  use OnboardingService.Web, :controller

  def ping(conn, _params) do
    send_resp(conn, 204, "")
  end
end
