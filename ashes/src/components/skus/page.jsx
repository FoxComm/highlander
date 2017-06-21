/**
 * @flow
 */

// libs
import _ from 'lodash';
import React, { Element } from 'react';
import { autobind } from 'core-decorators';

// components
import { Link, IndexLink } from 'components/link';
import PageNav from 'components/core/page-nav';
import { connectPage, ObjectPage } from '../object-page/object-page';

// actions
import * as SkuActions from 'modules/skus/details';

type Props = {
  actions: {
    skuNew: () => void,
    fetchSku: (code: string, context?: string) => Promise<*>,
    createSku: (sku: Sku, context?: string) => Promise<*>,
    updateSku: (sku: Sku, context?: string) => Promise<*>,
    archiveSku: (code: string, context?: string) => Promise<*>,
  },
  params: { skuCode: string },
  originalObject: Sku,
  children: Element<*>,
};

const getCode = object => _.get(object, 'attributes.code.v', '');

class SkuPage extends ObjectPage {
  props: Props;

  getObjectId(object) {
    return getCode(object);
  }

  get code(): string {
    return getCode(this.props.originalObject);
  }

  get pageTitle(): string {
    if (this.isNew) {
      return `New SKU`;
    }

    return this.code.toUpperCase();
  }

  get entityIdName(): string {
    return 'skuCode';
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
      const code = _.get(this.state, 'object.attributes.code.v');
      return `SKU with code ${code} already exists in the system`;
    }

    return error;
  }

  subNav() {
    const { params } = this.props;

    return (
      <PageNav>
        <IndexLink to="sku-details" params={params}>Details</IndexLink>
        {this.detailsLinks}
      </PageNav>
    );
  }
}

export default connectPage('sku', SkuActions)(SkuPage);
