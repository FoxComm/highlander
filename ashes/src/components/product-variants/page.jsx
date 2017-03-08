// @flow

// libs
import _ from 'lodash';
import React, { Element, PropTypes } from 'react';
import { autobind } from 'core-decorators';

// components
import { Link, IndexLink } from '../link';
import LocalNav from '../local-nav/local-nav';
import { connectPage, ObjectPage } from '../object-page/object-page';

// actions
import * as ProductVariantActions from 'modules/product-variants/details';
import { addExtraLinks } from 'modules/breadcrumbs';

// types
import type { ProductVariant } from 'modules/product-variants/details';

type Props = {
  actions: {
    productVariantNew: () => void,
    fetchProductVariant: (code: string, context?: string) => Promise<*>,
    createProductVariant: (variant: ProductVariant, context?: string) => Promise<*>,
    updateProductVariant: (variant: ProductVariant, context?: string) => Promise<*>,
    archiveProductVariant: (code: string, context?: string) => Promise<*>,
  },
  dispatch: (action: Function|Object) => any,
  route: {
    name: string,
  },
  params: { productVariantId: string },
  originalObject: ProductVariant,
  children: Element<*>,
};

class ProductVariantPage extends ObjectPage {
  props: Props;

  static contextTypes = {
    router: PropTypes.object.isRequired,
  };

  getTitle(object: ProductVariant): string {
    if (_.isEmpty(object)) return '';
    const productTitle = _.get(object, 'attributes.title.v', '');

    if (!_.isEmpty(object.options)) {
      const optionsString = _.map(object.options, option => option.value.name).join(', ');

      return `${productTitle} — ${optionsString}`;
    }

    return productTitle;
  }

  receiveNewObject(nextObject) {
    super.receiveNewObject(nextObject);

    if (_.isEmpty(nextObject)) return;

    const productTitle = _.get(nextObject.product, 'attributes.title.v');

    this.props.dispatch(addExtraLinks(this.props.route.name, [{
      to: 'product',
      params: {
        context: 'ru',
        productId: nextObject.product.id,
      },
      title: `Product ${productTitle}`,
    }]));
  }

  get pageTitle(): string {
    if (this.isNew) {
      return `New Product Variant`;
    }

    return this.getTitle(this.props.originalObject);
  }

  get entityIdName(): string {
    return 'productVariantId';
  }

  get detailsLinks() {
    if (this.isNew) {
      return;
    }

    const { params } = this.props;

    return [
      <Link to="product-variant-images" params={params} key="images">Images</Link>,
      <Link to="product-variant-inventory" params={params} key="inventory">Inventory</Link>,
      <Link to="product-variant-notes" params={params} key="notes">Notes</Link>,
      <Link to="product-variant-activity-trail" params={params} key="activity-trail">Activity Trail</Link>,
    ];
  }

  subNav() {
    const { params } = this.props;

    return (
      <LocalNav>
        <IndexLink to="product-variant-details" params={params}>Details</IndexLink>
        {this.detailsLinks}
      </LocalNav>
    );
  }

  childrenProps() {
    return {
      ...super.childrenProps(),
      productVariantTitle: this.getTitle(this.state.object),
    };
  }
}

export default connectPage(
  'productVariant', ProductVariantActions, {schemaName: 'product-variant'}
)(ProductVariantPage);
