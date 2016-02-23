const products = [
  {
    id: 1,
    isActive: true,
    attributes: {
      title: {
        type: 'string',
        default: 'Scala Mug',
      },
      description: {
        type: 'string',
        default: 'This is a Scala mug. Why would you want one?',
      },
      images: {
        type: 'images',
        default: ['http://lorempixel.com/75/75/fashion/'],
      },
    },
    variants: {
      default: ['SCALA-MUG-RED', 'SCALA-MUG-BLU', 'SCALA-MUG-BLK'],
    },
  },
  {
    id: 2,
    isActive: true,
    attributes: {
      title: {
        type: 'string',
        default: 'C++ Mug',
      },
      description: {
        type: 'string',
        default: 'This is a C++ mug. It\'s way better than the Scala mug.',
      },
      images: {
        type: 'images',
        default: ['http://lorempixel.com/75/75/fashion/'],
      },
    },
    variants: {
      default: [],
    },
  },
];

const productShadows = [
  {
    id: 1,
    productContextId: 1,
    productId: 1,
    attributes: {
      title: 'default',
      description: 'default',
      images: 'default',
    },
  },
  {
    id: 2,
    productContextId: 1,
    productId: 2,
    attributes: {
      title: 'default',
      description: 'default',
      images: 'default',
    },
  },
];

const skus = [
  {
    sku: 'SCALA-MUG-BLK',
    attributes: {
      title: {
        type: 'string',
        default: 'Black Scala Mug',
      },
      color: {
        type: 'color',
        default: {
          name: 'black',
          swatch: '#000',
          title: 'Midnight Black',
        },
      },
      price: {
        type: 'price',
        default: {
          price: '1599',
          currency: 'USD',
        },
      },
    },
  },
  {
    sku: 'SCALA-MUG-RED',
    attributes: {
      title: {
        type: 'string',
        default: 'Red Scala Mug',
      },
      color: {
        type: 'color',
        default: {
          name: 'red',
          swatch: '#ff0000',
          title: 'Ruby Red',
        },
      },
      price: {
        type: 'price',
        default: {
          price: '1599',
          currency: 'USD',
        },
      },
    },
  },
  {
    sku: 'SCALA-MUG-BLU',
    attributes: {
      title: {
        type: 'string',
        default: 'Blue Scala Mug',
      },
      color: {
        type: 'color',
        default: {
          name: 'blue',
          swatch: '#0000ff',
          title: 'Ocean Blue',
        },
      },
      price: {
        type: 'price',
        default: {
          price: '1799',
          currency: 'USD',
        },
      },
    },
  },
];

const skuShadows = [
  {
    id: 1,
    productContextId: 1,
    skuId: 1,
    attributes: {
      title: 'default',
      color: 'default',
      price: 'default',
    },
  },
  {
    id: 2,
    productContextId: 1,
    skuId: 2,
    attributes: {
      title: 'default',
      color: 'default',
      price: 'default',
    },
  },
  {
    id: 3,
    productContextId: 1,
    skuId: 3,
    attributes: {
      title: 'default',
      color: 'default',
      price: 'default',
    },
  },
];
