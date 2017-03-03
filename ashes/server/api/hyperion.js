const router = require('koa-router')();

const mockSuggestResp = {
  "secondary": [
    {
      "node_path": "Clothing, Shoes & Jewelry/Novelty & More/Clothing/Novelty/Girls/Tops & Tees/T-Shirts",
      "node_id": 9057040011,
      "item_type": "novelty-t-shirts",
      "department": "girls"
    },
    {
      "node_path": "Clothing, Shoes & Jewelry/Novelty & More/Clothing/Novelty/Women/Tops & Tees/T-Shirts",
      "node_id": 9056923011,
      "item_type": "novelty-t-shirts",
      "department": "womens"
    },
    {
      "node_path": "Clothing, Shoes & Jewelry/Novelty & More/Clothing/Novelty/Boys/Tops & Tees/T-Shirts",
      "node_id": 9057094011,
      "item_type": "novelty-t-shirts",
      "department": "boys"
    },
    {
      "node_path": "Clothing, Shoes & Jewelry/Novelty & More/Clothing/Movie & TV Fan/Tops & Tees/T-Shirts",
      "node_id": 2491811011,
      "item_type": "movie-and-tv-fan-t-shirts",
      "department": null
    }
  ],
  "primary": {
    "node_path": "Clothing, Shoes & Jewelry/Boys/Clothing/Tops & Tees/Tees",
    "node_id": 1288961011,
    "item_type": "fashion-t-shirts",
    "department": "boys"
  },
  "count": 5
};

function processList(list) {
  if (!list) {
    return null;
  }

  return list.map(item => {
    let categories = item.node_path.split('/');
    const deepest = categories.pop();

    return {
      id: item.node_id,
      item_type: item.item_type,
      department: item.department,
      text: deepest,
      prefix: categories.join(' » '),
    };
  });
}

router.get('/api/v1/amazon/categories/suggester', function *(next) {
  const primary = processList([mockSuggestResp.primary]);
  const secondary = processList(mockSuggestResp.secondary);
  const send = {
    primary,
    secondary,
  };

  this.body = send;
});

module.exports = router;
