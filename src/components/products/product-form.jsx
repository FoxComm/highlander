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
import ObjectScheduler from '../object-scheduler/object-scheduler';
import SkuList from './sku-list';
import Tags from '../tags/tags';
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
  general: ['skus', 'variants', 'activeFrom', 'activeTo', 'tags'],
};

const defaultKeys = {
  general: ['title', 'description'],
  misc: ['images'],
  seo: ['url', 'metaTitle', 'metaDescription'],
};

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
      ...defaultKeys.general,
      ...defaultKeys.misc,
      ...defaultKeys.seo,
      ..._.flatten(_.valuesIn(omitKeys)),
    ];
    const shadow = _.get(this.props, 'product.shadow.product.attributes', {});
    return [
      ...defaultKeys.general,
      ...(_(shadow).omit(toOmit).keys().value())
    ];
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
    const formAttributes = _.get(this.props, 'product.form.product.attributes', []);
    const shadowAttributes = _.get(this.props, 'product.shadow.product.attributes', []);

    return (
      <ObjectScheduler
        form={formAttributes}
        shadow={shadowAttributes}
        onChange={this.handleProductChange}
        title="Product" />
    );
  }

  render(): Element {
    const formAttributes = _.get(this.props, 'product.form.product.attributes', []);
    const shadowAttributes = _.get(this.props, 'product.shadow.product.attributes', []);

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
          <Tags
            form={formAttributes}
            shadow={shadowAttributes}
            onChange={this.handleProductChange} />
          {this.productState}
        </div>
      </div>
    );
  }
}
