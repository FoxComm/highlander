// @flow

// libs
import _ from 'lodash';
import React, { Element } from 'react';

// components
import { Link } from '../link';
import ObjectDetails from '../object-page/object-details';
import { renderFormField } from '../object-form/object-form-inner';

const layout = require('./layout.json');

export default class ProductVariantDetails extends ObjectDetails {
  layout = layout;

  get title(): string {
    // @TODO: add option values to the title
    const productTitle = _.get(this.props.object, 'attributes.title.v');
    const optionsString = 'TODO, implement';
    return `${productTitle} — ${optionsString}`;
  }

  get titleField(): Element {
    return renderFormField('title', <span>{this.title}</span>, {label: 'title'});
  }

  get skuField(): Element {
    const skuCode = _.get(this.props.object, 'attributes.code.v');
    const skuId = this.props.object.skuId;

    const field = (
      <Link to="sku-details" params={{skuId: skuId}}>{skuCode}</Link>
    );

    return renderFormField('SKU', field, {label: 'SKU'});
  }

  renderGeneralSection() {
    return (
      <div>
        {this.titleField}
        {this.skuField}
      </div>
    );
  }
}
