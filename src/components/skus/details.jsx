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
import ObjectScheduler from '../object-scheduler/object-scheduler';
import WaitAnimation from '../common/wait-animation';

// types
import type { Sku } from '../../modules/skus/details';

type Attribute = { t: string, v: any };
type Attributes = { [key:string]: Attribute };

type Props = {
  code: string,
  onChange: (sku: Sku) => void,
  sku: ?Sku,
};

const defaultKeys = {
  base: ['title'],
  general: ['upc', 'description'],
  pricing: ['retailPrice', 'salePrice', 'unitCost'],
};

const keysToOmit = ['activeFrom', 'activeTo', 'tags'];

export default class SkuDetails extends Component {
  props: Props;

  get generalAttrs(): Array<string> {
    const toOmitArray = [
      ..._.reduce(defaultKeys, (res, arr) => ([...res, ...arr]), []),
      ...keysToOmit,
    ];
    const shadow = _.get(this.props, 'sku.attributes', {});
    return [
      ...defaultKeys.base,
      ...defaultKeys.general,
      ...(_(shadow).omit(toOmitArray).keys().value())
    ];
  }

  get generalContent(): Element {
    const sku = _.get(this.props, 'sku');
    const attributes = _.get(this.props, 'sku.attributes', {});

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
            value={sku.code}
            onChange={_.noop} />
        </FormField>
        <ObjectFormInner
          canAddProperty={true}
          onChange={this.handleChange}
          fieldsToRender={this.generalAttrs}
          attributes={attributes} />
      </ContentBox>
    );
  }

  get skuState(): Element {
    const attributes = _.get(this.props, 'sku.attributes', {});

    return (
      <ObjectScheduler
        attributes={attributes}
        onChange={this.handleChange}
        title="SKU" />
    );
  }

  @autobind
  handleChange(attributes: Attributes) {
    const { sku } = this.props;
    if (sku) {
      const updatedSku = assoc(sku, 'attributes', attributes);
      this.props.onChange(updatedSku);
    }
  }

  render(): Element {
    const { sku } = this.props;
    if (!sku) {
      return <WaitAnimation />;
    }

    const attributes = _.get(sku, 'attributes', {});
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
            attributes={attributes}
            title="Pricing" />
        </div>
        <div className="fc-col-md-2-5">
          {this.skuState}
        </div>
      </div>
    );
  }
}
