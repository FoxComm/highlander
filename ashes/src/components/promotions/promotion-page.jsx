/* @flow weak */

// libs
import _ from 'lodash';
import React, { Element } from 'react';
import { autobind } from 'core-decorators';
import { assoc } from 'sprout-data';


// components
import SubNav from './sub-nav';
import { connectPage, ObjectPage } from '../object-page/object-page';
import { transitionTo } from 'browserHistory';
import qualifiers from './qualifiers';
import offers from './offers';

// actions
import * as PromotionActions from 'modules/promotions/details';

class PromotionPage extends ObjectPage {

  state = {
    object: this.props.originalObject,
    schema: this.props.schema,
    justSaved: false,
    clientSideErrors: {
      qualifierErrors: {},
      offerErrors: {},
    },
  };

  getDiscountChildErrors(discountChild, discountChildTypes, object) {
    const discountChildValue = _.get(object, `discounts[0].attributes.${discountChild}.v`, {});
    const key = _.keys(discountChildValue)[0];
    const errors = {};
    _.forEach(discountChildValue[key], (v,k) => {
      const { validate } = _.find(discountChildTypes, (dc) => dc.type == key);
      const validator = _.get(validate, `${k}.validate`, (v) => true);
      if (!validator(v)) {
        if (_.isEmpty(errors)) { errors[key] = {} }
        errors[key][k] = validate[k].error;
      };
    });
    return errors;
  }

  setClientSideErrors(errors) {
    this.setState({
      clientSideErrors: errors,
    });
  }

  @autobind
  updateClientSideErrors(errorsRoute, params, discountChildTypes) {
    const { discountType, key, value } = params;
    const { validate } = _.find(discountChildTypes, (dc) => dc.type == discountType);
    const validator = _.get(validate, `${key}.validate`, (v) => true);
    const { clientSideErrors } = this.state;
    const existingError = _.get(clientSideErrors, `${errorsRoute}.${discountType}`, {});
    if (!validator(value)) {
      const newError = {
        ...existingError,
        [key]: validate[key].error,
      };
      const newClientSideErrors = assoc(clientSideErrors, [errorsRoute, discountType], newError);
      this.setClientSideErrors(newClientSideErrors);
      return;
    }
    if (!_.isEmpty(existingError)) {
      const newError = _.omit(existingError, [key]);
      const newClientSideErrors = assoc(clientSideErrors, [errorsRoute, discountType], newError);
      this.setClientSideErrors(newClientSideErrors);
    }
  }

  @autobind
  getCombinedErrors(object = this.state.object) {
    const errors = {
      qualifierErrors: this.getDiscountChildErrors('qualifier', qualifiers, object),
      offerErrors: this.getDiscountChildErrors('offer', offers, object),
    };
    return errors;
  }

  save(): ?Promise<*> {
    const errors = this.getCombinedErrors();
    this.setClientSideErrors(errors);
    if (!_.isEmpty(errors.qualifierErrors) || !_.isEmpty(errors.offerErrors)) {
      this.validateForm();
      this.validate();
      return;
    };
    let isNew = this.isNew;
    let willBePromo = super.save();

    if (willBePromo && isNew) {
      willBePromo.then((data) => {
        if (data.applyType === 'coupon') {
          transitionTo('promotion-coupon-new',{promotionId: data.id});
        }
      });
    }

    return willBePromo;
  }

  childrenProps() {
    const clientSideErrors = _.get(this.state, 'clientSideErrors', {
      qualifierErrors: {},
      offerErrors: {},
    });
    return {
      ...super.childrenProps(),
      clientSideErrors,
      updateClientSideErrors: this.updateClientSideErrors,
    };
  }

  subNav(): Element<*> {
    return <SubNav applyType={_.get(this.props, 'details.promotion.applyType')} promotionId={this.entityId} />;
  }
}

export default connectPage('promotion', PromotionActions)(PromotionPage);
