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

import { autoAssignVariants, deleteVariantCombination, addSkusForVariants } from 'paragons/variants';

// types
import type { Attributes } from 'paragons/object';
import type { DetailsProps } from '../object-page/object-details';
import type { Product, Option, OptionValue } from 'paragons/product';
const layout = require('./layout.json');

/**
 * ProductForm is dumb component that implements the logic needed for creating
 * or updating a product.
 */
export default class ProductForm extends ObjectDetails {
  layout = layout;
  props: DetailsProps & {
    object: Product
  };

  renderSkuList(): Element<any> {
    return (
      <SkuContentBox
        fullProduct={this.props.object}
        updateField={this.props.onSetSkuProperty}
        updateFields={this.props.onSetSkuProperties}
        onDeleteSku={this.handleDeleteSku}
        onAddNewVariants={this.handleAddVariants}
        variants={this.props.object.variants}
      />
    );
  }

  @autobind
  updateVariants(newVariants: Array<Option>): void {
    // here we have new variants, but
    // we don't have empty skus in order user be able to edit them
    // also we need skuCodes for them in variant.values
    const newProduct = autoAssignVariants(this.props.object, newVariants);
    this.props.onUpdateObject(newProduct);
  }

  updateAttributes(attributes: Attributes) {
    // If the product only has one SKU, ensure that a few fields are fields are
    // kept in sync:
    // * Title
    // * Active To
    // * Active From
    const newObject = super.updateAttributes(attributes);
    let skus = this.props.object.skus;
    if (skus.length == 1) {
      const syncFields = ['title', 'activeTo', 'activeFrom'];
      const updateArgs =_.flatMap(syncFields, field => {
        const originalValue = _.get(this.props.object, ['attributes', field]);
        return [
          [0, 'attributes', field], _.get(attributes, field, originalValue)
        ];
      });

      skus = assoc(skus, ...updateArgs);
    }

    return assoc(newObject,
      ['skus'], skus
    );
  }

  renderOptionList() {
    return (
      <OptionList
        product={this.props.object}
        variants={this.props.object.variants}
        updateVariants={this.updateVariants}
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
