defmodule OnboardingService.PageController do
  use OnboardingService.Web, :controller

  def index(conn, _params) do
    render conn, "index.html"
  end
end
