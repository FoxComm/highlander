// @flow

// libs
import _ from 'lodash';
import React, { Element } from 'react';
import { autobind } from 'core-decorators';
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

class SkuInventoryPage extends ObjectPage {
  props: Props;

  get entityId() {
    return 1;
    return this.props.skuId;
  }

  receiveNewObject(nextObject) {
    this.setState({
      object: nextObject,
    });
  }

  transitionTo() {}
  transitionToList() {}

  @autobind
  sanitizeError(error: string): string {
    if (error.indexOf('duplicate key value violates unique constraint "skus_code_context_id"') != -1) {
      const code = _.get(this.state, 'entity.attributes.code.v');
      return `SKU with code ${code} already exists in the system`;
    }

    return error;
  }

  get body() {
    return (
      <div>
        <Prompt
          message="You have unsaved changes. Are you sure you want to leave this page?"
          when={this.unsaved}
        />
        {this.errors}
        {this.children}
      </div>
    );
  }
}

export default connectPage('sku', SkuActions)(SkuInventoryPage);
