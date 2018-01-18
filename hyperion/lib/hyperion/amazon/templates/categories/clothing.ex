defmodule Hyperion.Amazon.Templates.Categories.ClothingAccessories do
  def template_string do
    """
    <ClothingAccessories>
      <VariationData>
          <Parentage><%= @parentage %></Parentage>
          <%= if @parentage == "child" do %>
            <%= Hyperion.Amazon.TemplateBuilder.render_field(assigns[:size], "Size") %>
            <%= Hyperion.Amazon.TemplateBuilder.render_field(assigns[:color], "Color") %>
          <% end %>
          <%= Hyperion.Amazon.Templates.Categories.ClothingAccessories.render_variation_theme(assigns[:size], assigns[:color]) %>
        </VariationData>
      <ClassificationData>
        <Department><%= @department %></Department>
      </ClassificationData>
    </ClothingAccessories>
    """
  end

  # <VariationTheme>SizeColor</VariationTheme>
  def render_variation_theme(size, color) do
    cond do
      size && color ->
        """
        <VariationTheme>SizeColor</VariationTheme>
        """

      size && !color ->
        """
        <VariationTheme>Size</VariationTheme>
        """

      !size && color ->
        """
        <VariationTheme>Color</VariationTheme>
        """

      true ->
        """
        #{nil}
        """
    end
  end
end
