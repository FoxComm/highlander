/**
 * @flow
 */

// libs
import React, { Component, Element, PropTypes } from 'react';
import { assoc } from 'sprout-data';
import { autobind } from 'core-decorators';
import _ from 'lodash';

// components
import { FormField } from '../forms';
import ContentBox from '../content-box/content-box';
import ObjectForm from '../object-form/object-form';
import ObjectFormInner from '../object-form/object-form-inner';
import ProductState from '../products/product-state';
import WaitAnimation from '../common/wait-animation';

// types
import type { FullSku } from '../../modules/skus/details';

type Props = {
  code: string,
  onChange: (sku: FullSku) => void,
  sku: ?FullSku,
};

const defaultKeys = {
  general: ['upc', 'description'],
  pricing: ['retailPrice', 'salePrice', 'unitCost'],
};

const keysToOmit = ['activeFrom', 'activeTo'];

export default class SkuDetails extends Component<void, Props, void> {
  static propTypes = {
    code: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired,
    sku: PropTypes.object,
  };

  get generalAttrs(): Array<string> {
    const toOmit = _.omit(defaultKeys, 'general');
    const toOmitArray = [
      ..._.reduce(toOmit, (res, arr) => ([...res, ...arr]), []),
      ...keysToOmit,
    ];
    const shadow = _.get(this.props, 'sku.shadow.attributes', []);
    return _(shadow).omit(toOmitArray).keys().value();
  }

  get generalContent(): Element {
    const sku = _.get(this.props, 'sku');
    const formAttributes = _.get(this.props, 'sku.form.attributes', []);
    const shadowAttributes = _.get(this.props, 'sku.shadow.attributes', []);

    return (
      <ContentBox title="General">
        <FormField
          className="fc-object-form__field"
          labelClassName="fc-object-form__field-label"
          label="SKU"
          key="object-form-attribute-sku">
          <input
            className="fc-object-form__field-value"
            type="text"
            name="sku"
            value={sku.form.code}
            onChange={_.noop} />
        </FormField>
        <ObjectFormInner
          canAddProperty={true}
          onChange={this.handleChange}
          fieldsToRender={this.generalAttrs}
          form={formAttributes}
          shadow={shadowAttributes} />
      </ContentBox>
    );
  }

  get productState(): Element {
    const formAttributes = _.get(this.props, 'sku.form.attributes', []);
    const shadowAttributes = _.get(this.props, 'sku.shadow.attributes', []);

    return (
      <ProductState
        form={formAttributes}
        shadow={shadowAttributes}
        onChange={this.handleChange}
        title="SKU" />
    );
  }

  @autobind
  handleChange(form: FormAttributes, shadow: ShadowAttributes) {
    const { sku } = this.props;
    if (sku) {
      const updatedSku = assoc(sku,
        ['form', 'attributes'], form,
        ['shadow', 'attributes'], shadow);
      this.props.onChange(updatedSku);
    }
  }

  render(): Element {
    const { sku } = this.props;
    if (!sku) {
      return <WaitAnimation />;
    }

    const formAttributes = _.get(sku, 'form.attributes', []);
    const shadowAttributes = _.get(sku, 'shadow.attributes', []);

    return (
      <div className="fc-product-details fc-grid fc-grid-no-gutter">
        <div className="fc-col-md-3-5">
          {this.generalContent}
          <ObjectForm
            canAddProperty={false}
            onChange={this.handleChange}
            fieldsToRender={defaultKeys.pricing}
            form={formAttributes}
            shadow={shadowAttributes}
            title="Pricing" />
        </div>
        <div className="fc-col-md-2-5">
          {this.productState}
        </div>
      </div>
    );
  }
}
