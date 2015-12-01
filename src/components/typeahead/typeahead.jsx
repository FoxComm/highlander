import React, { PropTypes } from 'react';
import Api from '../../lib/api';
import TypeaheadItems from './items';
import { FormField } from '../forms';
import classNames from 'classnames';
import { debounce, autobind } from 'core-decorators';

export default class Typeahead extends React.Component {

  static propTypes = {
    onItemSelected: PropTypes.func,
    fetchItems: PropTypes.func,
    component: PropTypes.func,
    items: PropTypes.array.isRequired,
    label: PropTypes.string,
    name: PropTypes.string,
    placeholder: PropTypes.string,
    className: PropTypes.string,
    itemsComponent: PropTypes.oneOfType([
      PropTypes.func,
      PropTypes.instanceOf(React.Component)
    ]),
    itemsProps: PropTypes.object,
  };

  static defaultProps = {
    name: 'typeahead',
    itemsComponent: TypeaheadItems,
  };

  constructor(...args) {
    super(...args);
    this.state = {
      showItems: false,
      updating: false,
    };
  }

  get placeholder() {
    let placeholder = 'Search';
    if (this.props.placeholder) {
      placeholder = this.props.placeholder;
    }
    return placeholder;
  }

  @autobind
  onItemSelected(item) {
    let doHide = true;

    const event = {
      preventHiding() {
        doHide = false;
      }
    };

    if (this.props.onItemSelected) {
      this.props.onItemSelected(item, event);
    }

    if (doHide) {
      this.setState({
        showItems: false
      });
    }
  }

  @autobind
  inputKeyUp({keyCode}) {
    if (keyCode === 27) {
      // They hit escape
      this.setState({
        showItems: false
      });
    }
  }

  @debounce(500)
  fetchItems(value) {
    if (this.props.fetchItems) {
      this.props.fetchItems(value);
    }
  }

  @autobind
  textChange({target}) {
    let value = target.value;

    this.setState({
      showItems: !(value === ''),
      updating: true
    });

    this.fetchItems(value);
  }

  componentWillReceiveProps(nextProps) {
    if (nextProps.items && nextProps.items != this.props.items) {
      this.setState({
        updating: false
      });
    }
  }

  toggleVisibility(show) {
    this.setState({
      showItems: show
    });
  }

  render() {
    const ItemsComponent = this.props.itemsComponent;

    const menuClass = classNames('fc-typeahead__menu', {
      '_visible': this.state.showItems
    });

    return (
      <div className={ classNames('fc-typeahead', this.props.className) }>
        <FormField className="fc-typeahead-input-group" label={this.props.label}>
          <i className="fc-typeahead-input-icon icon-search"></i>
          <input className="fc-input fc-typeahead-input"
                 type="text"
                 name={this.props.name}
                 placeholder={this.placeholder}
                 onChange={this.textChange}
                 onKeyUp={this.inputKeyUp}
          />
        </FormField>
        <div className={menuClass}>
          <ItemsComponent
            onItemSelected={this.onItemSelected}
            component={this.props.component}
            updating={this.state.updating}
            items={this.props.items}
            toggleVisibility={show => this.toggleVisibility(show)}
            {...this.props.itemsProps}
          />
        </div>
      </div>
    );
  }
}
