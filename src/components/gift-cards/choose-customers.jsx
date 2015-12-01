
import React, { PropTypes } from 'react';
import { assoc, dissoc } from 'sprout-data';
import { PrimaryButton } from '../common/buttons';
import TypeaheadItems from '../typeahead/items';
import { Checkbox } from '../checkbox/checkbox';

export default class ChooseCustomers extends React.Component {

  static propTypes = {
    items: PropTypes.array,
    updating: PropTypes.bool,
  };

  constructor(...args) {
    super(...args);
    this.state = {
      selectedCustomers: {}
    };
  }

  toggleCustomerSelected(customer) {
    const selectedCustomers = this.state.selectedCustomers;

    if (selectedCustomers[customer.id]) {
      this.setState({
        selectedCustomers: dissoc(selectedCustomers, customer.id)
      });
    } else {
      this.setState({
        selectedCustomers: assoc(selectedCustomers, customer.id, customer)
      });
    }
  }

  get chooseCustomers() {
    const props = this.props;

    return (
      <div className="fc-choose-customers">
        <ul className="fc-choose-customers__list">
          {props.items.map(customer => {
            return (
              <li className="fc-choose-customers__entry">
                <label>
                  <Checkbox
                    checked={this.state.selectedCustomers[customer.id]}
                    onChange={event => this.toggleCustomerSelected(customer)} />
                  <span className="fc-choose-customers__customer-name">
                    {customer.name}
                  </span>
                </label>
                <div className="fc-choose-customers__info">
                  <div className="fc-choose-customers__customer-email">
                    {customer.email}
                  </div>
                  <div className="fc-choose-customers__customer-phone-number">
                    {customer.phoneNumber}
                  </div>
                </div>
              </li>
            );
          })}
        </ul>
        <footer className="fc-choose-customers__footer">
          <PrimaryButton>Add Customers</PrimaryButton>
        </footer>
      </div>
    );
  }

  render() {
    if (this.props.items.length == 0) {
      return <TypeaheadItems {...this.props} />;
    } else {
      return this.chooseCustomers;
    }
  }
}
