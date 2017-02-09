// @flow

// libs
import _ from 'lodash';
import React, { Element } from 'react';

// components
import { Link } from '../link';
import ObjectDetails from '../object-page/object-details';
import { renderFormField } from '../object-form/object-form-inner';
import AssociatedList from './associated-list';

const layout = require('./layout.json');

export default class ProductVariantDetails extends ObjectDetails {
  layout = layout;

  get title(): string {
    const productTitle = _.get(this.props.object, 'attributes.title.v');

    if (!_.isEmpty(this.props.object.options)) {
      const optionsString = _.map(this.props.object.options, option => option.value.name).join(', ');

      return `${productTitle} — ${optionsString}`;
    }

    return productTitle;
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

  get options(): Array<Element> {
    return _.map(this.props.object.options, option => {
      const name = _.get(option.attributes, 'name.v');
      const value = option.value.name;

      return renderFormField(name, <div>{value}</div>, {label: name});
    });
  }

  renderAssociatedVariants() {
    const list = [
      {
        image: 'https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Quay_Side.jpg',
        title: 'Nike Free 5.0 Women’s - Black, 6',
        subtitle: 'SKU-FRE2'
      },
      {
        image: 'https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Quay_Side.jpg',
        title: 'Nike Free 5.0 Women’s - Black, 7',
        subtitle: 'SKU-FRE3'
      },
    ];
    return (
      <AssociatedList
        title="Associated Variants"
        list={list}
      />
    );
  }

  renderGeneralSection() {
    return (
      <div>
        {this.titleField}
        {this.options}
        {this.skuField}
      </div>
    );
  }
}
