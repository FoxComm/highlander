
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
    items: this.addEmptyItemIfNeeded(this.props.initialItems),
  };

  addEmptyItemIfNeeded(initialItems) {
    return _.isEmpty(initialItems) ? {1: null} : initialItems;
  }

  componentWillReceiveProps(nextProps) {
    if (nextProps.initialItems && nextProps.initialItems !== this.props.initialItems) {
      this.setState({
        items: this.addEmptyItemIfNeeded(nextProps.initialItems),
      });
    }
  }

  get nextItemId() {
    return _.size(this.state.items) + 1;
  }

  @autobind
  onAddClick(e) {
    e.preventDefault();

    if (_.size(this.state.items) >= _.size(this.props.options)) {
      // nothing to add
      return;
    }
    this.setState({
      items: assoc(this.state.items, this.nextItemId, null)
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

  addMoreIcon(isLast) {
    if (isLast) {
      return <Button onClick={this.onAddClick} className='fc-vmultiselect-add icon-add'/>;
    } else {
      return <div className='fc-vmultiselect-or'>or</div>;
    }
  }

  @autobind
  renderSelect(key, index, arr) {
    const props = this.props;

    const isLast = index == arr.length - 1;
    const selectedValue = this.state.items[key];

    const availableValues = _.difference(_.keys(this.props.options), _.values(this.state.items));
    const items = availableValues.map(value => [value, props.options[value]]);

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
        {this.addMoreIcon(isLast)}
        <i onClick={_.partial(this.onClose, key)} className='fc-vmultiselect-close icon-close'/>
      </div>
    );
  }

  render() {
    const props = this.props;

    const className = classNames('fc-vmultiselect', props.className);

    return (
      <div className={className}>
        {_.keys(this.state.items).map(this.renderSelect)}
      </div>
    );
  }
}
