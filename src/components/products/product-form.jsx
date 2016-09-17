/**
 * @flow
 */

// libs
import React, { Component, Element, PropTypes } from 'react';
import { assoc } from 'sprout-data';
import { autobind } from 'core-decorators';
import _ from 'lodash';
import { flow, filter } from 'lodash/fp';

// components
import ContentBox from '../content-box/content-box';
import ObjectForm from '../object-form/object-form';
import ObjectScheduler from '../object-scheduler/object-scheduler';
import Tags from '../tags/tags';
import OptionList from './options/option-list';
import SkuContentBox from './skus/sku-content-box';

// types
import type { Attributes } from 'paragons/object';
import type { Product } from 'paragons/product';

// paragon
import { options } from 'paragons/product';

type Props = {
  product: Product,
  onUpdateProduct: (product: Product) => void,
  onSetSkuProperty: (code: string, field: string, value: any) => void,
  onSetSkuProperties: (code: string, toUpdate: Array<Array<any>>) => void,
};

type State = {
  isAddingProperty: bool,
};

const omitKeys = {
  general: ['skus', 'variants', 'activeFrom', 'activeTo', 'tags'],
};

const defaultKeys = {
  general: ['title', 'description'],
  misc: ['images'],
  seo: ['url', 'metaTitle', 'metaDescription'],
};

/**
 * ProductForm is dumb component that implements the logic needed for creating
 * or updating a product.
 */
export default class ProductForm extends Component {
  props: Props;

  state: State = {
    isAddingProperty: false,
  };

  get generalAttrs(): Array<string> {
    const toOmit = [
      ...defaultKeys.general,
      ...defaultKeys.misc,
      ...defaultKeys.seo,
      ..._.flatten(_.valuesIn(omitKeys)),
    ];
    const attributes = _.get(this.props, 'product.attributes', {});
    const filteredAttributes = flow(
      _.keys,
      filter((attr: string) => !_.includes(toOmit, attr))
    )(attributes);
    return [
      ...defaultKeys.general,
      ...filteredAttributes
    ];
  }

  get skusContentBox(): Element {
    const variants = [
      {
        "name": "Color",
        "values": [
          {
            "name": "Red",
            "swatch": "FF0000"
          },
          {
            "name": "Blue",
            "swatch": "0000FF"
          }
        ]
      },
      {
        "name": "Size",
        "values": [
          {
            "name": "S",
            "swatch": "S"
          },
          {
            "name": "M",
            "swatch": "M"
          },
          {
            "name": "L",
            "swatch": "L"
          }
        ]
      }
    ];

    return (
      <SkuContentBox
        fullProduct={this.props.product}
        updateField={this.props.onSetSkuProperty}
        updateFields={this.props.onSetSkuProperties}
        variants={variants}
      />
    );
  }

  @autobind
  handleProductChange(attributes: Attributes) {
    // If the product only has one SKU, ensure that a few fields are fields are
    // kept in sync:
    // * Title
    // * Active To
    // * Active From
    let skus = this.props.product.skus;
    if (skus.length == 1) {
      const title = _.get(this.props.product, 'attributes.title');
      const activeTo = _.get(this.props.product, 'attributes.activeTo');
      const activeFrom = _.get(this.props.product, 'attributes.activeFrom');
      skus = assoc(skus,
        [0, 'attributes', 'title'], title,
        [0, 'attributes', 'activeTo'], activeTo,
        [0, 'attributes', 'activeFrom'], activeFrom,
      );
    }

    const newProduct = assoc(
      this.props.product,
      ['attributes'], attributes,
      ['skus'], skus
    );
    this.props.onUpdateProduct(newProduct);
  }

  @autobind
  handleAddProperty() {
    this.setState({ isAddingProperty: true });
  }

  get productState(): Element {
    const { attributes } = this.props.product;

    return (
      <ObjectScheduler
        attributes={attributes}
        onChange={this.handleProductChange}
        title="Product" />
    );
  }

  render(): Element {
    const attributes = _.get(this.props, 'product.attributes', {});

    return (
      <div className="fc-grid fc-grid-no-gutter">
        <div className="fc-col-md-3-5">
          <ObjectForm
            canAddProperty={true}
            onChange={this.handleProductChange}
            fieldsToRender={this.generalAttrs}
            attributes={attributes}
            options={options}
            title="General" />

          <OptionList variants={this.props.product.variants} />

          {this.skusContentBox}

          <ObjectForm
            onChange={this.handleProductChange}
            fieldsToRender={defaultKeys.seo}
            attributes={attributes}
            title="SEO" />
        </div>
        <div className="fc-col-md-2-5">
          <Tags
            attributes={attributes}
            onChange={this.handleProductChange} />
          {this.productState}
        </div>
      </div>
    );
  }
}
