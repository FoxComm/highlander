defmodule Hyperion.Amazon.Templates.Categories.ClothingAccessories do
  def template_string do
    """
    <ClothingAccessories>
      <VariationData>
          <Parentage><%= @parentage %></Parentage>
          <%= if @parentage == "child" do %>
            <Size><%= @size %></Size>
            <Color><%= @color %></Color>
            <VariationTheme>SizeColor</VariationTheme>
          <% end %>
        </VariationData>
      <ClassificationData>
        <Department><%= @department %></Department>
      </ClassificationData>
    </ClothingAccessories>
    """
  end
end
