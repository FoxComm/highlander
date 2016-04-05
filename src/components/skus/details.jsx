/**
 * @flow
 */

// libs
import React, { Component, Element, PropTypes } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import _ from 'lodash';

// components
import ObjectForm from '../object-form/object-form';
import WaitAnimation from '../common/wait-animation';

// actions
import * as SkuActions from '../../modules/skus/details';

// types
import type { FullSku, SkuState } from '../../modules/skus/details';

type Props = {
  actions: {
    fetchSku: (code: string, context?: string) => void,
  },
  code: string,
  skus: SkuState,
};

const defaultKeys = {
  general: ['sku', 'upc', 'description'],
  pricing: ['retailPrice', 'salePrice', 'unitCost'],
};

export class SkuDetails extends Component<void, Props, void> {
  static propTypes = {
    actions: PropTypes.shape({
      fetchSku: PropTypes.func.isRequired,
    }).isRequired,
    code: PropTypes.string.isRequired,
    skus: PropTypes.object,
  };
  
  componentDidMount() {
    this.props.actions.fetchSku(this.props.code);
  }

  get generalAttrs(): Array<string> {
    const toOmit = _.omit(defaultKeys, 'general');
    const toOmitArray = _.reduce(toOmit, (res, arr) => ([...res, ...arr]), []);
    const shadow = _.get(this.props, 'skus.sku.shadow.attributes', []);
    return _(shadow).omit(toOmitArray).keys().value();
  }

  render(): Element {
    const sku = this.props.skus.sku;
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
            onChange={_.noop}
            fieldsToRender={this.generalAttrs}
            form={formAttributes}
            shadow={shadowAttributes}
            title="General" />    
          <ObjectForm
            canAddProperty={false}
            onChange={_.noop}
            fieldsToRender={defaultKeys.pricing}
            form={formAttributes}
            shadow={shadowAttributes}
            title="Pricing" />    
        </div>
      </div>
    );
  }
}

export default connect(
  state => ({ skus: state.skus.details }),
  dispatch => ({ actions: bindActionCreators(SkuActions, dispatch) })
)(SkuDetails);
