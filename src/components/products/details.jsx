/**
 * @flow
 */

// libs
import React, { Component, Element, PropTypes } from 'react';
import { autobind } from 'core-decorators';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import { dispatch } from 'redux';
import _ from 'lodash';

// actions
import * as DetailsActions from '../../modules/products/details';

// components
import { FormField } from '../forms';
import ContentBox from '../content-box/content-box';
import CurrencyInput from '../forms/currency-input';
import CustomProperty from './custom-property';
import InlineField from '../inline-field/inline-field';
import SkuList from './sku-list';
import VariantList from './variant-list';

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

type DetailsParams = {
  productId: number,
  product: FullProduct,
};

type Props = {
  details: ProductDetailsState,
  params: DetailsParams,
  productAddAttribute: (label: string, type: string) => void,

  onUpdateProduct: (key: string, value: string) => void,
  onUpdateSku: (code: string, key: string, value: string) => void,
  onUpdateVariant: (key: string, variant: Variant) => void,
  updatedProduct: { [key:string]: string },
};

type PriceAttribute = {
  currency: string,
  price: number,
};

type State = { isAddingProperty: boolean };

const reqGeneralKeys = ['title', 'description'];
const miscAttributeKeys = ['images'];
const pricingAttributeKeys = ['retailPrice', 'salePrice'];
const seoAttributeKeys = ['url', 'metaTitle', 'metaDescription'];

export class ProductDetails extends Component<void, Props, State> {
  state: State;

  constructor(...args: Array<any>) {
    super(...args);
    this.state = { isAddingProperty: false };
  }

  static propTypes = {
    details: PropTypes.shape({
      product: PropTypes.object,
    }),
  };

  get fullProduct(): ?FullProduct {
    return _.get(this.props, 'details.product');
  }

  get attributes(): IlluminatedAttributes {
    const fullProduct = this.fullProduct;
    return fullProduct ? getProductAttributes(fullProduct) : {};
  }

  get generalContentBox(): Element {
    const customKeys = [
      ...reqGeneralKeys,
      ...miscAttributeKeys,
      ...pricingAttributeKeys,
      ...seoAttributeKeys
    ];

    const { title, description } = this.attributes;
    const customAttrs = _.omit(this.attributes, customKeys);
    const generalAttrs = {
      title: title,
      description: description,
      ...customAttrs,
    };

    const attributes = this.renderAttributes(generalAttrs);
    return (
      <ContentBox title="General">
        {attributes}
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
    const attributes = pricingAttributeKeys.map(key => {
      return this.renderAttribute(this.attributes[key]);
    });

    return <ContentBox title="Pricing">{attributes}</ContentBox>;
  }

  get seoContentBox(): Element {
    const attributes = seoAttributeKeys.map(key => {
      return this.renderAttribute(this.attributes[key]);
    });
    return <ContentBox title="SEO">{attributes}</ContentBox>;
  }

  get skusContentBox(): Element {
    return (
      <ContentBox title="SKUs">
        <SkuList
          fullProduct={this.fullProduct}
          updateField={this.props.onUpdateSku} />
      </ContentBox>
    );
  }

  get variantContentBox(): Element {
    const variants = {
      color: {
        name: 'Color',
        type: 'color',
        values: {
          red: {
            id: 3452365363,
            swatch: "e8242b",
            image: null,
          },
          green: {
            id: 7432985798,
            swatch: "00ff00",
            image: null,
          },
        },
      },
      size: {
        name: 'Size',
        type: 'size',
        values: {
          'S/M': {
            id: 6734269823,
            swatch: null,
            image: null,
          },
          'L/XL': {
            id: 5423453263,
            swatch: null,
            image: null,
          },
        },
      },
    };

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

  renderAttributes(attributes: IlluminatedAttributes): Array<Element> {
    return _.map(attributes, attr => this.renderAttribute(attr));
  }

  renderAttribute(attribute: IlluminatedAttribute): Element {
    const { label, type, value } = attribute;
    const formattedLbl = _.snakeCase(label).split('_').reduce((res, val) => {
      return `${res} ${_.capitalize(val)}`;
    });

    return (
      <FormField
        className="fc-product-details__field"
        label={formattedLbl}
        labelClassName="fc-product-details__field-label"
        key={`product-page-field-${label}`}>
        {this.renderAttributeField(attribute)}
      </FormField>
    );
  }

  renderAttributeField(attribute: Attribute): Element {
    const { label, type, value } = attribute;
    const inputClass = 'fc-product-details__field-value';

    switch (type) {
      case 'price':
        const priceValue = _.get(this.props, ['updatedProduct', label], value.value);
        return (
          <CurrencyInput
            className={inputClass}
            inputName={label}
            value={priceValue}
            onChange={(value) => this.props.onUpdateProduct(label, value)} />
        );
      default:
        const val = _.get(this.props, ['updatedProduct', label], value);
        return (
          <input
            className={inputClass}
            type="text"
            name={label}
            value={val}
            onChange={({target}) => this.props.onUpdateProduct(label, target.value)} />
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
    }, () => this.props.productAddAttribute(property.fieldLabel, property.propertyType));
  }

  render() {
    return (
      <div className="fc-product-details fc-grid">
        <div className="fc-col-md-2-3">
          {this.generalContentBox}
          {this.pricingContentBox}
          {this.variantContentBox}
          {this.skusContentBox}
          {this.seoContentBox}
        </div>
        {this.customPropertyForm}
      </div>
    );
  }
}

type DetailsState = { details: ProductDetailsState };
function mapStateToProps(state: Object): DetailsState {
  return { details: state.products.details };
}

export default connect(mapStateToProps, DetailsActions)(ProductDetails);
