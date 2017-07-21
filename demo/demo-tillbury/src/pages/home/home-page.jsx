/* @flow */

// libs
import React, { Component } from 'react';

import ProductsList from 'components/products-list/products-list';
import Gallery from 'ui/gallery/gallery';

// styles
import styles from './home-page.css';


const trending = [
  {
    id: 1,
    index: 1,
    productId: 6228,
    slug: 'charlottes-magic-cream',
    context: 'default',
    title: 'CHARLOTTE\'S MAGIC CREAM',
    salePrice: '10000',
    retailPrice: '10000',
    currency: 'USD',
    albums: [{
      name: 'Default',
      images: [
        {
          src: 'http://media.charlottetilbury.com/catalog/product/cache/2/small_image/296x340/9df78eab33525d08d6e5fb8d27136e95/m/a/magic-cream1.jpg',
        },
      ],
    }],
  }, {
    id: 2,
    index: 2,
    productId: 74301,
    slug: 'wonderglow',
    context: 'default',
    title: 'WONDERGLOW',
    salePrice: '5500',
    retailPrice: '5500',
    currency: 'USD',
    albums: [{
      name: 'Default',
      images: [
        {
          src: 'http://media.charlottetilbury.com/catalog/product/cache/2/small_image/296x340/9df78eab33525d08d6e5fb8d27136e95/w/o/wonderglow_1_1_1.png',
        },
      ],
    }],
  }, {
    id: 3,
    index: 3,
    productId: 15329,
    slug: 'supermodel-body',
    context: 'default',
    title: 'SUPERMODEL BODY',
    salePrice: '6500',
    retailPrice: '6500',
    currency: 'USD',
    albums: [{
      name: 'Default',
      images: [
        {
          src: 'http://media.charlottetilbury.com/catalog/product/cache/2/small_image/296x340/9df78eab33525d08d6e5fb8d27136e95/s/u/supermodel-body-front-view_1.jpg',
        },
      ],
    }],
  }, {
    id: 4,
    index: 4,
    productId: 105477,
    slug: 'filmstar-bronze-&-glow',
    context: 'default',
    title: 'FILMSTAR BRONZE & GLOW',
    salePrice: '6800',
    retailPrice: '6800',
    currency: 'USD',
    albums: [{
      name: 'Default',
      images: [
        {
          src: 'http://media.charlottetilbury.com/catalog/product/cache/2/small_image/296x340/9df78eab33525d08d6e5fb8d27136e95/f/i/filmstar-bronze-and-glow_open_1.jpg',
        },
      ],
    }],
  },
];

const images = [
  'http://www.charlottetilbury.com/media/wysiwyg/OVERNIGHT-BRONZE-_-GLOW-2.jpg',
  'http://www.charlottetilbury.com/media/wysiwyg/MOTHERS-DAY_2.jpg',
  'http://www.charlottetilbury.com/media/wysiwyg/59084ea06d91b186627551.gif',
  'http://www.charlottetilbury.com/media/wysiwyg/BEAUTY-GLOW-PALETTE_1_.jpg',
];

class HomePage extends Component {

  get leftButton() {
    return (onClick, disabled) => {
      const handleClick = (event) => {
        event.stopPropagation();
        event.preventDefault();
        onClick(event);
      };

      return (
        <div styleName="left-handle">
          <a
            styleName="left"
            disabled={disabled}
            onClick={handleClick}
          />
        </div>
      );
    };
  }

  get rightButton() {
    return (onClick, disabled) => {
      const handleClick = (event) => {
        event.stopPropagation();
        event.preventDefault();
        onClick(event);
      };

      return (
        <div styleName="right-handle">
          <a
            styleName="right"
            disabled={disabled}
            onClick={handleClick}
          />
        </div>
      );
    };
  }

  get gallery() {
    return (
      <div styleName="gallery">
        <Gallery
          images={images}
          rightNav={this.rightButton}
          leftNav={this.leftButton}
        />
      </div>
    );
  }

  render() {
    return (
      <div styleName="page">
        {this.gallery}
        <div styleName="sample-banner">
          ENJOY FREE GROUND SHIPPING ON ALL ORDERS!
        </div>

        <div styleName="row">
          <img src="http://www.charlottetilbury.com/media/wysiwyg/WEDDING-HOME-PROMO-1.jpg" />
        </div>

        <div styleName="row">
          <div styleName="pillow">
            <img src="http://www.charlottetilbury.com/media/wysiwyg/PILLOW-TALK-FILTER_2.1.jpg" />
          </div>
          <div styleName="magic">
            <img src="http://www.charlottetilbury.com/media/wysiwyg/SKin-Clinic-Trial-US_CA.jpg" />
          </div>
        </div>

        <div styleName="row">
          <img src="http://www.charlottetilbury.com/media/wysiwyg/Light_Wonder_Foundation.gif" />
        </div>

        <div styleName="row">
          <div styleName="bestsellers">
            <div styleName="top-flourish" />
            <div styleName="bottom-flourish" />
            <div styleName="title">
              BESTSELLERS
            </div>
            <div styleName="list">
              <ProductsList
                key="featured-product-list"
                list={trending}
                isLoading={false}
                loadingBehavior={1}
                size="small"
                showAddToCartButton={false}
                showServings
                showDescriptionOnHover={false}
              />
            </div>
          </div>
        </div>

        <div styleName="row">
          <div styleName="my-charlotte-title">
            <p styleName="olapic-header-title">
              Tag &nbsp;
              <span styleName="middleyour">Your</span>
              &nbsp; Tilbury
            </p>
            <p>
              <span styleName="endhashtag">
                #CharlotteTilbury
              </span>
            </p>
            <img src="/images/home-page/olapic.png" />
          </div>
        </div>
      </div>
    );
  }
}

export default HomePage;
