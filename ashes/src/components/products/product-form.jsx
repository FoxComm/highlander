/**
 * @flow weak
 */

// libs
import React, { Element } from 'react';
import { assoc } from 'sprout-data';
import { autobind } from 'core-decorators';
import _ from 'lodash';

// components
import ObjectDetails from '../object-page/object-details';
import OptionList from './options/option-list';
import SkuContentBox from './skus/sku-content-box';
import TaxonomiesListWidget from '../taxonomies/widget/taxonomies-list-widget';

import { renderFormField } from 'components/object-form/object-form-inner';

import { autoAssignVariants, deleteVariantCombination, addSkusForVariants } from 'paragons/variants';

import styles from './form.css';

// types
import type { DetailsProps } from '../object-page/object-details';
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
        variants={props.object.variants}
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
    // If the product only has at least one SKU, ensure that a few fields are fields are
    // kept in sync:
    // * Active To
    // * Active From
    const newObject = super.updateAttributes(attributes);
    let skus = this.props.object.skus;
    if (skus.length) {
      const syncFields = ['activeTo', 'activeFrom'];
      const updateArgs = _.flatMap(syncFields, field => {
        const originalValue = _.get(this.props.object, ['attributes', field]);
        return [
          ['attributes', field], _.get(attributes, field, originalValue)
        ];
      });

      skus = _.map(skus, sku => assoc(sku, ...updateArgs));
    }

    return assoc(newObject,
      ['skus'], skus
    );
  }

  renderOptionList() {
    return (
      <OptionList
        // $FlowFixMe: WTF?
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
      <div styleName="slug-field-container">
        <span styleName="prefix">/products/</span>
        <input
          className={fieldClass}
          type="text"
          name="slug"
          value={value}
          onChange={({ target }) => this.onSlugChange(target.value)}
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

  renderTaxonomies() {
    const linkedTaxonomies = _.get(this.props.object, 'taxons', []);
    const productId = _.get(this.props.object, 'id', []);

    return (
      <TaxonomiesListWidget
        productId={productId}
        linkedTaxonomies={linkedTaxonomies}
        onChange={this.onTaxonsListChange}
      />
    );
  }

  @autobind
  onTaxonsListChange(addedTaxonList) {
    const newObject = assoc(this.props.object, 'taxons', addedTaxonList);

    this.props.onUpdateObject(newObject);
  }

}
