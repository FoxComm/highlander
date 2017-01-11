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

// types
import type { ProductVariant } from 'modules/product-variants/details';

type Props = {
  actions: {
    productVariantNew: () => void,
    fetchProductVariant: (code: string, context?: string) => Promise,
    createProductVariant: (variant: ProductVariant, context?: string) => Promise,
    updateProductVariant: (variant: ProductVariant, context?: string) => Promise,
    archiveProductVariant: (code: string, context?: string) => Promise,
  },
  params: { productVariantId: string },
  originalObject: ProductVariant,
  children: Element,
};

class ProductVariantPage extends ObjectPage {
  props: Props;

  static contextTypes = {
    router: PropTypes.object.isRequired,
  };

  get pageTitle(): string {
    if (this.isNew) {
      return `New Product Variant`;
    }

    return _.get(this.props.originalObject, 'attributes.title.v', '');
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
      <Link to="product-variant-inventory-details" params={params} key="inventory">Inventory</Link>,
      <Link to="product-variant-notes" params={params} key="notes">Notes</Link>,
      <Link to="product-variant-activity-trail" params={params} key="activity-trail">Activity Trail</Link>,
    ];
  }

  @autobind
  sanitizeError(error: string): string {
    if (error.indexOf('duplicate key value violates unique constraint "skus_code_context_id"') != -1) {
      const code = _.get(this.state, 'entity.attributes.code.v');
      return `SKU with code ${code} already exists in the system`;
    }

    return error;
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
}

export default connectPage('productVariant', ProductVariantActions, {schemaName: 'product-variant'})(ProductVariantPage);
