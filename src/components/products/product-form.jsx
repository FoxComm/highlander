/**
 * @flow
 */

// libs
import React, { Component, Element } from 'react';
import { assoc } from 'sprout-data';
import { autobind } from 'core-decorators';
import _ from 'lodash';

// components
import { PageTitle } from '../section-title';
import { PrimaryButton } from '../common/buttons';
import { Form, FormField } from '../forms';
import ContentBox from '../content-box/content-box';
import CurrencyInput from '../forms/currency-input';
import CustomProperty from './custom-property';
import ProductState from './product-state';
import SkuList from './sku-list';
import SubNav from './sub-nav';
import VariantList from './variant-list';
import WaitAnimation from '../common/wait-animation';

// helpers
import {
  addProductAttribute,
  getProductAttributes,
  setProductAttribute,
} from '../../paragons/product';

// types
import type {
  FullProduct,
  Attribute,
  Attributes,
  ProductDetailsState,
  Variant,
} from '../../modules/products/details';

import type {
  IlluminatedAttribute,
  IlluminatedAttributes,
  IlluminatedSku,
} from '../../paragons/product';

type Props = {
  isUpdating: bool,
  product: FullProduct,
  productId: string,
  title: string,
  onAddAttribute: (field: string, type: string) => void,
  onSubmit: (product: FullProduct) => void,
};

type State = {
  isAddingProperty: bool,
  product: { [key:string]: string },
};

const defaultKeys = {
  general: ['title', 'description'],
  misc: ['images'],
  pricing: ['retailPrice', 'salePrice'],
  seo: ['url', 'metaTitle', 'metaDescription'],
};

const requiredAttributes = ['title'];

/**
 * ProductForm is dumb component that implements the logic needed for creating
 * or updating a product.
 */
export default class ProductForm extends Component<void, Props, State> {
  state: State;

  constructor(props: Props) {
    super(props);
    this.state = {
      isAddingProperty: false,
      product: {},
    };
  }

  get generalContentBox(): Element {
    const customKeys: Array<string> = _.flatten(_.valuesIn(defaultKeys));
    const attributes = getProductAttributes(this.props.product);
    const { title, description } = attributes;
    const customAttrs = _.omit(attributes, customKeys);
    const generalAttrs = { title, description, ...customAttrs };

    const renderedAttributes = _.map(generalAttrs, attr => this.renderAttribute(attr));

    return (
      <ContentBox title="General">
        {renderedAttributes}
        <div className="fc-product-details__add-custom-property">
          Custom Property
          <a className="fc-product-details__add-custom-property-icon"
             onClick={this.handleAddProperty}>
            <i className="icon-add" />
          </a>
        </div>
      </ContentBox>
    );
  }

  get pricingContentBox(): Element {
    const attributes = getProductAttributes(this.props.product);
    const pricingAttributes = defaultKeys.pricing.map(key => {
      return this.renderAttribute(attributes[key]);
    });

    return <ContentBox title="Pricing">{pricingAttributes}</ContentBox>;
  }

  get seoContentBox(): Element {
    const attributes = getProductAttributes(this.props.product);
    const seoAttributes = defaultKeys.seo.map(key => {
      return this.renderAttribute(attributes[key]);
    });
    return <ContentBox title="SEO">{seoAttributes}</ContentBox>;
  }

  get skusContentBox(): Element {
    return (
      <ContentBox title="SKUs">
        <SkuList
          fullProduct={this.props.product}
          updateField={this.handleUpdateSku} />
      </ContentBox>
    );
  }

  get variantContentBox(): Element {
    const variants = _.get(this.props, 'product.form.product.variants.default', {});
    return <VariantList variants={variants} />;
  }

  get customPropertyForm(): ?Element {
    if (this.state.isAddingProperty) {
      return (
        <CustomProperty
          isVisible={true}
          onSave={this.handleCreateProperty}
          onCancel={() => this.setState({ isAddingProperty: false })} />
      );
    }
  }

  @autobind
  handleAddProperty() {
    this.setState({ isAddingProperty: true });
  }

  @autobind
  handleCreateProperty(property: { fieldLabel: string, propertyType: string }) {
    this.setState({
      isAddingProperty: false,
    }, () => this.props.onAddAttribute(property.fieldLabel, property.propertyType));
  }

  @autobind
  handleSubmit() {
    const product = _.reduce(this.state.product, (res, val, key) => {
      return setProductAttribute(res, key, val);
    }, this.props.product);

    this.props.onSubmit(product);
  }

  @autobind
  handleUpdateProduct(key: string, value: string) {
    this.setState(assoc(this.state, ['product', key], value));
  }

  @autobind
  handleUpdateSku(code: string, key: string, value: string) {
    const updateValue = {
      code: code,
      label: key,
      value: value,
    };

    const newState = assoc(this.state, ['product', 'skus'], updateValue);
    this.setState(newState);
  }

  renderAttributes(attributes: IlluminatedAttributes): Array<Element> {
    return _.map(attributes, attr => this.renderAttribute(attr));
  }

  renderAttribute(attribute: IlluminatedAttribute): Element {
    const { label, type, value } = attribute;
    const formattedLbl = _.snakeCase(label).split('_').reduce((res, val) => {
      return `${res} ${_.capitalize(val)}`;
    });

    const required = _.indexOf(requiredAttributes, label) != -1;

    return (
      <FormField
        className="fc-product-details__field"
        label={formattedLbl}
        labelClassName="fc-product-details__field-label"
        key={`product-page-field-${label}`}>
        {this.renderAttributeField(attribute, required)}
      </FormField>
    );
  }

  renderAttributeField(attribute: Attribute, required: bool): Element {
    const { label, type, value } = attribute;
    const inputClass = 'fc-product-details__field-value';

    switch (type) {
      case 'price':
        const priceValue = _.get(this.state, ['product', label], value.value);
        return (
          <CurrencyInput
            className={inputClass}
            inputName={label}
            value={priceValue}
            onChange={(value) => this.handleUpdateProduct(label, value)} />
        );
      default:
        const val = _.get(this.state, ['product', label], value);
        return (
          <input
            className={inputClass}
            type="text"
            name={label}
            value={val}
            required={required}
            onChange={({target}) => this.handleUpdateProduct(label, target.value)} />
        );
    }
  }

  get saveButton(): Element {
    const disabled = this.props.isUpdating;
    const wait = disabled ? <WaitAnimation /> : null;

    return (
      <PrimaryButton 
        className="fc-product-details__save-button" 
        type="submit" 
        disabled={disabled}>
        Save Draft {wait}
      </PrimaryButton>
    );
  }

  get productState(): Element {
    return (
      <ProductState 
        onSetActive={(x, y) => console.log(x, y)}
        product={this.props.product} />
    );
  }

  render(): Element {
    return (
      <Form onSubmit={this.handleSubmit}>
        <PageTitle title={this.props.title}>
          {this.saveButton}
        </PageTitle>
        <div>
          <SubNav productId={this.props.productId} product={this.props.product} />
          <div className="fc-product-details fc-grid">
            <div className="fc-col-md-3-5">
              {this.generalContentBox}
              {this.pricingContentBox}
              {this.variantContentBox}
              {this.skusContentBox}
              {this.seoContentBox}
            </div>
            <div className="fc-col-md-2-5">
              {this.productState}
            </div>
            {this.customPropertyForm}
          </div>
        </div>
      </Form>
    );
  }
}
