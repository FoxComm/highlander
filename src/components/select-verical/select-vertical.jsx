
import _ from 'lodash';
import React, { PropTypes, Component } from 'react';
import { autobind } from 'core-decorators';
import classNames from 'classnames';
import { assoc, get, dissoc } from 'sprout-data';

// components
import Dropdown from '../dropdown/dropdown';
import { Button } from '../common/buttons';

export default class SelectVertical extends Component {

  static propTypes = {
    options: PropTypes.object.isRequired, // {value -> title}
    initialItems: PropTypes.object, // {counter -> value}
    onChange: PropTypes.func.isRequired,
    className: PropTypes.string,
    placeholder: PropTypes.string,
  };

  static defaultProps = {
    initialItems: {1: null},
  };

  state = {
    counter: 2,
    items: this.initialItems,
  };

  get initialItems() {
    return _.isEmpty(this.props.initialItems) ? {1: null} : this.props.initialItems;
  }

  componentWillReceiveProps(nextProps) {
    if (nextProps.initialItems && nextProps.initialItems !== this.props.initialItems) {
      this.setState({
        items: nextProps.initialItems,
      });
    }
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
                    placeholder={props.placeholder}
                    onChange={_.partial(this.onChangeItem, key)}
                    className='fc-vmultiselect-item'/>
          {AddOrOr(isLast)}
          <i onClick={_.partial(this.onClose, key)} className='fc-vmultiselect-close icon-close'/>
        </div>
      );
    }

    const className = classNames('fc-vmultiselect', props.className);

    return (
      <div className={className}>
        {_.keys(this.state.items).map(renderSelect.bind(this))}
      </div>
    );
  }
}
