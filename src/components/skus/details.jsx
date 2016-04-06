/**
 * @flow
 */

// libs
import React, { Component, Element, PropTypes } from 'react';
import { assoc } from 'sprout-data';
import { autobind } from 'core-decorators';
import _ from 'lodash';

// components
import ObjectForm from '../object-form/object-form';
import WaitAnimation from '../common/wait-animation';

// types
import type { FullSku } from '../../modules/skus/details';

type Props = {
  code: string,
  onChange: (sku: FullSku) => void,
  sku: ?FullSku,
};

const defaultKeys = {
  general: ['sku', 'upc', 'description'],
  pricing: ['retailPrice', 'salePrice', 'unitCost'],
};

export default class SkuDetails extends Component<void, Props, void> {
  static propTypes = {
    code: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired,
    sku: PropTypes.object,
  };
  
  get generalAttrs(): Array<string> {
    const toOmit = _.omit(defaultKeys, 'general');
    const toOmitArray = _.reduce(toOmit, (res, arr) => ([...res, ...arr]), []);
    const shadow = _.get(this.props, 'sku.shadow.attributes', []);
    return _(shadow).omit(toOmitArray).keys().value();
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
          <ObjectForm
            canAddProperty={true}
            onChange={this.handleChange}
            fieldsToRender={this.generalAttrs}
            form={formAttributes}
            shadow={shadowAttributes}
            title="General" />    
          <ObjectForm
            canAddProperty={false}
            onChange={this.handleChange}
            fieldsToRender={defaultKeys.pricing}
            form={formAttributes}
            shadow={shadowAttributes}
            title="Pricing" />    
        </div>
      </div>
    );
  }
}
