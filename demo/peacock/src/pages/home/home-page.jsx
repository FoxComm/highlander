/* @flow */

import React from 'react';
import Button from 'ui/buttons';
import ProductsList from 'components/related-products-list/related-products-list';
import { Link } from 'react-router';
import styles from './home-page.css';

const trending = [
  {
    id: 1,
    index: 1,
    productId: 6228,
    slug: 'stan-smith-shoes-5',
    context: 'default',
    title: 'Stan Smith Shoes',
    salePrice: '3000',
    retailPrice: '3000',
    currency: 'USD',
    albums: [{
      name: 'Default',
      images: [
        {
          src: 'http://demandware.edgesuite.net/sits_pod20-adidas/dw/image/v2/aaqx_prd/on/demandware.static/-/Sites-adidas-products/en_US/dw30317f50/zoom/S75187_01_standard.jpg',
        }, {
          src: 'http://demandware.edgesuite.net/sits_pod20-adidas/dw/image/v2/aaqx_prd/on/demandware.static/-/Sites-adidas-products/default/dw6ab2d047/zoom/B24101_01_standard.jpg',
        },
      ],
    }],
  }, {
    id: 2,
    index: 2,
    productId: 74301,
    slug: 'nmd_xr1-primeknit-shoes-3',
    context: 'default',
    title: 'NMD_XR1 Primeknit Shoes',
    salePrice: '14000',
    retailPrice: '14000',
    currency: 'USD',
    albums: [{
      name: 'Default',
      images: [
        {
          src: 'http://demandware.edgesuite.net/sits_pod20-adidas/dw/image/v2/aaqx_prd/on/demandware.static/-/Sites-adidas-products/en_US/dw30ac3559/zoom/BB1967_01_standard.jpg',
        },
      ],
    }],
  }, {
    id: 3,
    index: 3,
    productId: 15329,
    slug: 'd-rose-7-primeknit-shoes',
    context: 'default',
    title: 'D Rose 7 Primeknit Shoes',
    salePrice: '11200',
    retailPrice: '11200',
    currency: 'USD',
    albums: [{
      name: 'Default',
      images: [
        {
          src: 'http://demandware.edgesuite.net/sits_pod20-adidas/dw/image/v2/aaqx_prd/on/demandware.static/-/Sites-adidas-products/en_US/dw415d68bf/zoom/B72720_01_standard.jpg',
        }, {
          src: 'http://demandware.edgesuite.net/sits_pod20-adidas/dw/image/v2/aaqx_prd/on/demandware.static/-/Sites-adidas-products/en_US/dw415d68bf/zoom/B72720_01_standard.jpg',
        },
      ],
    }],
  }, {
    id: 4,
    index: 4,
    productId: 105477,
    slug: 'vengeful-shoes-2',
    context: 'default',
    title: 'Vengeful Shoes',
    salePrice: '7700',
    retailPrice: '7700',
    currency: 'USD',
    albums: [{
      name: 'Default',
      images: [
        {
          src: 'http://demandware.edgesuite.net/sits_pod20-adidas/dw/image/v2/aaqx_prd/on/demandware.static/-/Sites-adidas-products/en_US/dwdd8e5444/zoom/BB1638_01_standard.jpg',
        },
      ],
    }],
  },
];

const HomePage = () => {
  return (
    <div>
      <div styleName="header-wrap">
        <div styleName="header-content">
          <div styleName="header-title">
            Greater<br />Every Mile
          </div>
          <div styleName="header-body">
            <p>Training is over. Glory awaits.</p>
            <Button>
              Shop now
            </Button>
          </div>
        </div>
      </div>
      <ul styleName="category-list">
        <li styleName="category-item">
          <Link styleName="shoes-img" to="/s/shoes">
            <div styleName="category-title">Shoes</div>
          </Link>
        </li>
        <li styleName="category-item">
          <Link styleName="jackets-img" to="/s/jackets">
            <div styleName="category-title">Jackets</div>
          </Link>
        </li>
      </ul>
      <ProductsList
        list={trending}
        isLoading={false}
        loadingBehavior={1}
        title="Trending"
        productsOrder={[6228, 74301, 15329, 105477]}
      />
    </div>
  );
};

export default HomePage;
