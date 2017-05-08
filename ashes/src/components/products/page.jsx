/**
 * @flow
 */

// libs
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import _ from 'lodash';
import { setSkuAttribute, skuId } from 'paragons/product';
import { assoc } from 'sprout-data';

// actions
import * as productActions from 'modules/products/details';
import { sanitizeError } from 'modules/products/details';
import { transitionTo } from 'browserHistory';

// components
import SubNav from './sub-nav';
import { connectPage, ObjectPage } from '../object-page/object-page';
import { Dropdown } from '../dropdown';
import { Button } from 'components/core/button';
import s from './page.css';

type Props = {
  actions: {
    newEntity: () => void,
    createProduct: (product: Product) => void,
    fetchProduct: (productId: string, context: string) => Promise<*>,
    productNew: () => void,
    productDuplicate: () => void,
    updateProduct: (product: Product, context: string) => void,
    archiveProduct: (id: string|number, context: ?string) => Promise<*>,
  },
  children: any,
  params: { productId: string, context: string },
  details: {
    product: ?Product,
  },
  originalObject: ?Product,
  selectContextAvailable: boolean,
  hasAmazon: ?boolean,
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

  fetchEntity(): Promise<*> {
    return this.props.actions.fetchProduct(this.entityId, this.entityContext);
  }

  removeEmptySkus(object) {
    let existsCodes = {};

    let skus = _.reduce(object.skus, (acc, sku) => {
      const code = _.get(sku.attributes, 'code.v');
      if (code) {
        existsCodes[skuId(sku)] = 1;
        return [...acc, sku];
      }
      return acc;
    }, []);

    if (!skus.length && object.skus.length) {
      const firstSku = object.skus[0];
      skus = [firstSku];
      existsCodes[skuId(firstSku)] = 1;
    }

    const variants = _.map(object.variants, variant => {
      const values = _.map(variant.values, value => {
        const skuCodes = _.reduce(value.skuCodes, (acc, code) => {
          if (code in existsCodes) {
            return [...acc, code];
          }
          return acc;
        }, []);
        return assoc(value, 'skuCodes', skuCodes);
      });
      return assoc(variant, 'values', values);
    });

    return assoc(object,
      'skus', skus,
      'variants', variants
    );
  }

  prepareObjectForValidation(object) {
    return this.removeEmptySkus(object);
  }

  prepareObjectForSaving(object) {
    return this.removeEmptySkus(object);
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
  handleSetSkuProperty(code: string, field: string, value: string) {
    const { object } = this.state;

    if (object) {
      this.setState({
        object: setSkuAttribute(object, code, field, value),
      });
    }
  }

  @autobind
  handleSetSkuProperties(code: string, updateArray: Array<Array<any>>) {
    const { object } = this.state;

    if (object) {
      const newProduct = _.reduce(updateArray, (p, [field, value]) => {
        return setSkuAttribute(p, code, field, value);
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
      onSetSkuProperty: this.handleSetSkuProperty,
      onSetSkuProperties: this.handleSetSkuProperties,
    };
  }

  subNav() {
    return <SubNav productId={this.entityId} product={this.state.object} context={this.entityContext} />;
  }

  get amazonButton(): ?Element<*> {
    const amazonTitle = 'Push to Amazon';

    return (
      <Button key="amazonButton" type="button" onClick={() => this.handleAmazon()} className={s.amazonButton}>
        {amazonTitle}
      </Button>
    );
  }

  handleAmazon() {
    transitionTo('product-amazon', {
      productId: this.entityId
    });
  }

  renderHead() {
    const { hasAmazon } = this.props;
    const buttons = [
      this.selectContextDropdown,
      this.cancelButton,
    ];

    if (hasAmazon) {
      buttons.unshift(this.amazonButton);
    }

    return buttons;
  }
}

export default connectPage('product', productActions)(ProductPage);
