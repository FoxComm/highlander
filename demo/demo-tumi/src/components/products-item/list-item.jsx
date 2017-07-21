/* @flow */

import React, { Element } from 'react';
import { findDOMNode } from 'react-dom';
import styles from './list-item.css';
import { Link } from 'react-router';
import _ from 'lodash';
import { autobind } from 'core-decorators';
import { addLineItem, toggleCart } from 'modules/cart';
import { connect } from 'react-redux';
import * as tracking from 'lib/analytics';
import { fancyColors } from 'modules/products';

import Currency from 'ui/currency';
import ImagePlaceholder from './image-placeholder';
import ProductImage from 'components/image/image';
import Gallery from 'ui/gallery/gallery';

type Image = {
  alt?: string,
  src: string,
  title?: string,
  baseurl?: string,
};

type Album = {
  name: string,
  images: Array<Image>,
};

type Product = {
  id: number,
  index: number,
  productId: number,
  slug: ?string,
  context: string,
  title: string,
  description: ?string,
  salePrice: string,
  retailPrice: string,
  currency: string,
  albums: ?Array<Album> | Object,
  skus: Array<string>,
  tags?: Array<string>,
  addLineItem: Function,
  toggleCart: Function,
};

type State = {
  error?: any,
  mouseOver: boolean,
};

class ListItem extends React.Component {
  props: Product;
  state: State = {
    mouseOver: false,
    error: null,
  };

  static defaultProps = {
    skus: [],
  };

  @autobind
  addToCart() {
    const skuId = this.props.skus[0];
    const quantity = 1;

    tracking.addToCart(this.props, quantity);
    this.props.addLineItem(skuId, quantity)
      .then(() => {
        this.props.toggleCart();
      })
      .catch((ex) => {
        this.setState({
          error: ex,
        });
      });
  }

  get image() {
    const previewImageUrl = _.get(this.props.albums, [0, 'images', 0, 'src']);

    return previewImageUrl
      ? <ProductImage src={previewImageUrl} styleName="preview-image" ref="image" width={300} height={300} />
      : <ImagePlaceholder ref="image" />;
  }

  get gallery() {
    const images = _.get(this.props, ['albums', 0, 'images'], []);

    let imageUrls = [];
    if (images.length > 1) {
      imageUrls = _.slice(images, 1).map(image => image.src);
    } else if (images.length == 1) {
      imageUrls = [images[0].src];
    }

    return (
      <Gallery
        images={imageUrls}
      />
    );
  }

  getImageNode() {
    return findDOMNode(this.refs.image);
  }

  @autobind
  handleClick() {
    const { props } = this;

    tracking.clickPdp(props, props.index);
  }

  @autobind
  onMouseEnter() {
    this.setState({ mouseOver: true });
  }

  @autobind
  onMouseLeave() {
    this.setState({ mouseOver: false });
  }

  isOnSale(): Element<*> {
    const { currency } = this.props;

    let {
      salePrice,
      retailPrice,
    } = this.props;

    salePrice = Number(salePrice);
    retailPrice = Number(retailPrice);

    return (retailPrice > salePrice) ? (
      <div styleName="price">
        <Currency
          styleName="retail-price"
          value={retailPrice}
          currency={currency}
        />
        <Currency
          styleName="on-sale-price"
          value={salePrice}
          currency={currency}
        />
      </div>
      ) : (
        <div styleName="price">
          <Currency value={salePrice} currency={currency} />
        </div>
      );
  }

  get label() {
    // ToDo: condition for data to check BEST SELLER vs TUMI EXCLUSIVE vs other
    if (this.props.productId % 3 == 0) {
      return <div styleName="empty-label" />;
    }

    const message = this.props.productId % 2 == 0 ? 'TUMI EXCLUSIVE' : 'BEST SELLER';

    return (
      <div styleName="label">
        {message}
      </div>
    );
  }

  get hoverGallery() {
    return (
      <div styleName="hover-gallery">
        <div styleName="hover-inner">
          {this.gallery}
        </div>
      </div>
    );
  }

  get colorBlocks() {
    const taxonomies = _.get(this.props, 'taxonomies', []);
    const colorGroups = _.find(taxonomies, { taxonomy: 'colorGroup' });
    const colorTaxons = _.get(colorGroups, 'taxons', []);
    const colors = _.reduce(colorTaxons, (acc, taxon) => {
      const value = _.toUpper(taxon[0]);
      if (acc.indexOf(value) < 0) {
        const next = acc.concat([value]);
        return next;
      }
      return acc;
    }, []);

    let body = null;
    const size = _.size(colors);
    if (size > 1 && size <= 3) {
      body = _.map(colors, (color) => {
        const style = { backgroundColor: color };
        return <div styleName="color-swatch" style={style} />;
      });
    }
    if (size > 3) {
      body = _.map(colors.slice(0, 3), (color) => {
        const style = { backgroundColor: fancyColors[color] };
        return <div styleName="color-swatch" style={style} />;
      });
      body.push(
        <div styleName="color-swatch">+1</div>
      );
    }

    return (
      <div styleName="color-swatches">
        {body}
      </div>
    );
  }

  get collection() {
    const taxonomies = _.get(this.props, 'taxonomies', []);
    const collection = _.find(taxonomies, { taxonomy: 'collection' });
    const collectionNames = _.get(collection, 'taxons', []);

    return _.size(collectionNames) > 0 ? collectionNames[0] : '';
  }

  get normalState() {
    const {
      productId,
      slug,
      title,
    } = this.props;

    const productSlug = slug != null && !_.isEmpty(slug) ? slug : productId;

    const image = this.state.mouseOver
      ? (
          <div>{this.hoverGallery}</div>
      ): (
          <div styleName="preview">
            {this.image}
          </div>
      );

    return (
      <Link
        styleName="link"
        onClick={this.handleClick}
        to={`/products/${productSlug}`}
      >
        <div styleName="dark-overlay"></div>
        <div styleName="link-content">
          {this.label}
          {image}

          {this.colorBlocks}

          <div styleName="text-block">
            <div styleName="title-line">
              <h1 styleName="title" alt={title}>
                {title}
              </h1>
              <h2 styleName="subtitle">
                {this.collection}
              </h2>
            </div>
            <div styleName="price-line">
              {this.isOnSale()}
            </div>
          </div>
        </div>
      </Link>
    );
  }

  get hoverState() {
    if (!this.state.mouseOver) return null;

    const {
      productId,
      slug,
      title,
    } = this.props;

    const productSlug = slug != null && !_.isEmpty(slug) ? slug : productId;

    return (
      <div styleName="preview-hover">
        <Link
          className={`${styles['link-inner']} ${styles['link-inner-label']}`}
          onClick={this.handleClick}
          to={`/products/${productSlug}`}
        >
          {this.label}
        </Link>

        {this.hoverGallery}

        <Link
          styleName="link-inner"
          onClick={this.handleClick}
          to={`/products/${productSlug}`}
        >

          {this.colorBlocks}

          <div styleName="text-block">
            <div styleName="title-line">
              <h1 styleName="title" alt={title}>
                {title}
              </h1>
              <h2 styleName="subtitle">
                {this.collection}
              </h2>
            </div>
            <div styleName="price-line">
              {this.isOnSale()}
            </div>
          </div>
        </Link>
      </div>
    );
  }

  render(): Element<*> {
    /* {this.hoverState}*/
    return (
      <div
        styleName="list-item"
        onMouseEnter={this.onMouseEnter}
        onMouseLeave={this.onMouseLeave}
      >
        {this.normalState}
      </div>
    );
  }
}

export default connect(null, {
  addLineItem,
  toggleCart,
}, void 0, { withRef: true })(ListItem);
