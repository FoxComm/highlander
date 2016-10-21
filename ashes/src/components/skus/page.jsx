/**
 * @flow
 */

// libs
import _ from 'lodash';
import React, { Element } from 'react';
import { autobind } from 'core-decorators';

// components
import { Link, IndexLink } from '../link';
import LocalNav from '../local-nav/local-nav';
import { connectPage, ObjectPage } from '../object-page/object-page';

// actions
import * as SkuActions from 'modules/skus/details';

// types
import type { Sku } from 'modules/skus/details';

type Props = {
  actions: {
    skuNew: () => void,
    fetchSku: (skuId: string, context?: string) => Promise,
    createSku: (sku: Sku, context?: string) => Promise,
    updateSku: (sku: Sku, context?: string) => Promise,
    archiveSku: (skuId: string, context?: string) => Promise,
  },
  params: { skuId: string },
  originalObject: Sku,
  children: Element,
};

class SkuPage extends ObjectPage {
  props: Props;

  get code(): string {
    return _.get(this.props.originalObject, 'attributes.code.v', '');
  }

  get pageTitle(): string {
    if (this.isNew) {
      return `New SKU`;
    }

    return this.code.toUpperCase();
  }

  get entityIdName(): string {
    return 'skuId';
  }

  get detailsLinks() {
    if (this.isNew) {
      return;
    }

    const { params } = this.props;

    return [
      <Link to="sku-images" params={params} key="images">Images</Link>,
      <Link to="sku-inventory-details" params={params} key="inventory">Inventory</Link>,
      <Link to="sku-notes" params={params} key="notes">Notes</Link>,
      <Link to="sku-activity-trail" params={params} key="activity-trail">Activity Trail</Link>,
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
        <IndexLink to="sku-details" params={params}>Details</IndexLink>
        {this.detailsLinks}
      </LocalNav>
    );
  }
}

export default connectPage('sku', SkuActions)(SkuPage);
