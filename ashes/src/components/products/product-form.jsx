/**
 * @flow weak
 */

// libs
import React, { Component, Element, PropTypes } from 'react';
import { assoc } from 'sprout-data';
import { autobind } from 'core-decorators';
import _ from 'lodash';

// components
import ObjectDetails from '../object-page/object-details';
import OptionList from './options/option-list';
import SkuContentBox from './skus/sku-content-box';

import { autoAssignOptions, deleteVariantCombination, addSkusForVariants } from 'paragons/variants';

// types
import type { DetailsProps } from '../object-page/object-details';
import type { Product, Option, OptionValue } from 'paragons/product';
const layout = require('./layout.json');

type Props = DetailsProps & {
  object: Product,
}

/**
 * ProductForm is dumb component that implements the logic needed for creating
 * or updating a product.
 */
export default class ProductForm extends ObjectDetails {
  layout = layout;
  props: Props;

  renderSkuList(): Element<any> {
    const { props } = this;
    return (
      <SkuContentBox
        // $FlowFixMe: WTF?
        fullProduct={props.object}
        updateField={props.onSetSkuProperty}
        updateFields={props.onSetSkuProperties}
        onDeleteSku={this.handleDeleteSku}
        onAddNewVariants={this.handleAddVariants}
        options={props.object.options}
      />
    );
  }

  @autobind
  updateOptions(newOptions: Array<Option>): void {
    // here we have new variants, but
    // we don't have empty skus in order user be able to edit them
    // also we need skuCodes for them in variant.values
    const newProduct = autoAssignOptions(this.props.object, newOptions);
    this.props.onUpdateObject(newProduct);
  }

  updateAttributes(attributes: Attributes) {
    // If the product only has at least one SKU, ensure that a few fields are fields are
    // kept in sync:
    // * Active To
    // * Active From
    const newObject = super.updateAttributes(attributes);
    let variants = this.props.object.variants;
    if (variants.length) {
      const syncFields = ['activeTo', 'activeFrom'];
      const updateArgs =_.flatMap(syncFields, field => {
        const originalValue = _.get(this.props.object, ['attributes', field]);
        return [
          ['attributes', field], _.get(attributes, field, originalValue)
        ];
      });

      variants = _.map(variants, variant => assoc(variant, ...updateArgs));
    }

    return assoc(newObject,
      ['variants'], variants
    );
  }

  renderOptionList() {
    return (
      <OptionList
        // $FlowFixMe: WTF?
        product={this.props.object}
        options={this.props.object.options}
        updateOptions={this.updateOptions}
      />
    );
  }

  @autobind
  handleDeleteSku(skuCode: string) {
    this.props.onUpdateObject(
      deleteVariantCombination(this.props.object, skuCode)
    );
  }

  @autobind
  handleAddVariants(variantValues: Array<Array<OptionValue>>) {
    this.props.onUpdateObject(
      addSkusForVariants(this.props.object, variantValues)
    );
  }
}
