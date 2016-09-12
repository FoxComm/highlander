/**
 * @flow
 */

// libs
import _ from 'lodash';
import React, { Element } from 'react';

// components
import { Link, IndexLink } from '../link';
import LocalNav from '../local-nav/local-nav';
import { connectPage, ObjectPage } from '../object-page/object-page';

// actions
import * as SkuActions from 'modules/skus/details';

//helpers
import { isSkuValid } from 'paragons/sku';

// types
import type { Sku } from 'modules/skus/details';

type Props = {
  actions: {
    skuNew: () => void,
    fetchSku: (code: string, context?: string) => Promise,
    createSku: (sku: Sku, context?: string) => Promise,
    updateSku: (sku: Sku, context?: string) => Promise,
    archiveSku: (code: string, context?: string) => Promise,
  },
  sku: ?Sku,
  params: { skuCode: string },
  children: Element,
};

class SkuPage extends ObjectPage {
  props: Props;

  get code(): string {
    return _.get(this.entity, 'attributes.code.v', '');
  }

  get pageTitle(): string {
    if (this.isNew) {
      return `New SKU`;
    }

    return this.code.toUpperCase();
  }

  get preventSave(): boolean {
    const { entity } = this.state;
    if (entity) {
      return !isSkuValid(entity);
    }

    return true;
  }

  get entityIdName(): string {
    return 'skuCode';
  }

  subNav() {
    const { params } = this.props;
    return (
      <LocalNav>
        <IndexLink to="sku-details" params={params}>Details</IndexLink>
        <Link to="sku-images" params={params}>Images</Link>
        <Link to="sku-inventory-details" params={params}>Inventory</Link>
        <Link to="sku-notes" params={params}>Notes</Link>
        <Link to="sku-activity-trail" params={params}>Activity Trail</Link>
      </LocalNav>
    );
  }
}

export default connectPage('sku', SkuActions)(SkuPage);
