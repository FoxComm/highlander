// @flow weak

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import localized from 'lib/i18n';
import { connect } from 'react-redux';
import { browserHistory } from 'lib/history';

// components
import { Link } from 'react-router';
import EditableBlock from 'ui/editable-block';
import RadioButton from 'ui/radiobutton/radiobutton';
import { AddressDetails } from 'ui/address';
import Block from '../common/block';

// styles
import addressStyles from 'pages/checkout/shipping/address-list.css';
import profileStyles from '../profile.css';

import type { Address } from 'types/address';

const styles = {...addressStyles, ...profileStyles};

import * as checkoutActions from 'modules/checkout';

type Props = {
  fetchAddresses: () => Promise,
  addresses: Array<Address>,
  deleteAddress: (id: number) => Promise,
  restoreAddress: (id: number) => Promise,
  setAddressAsDefault: (id: number) => Promise,
  t: any,
};

function mapStateToProps(state) {
  return {
    addresses: state.checkout.addresses,
  };
}

class MyShippingAddresses extends Component {
  props: Props;

  componentWillMount() {
    this.props.fetchAddresses();
  }

  @autobind
  addAddress() {
    browserHistory.push('/profile/addresses/new');
  }

  @autobind
  deleteAddress(address) {
    this.props.deleteAddress(address.id);
  }

  @autobind
  handleSelectAddress(address) {
    this.props.setAddressAsDefault(address.id);
  }

  renderAddresses() {
    const { props } = this;
    const items = _.map(props.addresses, (address: Address) => {
      const contentAttrs = address.isDeleted ? {className: styles['deleted-content']} : {};
      const content = <AddressDetails address={address} hideName {...contentAttrs} />;
      const checked = address.isDefault;
      const key = address.id;

      let actionsContent;
      let title;

      if (address.isDeleted) {
        actionsContent = (
          <div styleName="actions-block">
            <div styleName="link" onClick={() => this.props.restoreAddress(address.id)}>{props.t('RESTORE')}</div>
          </div>
        );
        title = <span styleName="deleted-content">{address.name}</span>;
      } else {
        actionsContent = (
          <div styleName="actions-block">
            <Link styleName="link" to={`/profile/addresses/${address.id}`}>{props.t('EDIT')}</Link>
            &nbsp;|&nbsp;
            <div styleName="link" onClick={() => this.deleteAddress(address)}>
              {props.t('REMOVE')}
            </div>
          </div>
        );
        title = address.name;
      }

      return (
        <li styleName="item" key={`address-radio-${key}`}>
          <RadioButton
            id={`address-radio-${key}`}
            name={`address-radio-${key}`}
            checked={checked}
            disabled={address.isDeleted}
            onChange={() => this.handleSelectAddress(address)}
          >
            <EditableBlock
              styleName="item-content"
              title={title}
              content={content}
              actionsContent={actionsContent}
            />
          </RadioButton>
        </li>
      );
    });

    return (
      <div>
        <ul styleName="list">{items}</ul>
        <div styleName="buttons-footer">
          <button styleName="link-button" type="button" onClick={this.addAddress}>
            Add Address
          </button>
        </div>
      </div>
    );
  }

  render() {
    return (
      <Block title="My Shipping Addresses">
        {this.renderAddresses()}
      </Block>
    );
  }
}

export default _.flowRight(
  connect(mapStateToProps, {...checkoutActions}),
  localized
)(MyShippingAddresses);
