import React, { PropTypes } from 'react';
import _ from 'lodash';
import { autobind } from 'core-decorators';
import { assoc, get, dissoc } from 'sprout-data';
// components
import Dropdown from '../dropdown/dropdown';
import { Button } from '../common/buttons';

export default class SelectVertical extends React.Component {

  static propTypes = {
    options: PropTypes.object.isRequired, // {value -> title}
    onChange: PropTypes.func.isRequired,
  };

  constructor(...args) {
    super(...args);
    this.state = {
      counter: 2,
      items: {1: null}
    };
  }

  @autobind
  onAddClick(e) {
    e.preventDefault();

    if (_.size(this.state.items) == _.size(this.props.options)) {
      // nothing to add
      return;
    }
    this.setState({
      counter: this.state.counter + 1,
      items: assoc(this.state.items, this.state.counter, null)
    });
  }

  onUpdate(newItems) {
    const newValues = _.filter(_.values(newItems));
    this.props.onChange(newValues);
  }

  @autobind
  onChangeItem(key, newVal) {
    const newItems = assoc(this.state.items, key, newVal);
    this.setState({
      items: newItems
    });
    this.onUpdate(newItems);
  }

  @autobind
  onClose(key) {
    let newItems;
    if (_.size(this.state.items) == 1) {
      newItems = assoc(this.state.items, key, null);
    } else {
      newItems = dissoc(this.state.items, key);
    }
    this.setState({
      items: newItems
    });
    this.onUpdate(newItems);
  }

  render() {
    const props = this.props;

    const AddOrOr = ((isLast) => {
      if (isLast) {
        return <Button onClick={this.onAddClick} className='fc-vmultiselect-add icon-add'/>;
      } else {
        return <div className='fc-vmultiselect-or'>or</div>;
      }
    }).bind(this);

    const availableValues = _.difference(_.keys(this.props.options), _.values(this.state.items));
    const items = availableValues.map(value => [value, props.options[value]]);

    function renderSelect(key, index, arr) {
      const isLast = index == arr.length - 1;
      const selectedValue = this.state.items[key];

      let curItems = items;
      if (selectedValue) {
        curItems = [...items, [selectedValue, props.options[selectedValue]]];
      }

      return (
        <div className='fc-vmultiselect-cont' key={key}>
          <Dropdown items={curItems}
                    value={selectedValue}
                    onChange={_.partial(this.onChangeItem, key)}
                    className='fc-vmultiselect-item'/>
          {AddOrOr(isLast)}
          <i onClick={_.partial(this.onClose, key)} className='fc-vmultiselect-close icon-close'/>
        </div>
      );
    }

    return (
      <div className='fc-grid fc-vmultiselect'>
        {_.keys(this.state.items).map(renderSelect.bind(this))}
      </div>
    );
  }
}
