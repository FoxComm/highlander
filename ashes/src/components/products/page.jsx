// @flow
import React, { Component, Element, PropTypes } from 'react';
import { autobind } from 'core-decorators';
import _ from 'lodash';
import { setVariantAttribute, productVariantCode } from 'paragons/product';
import { assoc } from 'sprout-data';

// actions
import * as ProductActions from 'modules/products/details';
import { sanitizeError } from 'modules/products/details';

// components
import SubNav from './sub-nav';
import { connectPage, ObjectPage } from '../object-page/object-page';
import { Dropdown } from '../dropdown';

// types
import type { Product } from 'paragons/product';

type Props = {
  actions: {
    createProduct: (product: Product) => void,
    fetchProduct: (productId: string, context: string) => Promise,
    productNew: () => void,
    productDuplicate: () => void,
    updateProduct: (product: Product, context: string) => void,
    archiveProduct: (id: string|number, context: ?string) => Promise,
  },
  children: any,
  params: { productId: string, context: string },
  details: {
    product: ?Product,
  },
  originalObject: ?Product,
  selectContextAvailable: boolean,
};

const SELECT_CONTEXT = [
  ['default', 'Default'],
  ['ru', 'Russian'],
];

/**
 * ProductPage represents the default layout of a product details page.
 * It displays the title, sub nav, and save button.
 */
class ProductPage extends ObjectPage {
  props: Props;

  // $FlowFixMe: defaultProps
  static defaultProps = {
    selectContextAvailable: false,
  };

  get entityContext(): string {
    return _.get(this.props.params, 'context', 'default');
  }

  get pageTitle(): string {
    if (this.isNew) {
      return 'New Product';
    }

    return _.get(this.props.originalObject, 'attributes.title.v', '');
  }

  fetchEntity(): Promise {
    return this.props.actions.fetchProduct(this.entityId, this.entityContext);
  }

  removeEmptyVariants(object) {
    let existsCodes = {};

    let variants = _.reduce(object.variants, (acc, variant) => {
      const code = _.get(variant.attributes, 'code.v');
      if (code) {
        existsCodes[productVariantCode(variant)] = 1;
        return [...acc, variant];
      }
      return acc;
    }, []);

    if (!variants.length && object.variants.length) {
      const firstVariant = object.variants[0];
      variants = [firstVariant];
      existsCodes[productVariantCode(firstVariant)] = 1;
    }

    const options = _.map(object.options, option => {
      const values = _.map(option.values, value => {
        const skuCodes = _.reduce(value.skuCodes, (acc, code) => {
          if (code in existsCodes) {
            return [...acc, code];
          }
          return acc;
        }, []);
        return assoc(value, 'skuCodes', skuCodes);
      });
      return assoc(option, 'values', values);
    });

    return assoc(object,
      'variants', variants,
      'options', options
    );
  }

  prepareObjectForValidation(object) {
    return this.removeEmptyVariants(object);
  }

  prepareObjectForSaving(object) {
    return this.removeEmptyVariants(object);
  }

  get selectContextDropdown() {
    if (this.props.selectContextAvailable) {
      return (
        <Dropdown
          onChange={this.handleContextChange}
          value={this.entityContext}
          items={SELECT_CONTEXT}
        />
      );
    }
  }

  @autobind
  sanitizeError(error: string): string {
    return sanitizeError(error);
  }

  @autobind
  handleContextChange(context: string) {
    this.transitionTo(this.entityId, {
      context,
    });
    this.props.actions.fetchProduct(this.entityId, context);
  }

  @autobind
  handleSetVariantProperty(id: string, field: string, value: string) {
    const { object } = this.state;

    if (object) {
      this.setState({
        object: setVariantAttribute(object, id, field, value),
      });
    }
  }

  @autobind
  handleSetVariantProperties(id: string, updateArray: Array<Array<any>>) {
    const { object } = this.state;

    if (object) {
      const newProduct = _.reduce(updateArray, (p, [field, value]) => {
        return setVariantAttribute(p, id, field, value);
      }, object);
      this.setState({object: newProduct});
    }
  }

  handleDuplicate() {
    super.handleDuplicate();

    this.props.actions.productDuplicate();
  }

  createEntity(entity) {
    return this.props.actions.createProduct(entity, this.entityContext);
  }

  updateEntity(entity) {
    return this.props.actions.updateProduct(entity, this.entityContext);
  }

  @autobind
  archiveEntity() {
    this.props.actions.archiveProduct(this.entityId, this.entityContext).then(() => {
      this.transitionToList();
    });
  }

  detailsRouteProps(): Object {
    return {
      context: this.entityContext,
    };
  }

  childrenProps() {
    return {
      ...super.childrenProps(),
      onSetVariantProperty: this.handleSetVariantProperty,
      onSetVariantProperties: this.handleSetVariantProperties,
    };
  }

  subNav() {
    return <SubNav productId={this.entityId} product={this.state.object} context={this.entityContext} />;
  }

  renderHead() {
    return [
      this.selectContextDropdown,
      this.cancelButton,
    ];
  }
}

export default connectPage('product', ProductActions)(ProductPage);
