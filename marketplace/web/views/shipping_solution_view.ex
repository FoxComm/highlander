defmodule Marketplace.ShippingSolutionView do
  use Marketplace.Web, :view

  def render("index.json", %{shipping_solutions: shipping_solutions}) do
    %{shipping_solutions: render_many(shipping_solutions, Marketplace.ShippingSolutionView, "shipping_solution.json")}
  end

  def render("shipping_solution.json", %{shipping_solution: shipping_solution}) do
    %{id: shipping_solution.id,
      carrier_name: shipping_solution.carrier_name,
      price: shipping_solution.price
    }
  end

  def render("show.json", %{shipping_solution: shipping_solution}) do 
    %{shipping_solution: render_one(shipping_solution, Marketplace.ShippingSolutionView, "shipping_solution.json")}
  end
end
