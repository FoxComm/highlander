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
import SkuContentBox from './variants/variants';
import InputMask from 'react-input-mask';

import { renderFormField } from 'components/object-form/object-form-inner';

import { autoAssignOptions, deleteVariantCombination, addProductVariantsByOptionTuples } from 'paragons/variants';

import styles from './form.css';

// types
import type { DetailsProps } from '../object-page/object-details';
import type { Product, Option, OptionValue } from 'paragons/product';
const layout = require('./layout.json');

type Props = DetailsProps & {
  object: Product,
};

/**
 * ProductForm is dumb component that implements the logic needed for creating
 * or updating a product.
 */
export default class ProductForm extends ObjectDetails {
  layout = layout;
  props: Props;

  renderVariants(): Element<any> {
    const { props } = this;
    return (
      <Variants
        // $FlowFixMe: WTF?
        fullProduct={props.object}
        updateField={props.onSetVariantProperty}
        updateFields={props.onSetVariantProperties}
        onDeleteSku={this.handleDeleteSku}
        onAddNewOptions={this.handleAddOptionValues}
        options={props.object.options}
      />
    );
  }

  @autobind
  updateOptions(newOptions: Array<Option>): void {
    // here we have new variants, but
    // we don't have empty skus in order user be able to edit them
    // also we need skus for them in variant.values
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
  handleDeleteSku(variantId: string) {
    this.props.onUpdateObject(
      deleteVariantCombination(this.props.object, variantId)
    );
  }

  @autobind
  handleAddOptionValues(optionValuesTuples: Array<Array<OptionValue>>) {
    this.props.onUpdateObject(
      addProductVariantsByOptionTuples(this.props.object, optionValuesTuples)
    );
  }

  @autobind
  renderSlug() {
    return this.slugField;
  }

  @autobind
  onSlugChange(value: string) {
    const realSlug = _.toLower(value.replace(/\W/g, '-'));
    const product = assoc(this.props.object, 'slug', realSlug);
    this.props.onUpdateObject(product);
  }

  get slugField() {
    const value = _.get(this.props, 'object.slug', '');
    const fieldClass = `fc-object-form__field-value ${styles['slug-field']}`;
    const slugField = (
      <div styleName="slug-field-container" >
        <span styleName="prefix" >/products/</span>
        <input
          className={fieldClass}
          type="text"
          name="slug"
          value={value}
          onChange={({target}) => this.onSlugChange(target.value)}
        />
        <div styleName="field-comment">
          Slug can only contain letters, numbers, dashes, and underscores.
        </div>
      </div>
    );
    const opts = {
      label: 'Slug',
      required: false,
      isDefined: (value) => _.isEmpty(value),
    };

    return renderFormField('SLUG', slugField, opts);
  }
}
