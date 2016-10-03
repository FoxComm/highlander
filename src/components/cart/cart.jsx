/* @flow weak */

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import classNames from 'classnames';
import { connect } from 'react-redux';
import { browserHistory } from 'react-router';
import { autobind } from 'core-decorators';

// localization
import localized from 'lib/i18n';

// components
import Currency from 'ui/currency';
import LineItem from './line-item';
import Button from 'ui/buttons';
import Icon from 'ui/icon';
import ErrorAlerts from 'wings/lib/ui/alerts/error-alerts';
import { parseError } from 'api-js';

// styles
import styles from './cart.css';

// actions
import * as actions from 'modules/cart';

const mapStateToProps = state => ({ ...state.cart, ...state.auth });

type Props = {
  fetch: Function,
  deleteLineItem: Function,
  updateLineItemQuantity: Function,
  toggleCart: Function,
  skus: Array<any>,
  totals: Object,
  user: Object,
  isVisible: boolean,
  t: any,
};

type State = {
  errors?: Array<any>,
};

class Cart extends Component {
  props: Props;

  state: State = {

  };

  componentDidMount() {
    /** prevent loading if no user logged in */
    if (this.props.user) {
      this.props.fetch();
    }
  }

  @autobind
  deleteLineItem(id) {
    this.props.deleteLineItem(id).catch(ex => {
      this.setState({
        errors: parseError(ex),
      });
    });
  }

  @autobind
  updateLineItemQuantity(id, quantity) {
    this.props.updateLineItemQuantity(id, quantity).catch(ex => {
      this.setState({
        errors: parseError(ex),
      });
    });
  }

  get lineItems() {
    return _.map(this.props.skus, sku => {
      return (
        <LineItem
          {...sku}
          deleteLineItem={this.deleteLineItem}
          updateLineItemQuantity={this.updateLineItemQuantity}
          key={sku.sku}
        />
      );
    });
  }

  @autobind
  closeError(error, index) {
    const { errors } = this.state;

    if (!errors) return;

    errors.splice(index, 1);

    this.setState({
      errors,
    });
  }

  get errorsLine() {
    if (this.state.errors) {
      return <ErrorAlerts errors={this.state.errors} closeAction={this.closeError} />;
    }
  }

  @autobind
  onCheckout() {
    if (!this.props.user) {
      browserHistory.push({
        pathname: document.location.pathname,
        query: { auth: 'login' },
      });

      return;
    }
    browserHistory.push('/checkout');
  }

  render() {
    const { props } = this;
    const { t } = props;
    const cartClass = classNames({
      'cart-hidden': !props.isVisible,
      'cart-shown': props.isVisible,
    });

    const checkoutDisabled = _.size(props.skus) < 1;

    return (
      <div styleName={cartClass}>
        <div styleName="overlay" onClick={props.toggleCart}>
        </div>
        <div styleName="cart-box">
          <div styleName="cart-header" onClick={props.toggleCart}>
            <Icon name="fc-chevron-left" styleName="back-icon"/>
            <div styleName="header-text">
              {t('KEEP SHOPPING')}
            </div>
          </div>
          <div styleName="cart-content">
            <div styleName="line-items">
              {this.lineItems}
            </div>
            <div styleName="cart-subtotal">
              <div styleName="subtotal-title">
                {t('SUBTOTAL')}
              </div>
              <div styleName="subtotal-price">
                <Currency value={props.totals.subTotal} />
              </div>
            </div>
            {this.errorsLine}
          </div>
          <div styleName="cart-footer">
            <Button onClick={this.onCheckout} disabled={checkoutDisabled} styleName="checkout-button">
              {t('CHECKOUT')}
            </Button>
          </div>
        </div>
      </div>
    );
  }
}

export default connect(mapStateToProps, actions)(localized(Cart));
