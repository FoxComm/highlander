import React, { PropTypes } from 'react';
import AddressDetails from './address-details';
import { Button, AddButton } from '../common/buttons';
import classnames from 'classnames';
import { autobind } from 'core-decorators';

export default class AddressSelect extends React.Component {

  static propTypes = {
    customerId: PropTypes.number.isRequired,
    name: PropTypes.string,
    initialValue: PropTypes.number,
    items: PropTypes.array,
    onItemSelect: PropTypes.func,
    className: PropTypes.string
  };

  static defaultProps = {
    initialValue: 0,
  };

  state = {
    value: this.props.initialValue,
  };

  @autobind
  onItemSelect(value) {
    this.setState({
      value: value
    }, () => {
      if (this.props.onItemSelect) {
        this.props.onItemSelect(value);
      }
    });
  }

  @autobind
  renderSelectItem(address) {
    const isSelected = address.id === this.state.value;
    const itemClassName = classnames(
      'fc-address-select-item',
      { 'is-address-select-item-active': isSelected }
    );
    const key = `cutomer-address-${ address.id }`;
    return (
      <div className={ itemClassName }
           key={ key }>
        <div className="fc-address-select-item-info">
          <AddressDetails address={ address }
                          customerId={ this.props.customerId }/>
        </div>
        <div className="fc-address-select-item-controlls">
          <Button type="button" onClick={ () => this.onItemSelect(address.id) } >Choose</Button>
        </div>
      </div>
    );
  }

  render() {
    const rootClassName = classnames('fc-address-select', this.props.className);
    return (
      <div className={ rootClassName }>
        <div className="fc-address-select-header">
          <div className="fc-left">
            Address Book
          </div>
          <div className="fc-right">
            <AddButton />
          </div>
        </div>
        <div className="fc-address-select-body">
          <input type="hidden" name={ this.props.name } value={ this.state.value } readOnly/>
          <div className="fc-address-select-list">
            {(this.props.items && this.props.items.map( this.renderSelectItem ))}
          </div>
        </div>
      </div>
    );
  }

}
