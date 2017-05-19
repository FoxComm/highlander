/* @flow */

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import localized from 'lib/i18n';
import { connect } from 'react-redux';
import { browserHistory } from 'lib/history';

// actions
import * as checkoutActions from 'modules/checkout';
import * as actions from 'modules/profile';

// components
import { AddressDetails } from 'ui/address';
import DetailsBlock from '../details-block';
import AddressList from 'ui/address/address-list';

// styles
// import addressStyles from 'ui/address-list.css';

import type { Address } from 'types/address';

import styles from '../profile.css';

type Props = {
  fetchAddresses: () => Promise<*>,
  addresses: Array<Address>,
  deleteAddress: (id: number) => Promise<*>,
  restoreAddress: (id: number) => Promise<*>,
  setAddressAsDefault: (id: number) => Promise<*>,
  cleanDeletedAddresses: () => void,
  t: any,
  className: string,
};

class AddressBlock extends Component {
  props: Props;

  componentWillMount() {
    this.props.fetchAddresses();
  }

  componentWillUnmount() {
    this.props.cleanDeletedAddresses();
  }

  // @autobind
  // addAddress() {
  //   browserHistory.push('/profile/addresses/new');
  // }
  //
  // @autobind
  // deleteAddress(address) {
  //   this.props.deleteAddress(address.id);
  // }
  //
  // @autobind
  // handleSelectAddress(address) {
  //   this.props.setAddressAsDefault(address.id);
  // }
  //
  // renderAddresses() {
  //   const { props } = this;
  //   const seenAddressNames = {};
  //   const items = _.map(props.addresses, (address: Address) => {
  //     const contentAttrs = address.isDeleted ? {className: styles['deleted-content']} : {};
  //     const content = <AddressDetails address={address} hideName {...contentAttrs} />;
  //     const checked = address.isDefault;
  //     const key = address.name in seenAddressNames ? address.id : address.name;
  //     seenAddressNames[address.name] = 1;
  //
  //     let actionsContent;
  //     let title;
  //
  //     if (address.isDeleted) {
  //       actionsContent = (
  //         <div styleName="actions-block">
  //           <div styleName="link" onClick={() => this.props.restoreAddress(address.id)}>{props.t('RESTORE')}</div>
  //         </div>
  //       );
  //       title = <span styleName="deleted-content">{address.name}</span>;
  //     } else {
  //       actionsContent = (
  //         <div styleName="actions-block">
  //           <Link styleName="link" to={`/profile/addresses/${address.id}`}>{props.t('EDIT')}</Link>
  //           &nbsp;|&nbsp;
  //           <div styleName="link" onClick={() => this.deleteAddress(address)}>
  //             {props.t('REMOVE')}
  //           </div>
  //         </div>
  //       );
  //       title = address.name;
  //     }
  //
  //     return (
  //       <li styleName="list-item" key={`address-radio-${key}`}>
  //         <RadioButton
  //           id={`address-radio-${key}`}
  //           name={`address-radio-${key}`}
  //           checked={checked}
  //           disabled={address.isDeleted}
  //           onChange={() => this.handleSelectAddress(address)}
  //           styleName="shipping-row"
  //         >
  //           <EditableBlock
  //             styleName="item-content"
  //             title={title}
  //             content={content}
  //             editAllowed={false}
  //           />
  //         </RadioButton>
  //         {actionsContent}
  //       </li>
  //     );
  //   });
  //
  //   return (
  //     <div>
  //       <ul styleName="list">{items}</ul>
  //       <div styleName="buttons-footer">
  //         <button styleName="link-button" type="button" onClick={this.addAddress}>
  //           Add Address
  //         </button>
  //       </div>
  //     </div>
  //   );
  // }

  get defaultAddress() {
    const { addresses } = this.props;
    return _.filter(addresses, (address) => address.isDefault);
  }

  get addressDetails() {
    const defaultAddress = this.defaultAddress[0];

    if (_.isEmpty(defaultAddress)) return 'No default address found.';

    return (
      <AddressDetails
        address={defaultAddress}
        styleName="shippingAddress"
      />
    );
  }

  get addressesModalContent() {
    return (
      <AddressList {...this.props}  />
    );
  }

  render() {
    const { className, toggleAddressesModal, addressesModalVisible } = this.props;

    return (
      <div className={className}>
        <DetailsBlock
          data={this.addressDetails}
          toggleModal={toggleAddressesModal}
          modalVisible={addressesModalVisible}
          actionTitle="Edit"
          modalContent={this.addressesModalContent}
          blockTitle="Shipping addresses"
        />
      </div>
    );
  }
}

const mapStateToProps = (state) => {
  return {
    addresses: _.get(state.checkout, 'addresses', []),
    addressesModalVisible: _.get(state.profile, 'addressesModalVisible', false),
  };
}

export default connect(mapStateToProps, {
  ...checkoutActions,
  ...actions,
})(localized(AddressBlock))
