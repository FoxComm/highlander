// @flow

// libs
import _ from 'lodash';
import React, { Element } from 'react';
import { autobind } from 'core-decorators';

// components
import { Link, IndexLink } from '../link';
import LocalNav from '../local-nav/local-nav';
import { connectPage, ObjectPage } from 'components/object-page/object-page';

// actions
import * as SkuActions from 'modules/skus/details';

// types
import type { Sku } from 'modules/skus/details';
import type { ProductVariant } from 'modules/product-variants/details';

type Props = {
  skuId: number,
  children: Element,
  // connected
  actions: {
    skuNew: () => void,
    fetchSku: (skuId) => Promise,
    updateSku: (sku: Sku) => Promise,
  },
  originalObject: Sku,
  object: ProductVariant,
};

class SkuPage extends ObjectPage {
  props: Props;

  get pageTitle(): string {
    if (this.isNew) {
      return `New SKU`;
    }

    return _.get(this.props.originalObject, 'title', '');
  }

  get entityIdName(): string {
    return 'skuId';
  }

  subNav() {
    const { params } = this.props;

    return (
      <LocalNav>
        <IndexLink to="sku-details" params={params}>Details</IndexLink>
        <Link to="sku-inventory" params={params}>Inventory</Link>
      </LocalNav>
    );
  }

  @autobind
  sanitizeError(error: string): string {
    if (error.indexOf('duplicate key value violates unique constraint "skus_code_context_id"') != -1) {
      const code = _.get(this.state, 'entity.attributes.code.v');
      return `SKU with code ${code} already exists in the system`;
    }

    return error;
  }
}

export default connectPage('sku', SkuActions)(SkuPage);
