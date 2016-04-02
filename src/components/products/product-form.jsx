/**
 * @flow
 */

// libs
import React, { Component, Element, PropTypes } from 'react';
import { assoc } from 'sprout-data';
import { autobind } from 'core-decorators';
import _ from 'lodash';

// components
import ContentBox from '../content-box/content-box';
import ObjectForm from '../object-form/object-form';
import ProductState from './product-state';
import SkuList from './sku-list';
import VariantList from './variant-list';
import WaitAnimation from '../common/wait-animation';

// types
import type { Attributes, ShadowAttributes } from '../../modules/products/details';
import type { FullProduct } from '../../modules/products/details';

type Props = {
  product: FullProduct,
  onUpdateProduct: (product: FullProduct) => void,
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
    onSetSkuProperty: PropTypes.func.isRequired,
    onUpdateProduct: PropTypes.func.isRequired,
  };

  state: State;

  constructor(props: Props) {
    super(props);
    this.state = {
      isAddingProperty: false,
    };
  }

  get generalAttrs(): Array<string> {
    const toOmit = [
      ...defaultKeys.misc,
      ...defaultKeys.seo,
      ..._.flatten(_.valuesIn(omitKeys)),
    ];
    const shadow = this.props.product.shadow.product.attributes;
    return _(shadow).omit(toOmit).keys().value();
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

  @autobind
  handleProductChange(form: Attributes, shadow: ShadowAttributes) {
    const newProduct = assoc(this.props.product,
      ['form', 'product', 'attributes'], form,
      ['shadow', 'product', 'attributes'], shadow
    );

    this.props.onUpdateProduct(newProduct);
  }

  @autobind
  handleAddProperty() {
    this.setState({ isAddingProperty: true });
  }

  get productState(): Element {
    return (
      <ProductState
        onSetActive={(x, y) => console.log(x, y)}
        product={this.props.product} />
    );
  }

  render(): Element {
    const formAttributes = this.props.product.form.product.attributes;
    const shadowAttributes = this.props.product.shadow.product.attributes;

    return (
      <div className="fc-product-details fc-grid fc-grid-no-gutter">
        <div className="fc-col-md-3-5">
          <ObjectForm
            canAddProperty={true}
            onChange={this.handleProductChange}
            fieldsToRender={this.generalAttrs}
            form={formAttributes}
            shadow={shadowAttributes}
            title="General" />
          {this.variantContentBox}
          {this.skusContentBox}
          <ObjectForm
            onChange={this.handleProductChange}
            fieldsToRender={defaultKeys.seo}
            form={formAttributes}
            shadow={shadowAttributes}
            title="SEO" />
        </div>
        <div className="fc-col-md-2-5">
          {this.productState}
        </div>
      </div>
    );
  }
}
