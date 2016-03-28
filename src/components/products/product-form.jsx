/**
 * @flow
 */

// libs
import React, { Component, Element, PropTypes } from 'react';
import { assoc } from 'sprout-data';
import { autobind } from 'core-decorators';
import _ from 'lodash';

// components
import { PageTitle } from '../section-title';
import { PrimaryButton } from '../common/buttons';
import { Form, FormField } from '../forms';
import { SliderCheckbox } from '../checkbox/checkbox';
import ContentBox from '../content-box/content-box';
import CurrencyInput from '../forms/currency-input';
import CustomProperty from './custom-property';
import DatePicker from '../datepicker/datepicker';
import ProductState from './product-state';
import RichTextEditor from '../rich-text-editor/rich-text-editor';
import SkuList from './sku-list';
import SubNav from './sub-nav';
import VariantList from './variant-list';
import WaitAnimation from '../common/wait-animation';

// helpers
import { getProductAttributes } from '../../paragons/product';

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
  product: FullProduct,
  onSetProperty: (field: string, type: string, value: any) => void,
  onSetSkuProperty: (code: string, field: string, type: string, value: any) => void,
};

type State = {
  isAddingProperty: bool,
};

const omitKeys = {
  general: ['skus', 'variants'],
};

const defaultKeys = {
  general: ['title', 'description'],
  misc: ['images'],
  seo: ['url', 'metaTitle', 'metaDescription'],
};

const requiredAttributes = ['title'];

/**
 * ProductForm is dumb component that implements the logic needed for creating
 * or updating a product.
 */
export default class ProductForm extends Component<void, Props, State> {
  static propTypes = {
    product: PropTypes.object.isRequired,
    onSetProperty: PropTypes.func.isRequired,
    onSetSkuProperty: PropTypes.func.isRequired,
  };

  state: State;

  constructor(props: Props) {
    super(props);
    this.state = {
      isAddingProperty: false,
    };
  }

  get generalContentBox(): Element {
    const customKeys: Array<string> = _.flatten(_.valuesIn(defaultKeys));
    const hideKeys: Array<string> = _.flatten(_.valuesIn(omitKeys));
    const attributes = getProductAttributes(this.props.product);
    const { title, description } = attributes;
    const customAttrs = _.omit(attributes, [...customKeys, ...hideKeys]);

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
          updateField={this.props.onSetSkuProperty} />
      </ContentBox>
    );
  }

  get variantContentBox(): Element {
    return <VariantList variants={{}} />;
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
    const { fieldLabel, propertyType } = property;
    this.setState({
      isAddingProperty: false
    }, () => this.props.onSetProperty(fieldLabel, propertyType, ''));
  }

  renderAttributes(attributes: IlluminatedAttributes): Array<Element> {
    return _.map(attributes, attr => this.renderAttribute(attr));
  }

  renderAttribute(attribute: IlluminatedAttribute): Element {
    if (!attribute) {
      console.log('wtf');
      return <div></div>;
    }
    const { label, type, value } = attribute;
    const formattedLbl = _.snakeCase(label).split('_').reduce((res, val) => {
      return `${res} ${_.capitalize(val)}`;
    });

    const required = _.indexOf(requiredAttributes, label) != -1;
    const inputClass = 'fc-product-details__field-value';

    switch (type) {
      case 'price':
        const priceValue = _.get(value, 'value', '');
        return (
          <FormField
            className="fc-product-details__field"
            label={formattedLbl}
            labelClassName="fc-product-details__field-label"
            key={`product-page-field-${label}`}>
            <CurrencyInput
              className={inputClass}
              inputName={label}
              value={priceValue}
              onChange={(value) => this.props.onSetProperty(label, type, value)} />
          </FormField>
        );
      case 'richText':
        return (
          <RichTextEditor
            label={formattedLbl}
            value={value}
            onChange={(value) => this.props.onSetProperty(label, type, value)} />
        );
      case 'date':
        return (
          <FormField
            className="fc-product-details__field"
            label={formattedLbl}
            labelClassName="fc-product-details__field-label"
            key={`product-page-field-${label}`}>
            <DatePicker
              date={new Date(value)}
              onChange={(value) => this.props.onSetProperty(label, type, value)} />
          </FormField>
        );
      case 'bool':
        return (
          <div className="fc-product-details_field">
            <div className="fc-product-details__field-label">{formattedLbl}</div>
            <SliderCheckbox
              checked={value}
              onChange={() => this.props.onSetProperty(label, type, !value)} />
          </div>
        );
      default:
        return (
          <FormField
            className="fc-product-details__field"
            label={formattedLbl}
            labelClassName="fc-product-details__field-label"
            key={`product-page-field-${label}`}>
            <input
              className={inputClass}
              type="text"
              name={label}
              value={value}
              required={required}
              onChange={({target}) => this.props.onSetProperty(label, type, target.value)} />
          </FormField>
        );
    }
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
      <div className="fc-product-details fc-grid">
        <div className="fc-col-md-3-5">
          {this.generalContentBox}
          {this.variantContentBox}
          {this.skusContentBox}
          {this.seoContentBox}
        </div>
        <div className="fc-col-md-2-5">
          {this.productState}
        </div>
        {this.customPropertyForm}
      </div>
    );
  }
}
