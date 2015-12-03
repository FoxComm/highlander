
// libs
import React, { PropTypes } from 'react';
import classNames from 'classnames';
import { debounce, autobind } from 'core-decorators';
import { cloneElement } from '../../lib/react-utils';

// components
import TypeaheadItems from './items';
import TypeaheadInput from './input';
import { FormField } from '../forms';
import Alert from '../alerts/alert';

export default class Typeahead extends React.Component {

  static propTypes = {
    onItemSelected: PropTypes.func,
    // fetchItems if passed should return promise for results
    fetchItems: PropTypes.func,
    component: PropTypes.func,
    items: PropTypes.array.isRequired,
    label: PropTypes.string,
    name: PropTypes.string,
    placeholder: PropTypes.string,
    className: PropTypes.string,
    itemsElement: PropTypes.element,
    inputElement: PropTypes.element,
    minQueryLength: PropTypes.number,
  };

  static defaultProps = {
    name: 'typeahead',
    placeholder: 'Search',
    minQueryLength: 1,
  };

  constructor(...args) {
    super(...args);
    this.state = {
      showMenu: false,
      updating: false,
      query: '',
    };
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
        showMenu: false
      });
    }
  }

  @autobind
  inputKeyUp({keyCode}) {
    if (keyCode === 27) {
      // They hit escape
      this.setState({
        showMenu: false
      });
    }
  }

  @debounce(500)
  fetchItems(value) {
    if (this.props.fetchItems) {
      this.props.fetchItems(value).then(this.onFetchDone, this.onFetchDone);
    }
  }

  @autobind
  onFetchDone() {
    this.setState({
      updating: false
    });
  }

  @autobind
  textChange({target}) {
    let value = target.value;

    const needUpdateItems = value.length >= this.props.minQueryLength;

    this.setState({
      showMenu: value.length > 0,
      query: value,
      updating: needUpdateItems
    });

    if (needUpdateItems) {
      this.fetchItems(value);
    }
  }

  toggleVisibility(show) {
    this.setState({
      showMenu: show
    });
  }

  get menuContent() {
    const queryLength = this.state.query.length;
    if (queryLength < this.props.minQueryLength && queryLength > 0) {
      return (
        <div className="fc-typeahead__need-more-characters">
          <Alert type={Alert.WARNING}>
            Please enter at least {this.props.minQueryLength} characters.
          </Alert>
        </div>
      );
    } else {
      const itemsElement = this.props.itemsElement;

      const ourProps = {
        onItemSelected: this.onItemSelected,
        component: this.props.component,
        updating: this.state.updating,
        items: this.props.items,
        toggleVisibility: show => this.toggleVisibility(show),
      };

      if (itemsElement) {
        return React.cloneElement(itemsElement, ourProps);
      } else {
        return <TypeaheadItems {...ourProps} />;
      }
    }
  }

  get inputContent() {
    const inputElement = this.props.inputElement;

    const defaultProps = {
      name: this.props.name,
      placeholder: this.props.placeholder,
    };

    const handlers = {
      onChange: this.textChange,
      onKeyUp: this.inputKeyUp,
    };

    if (inputElement) {
      return cloneElement(inputElement, {defaultProps, handlers});
    } else {
      return <TypeaheadInput {...defaultProps} {...handlers} />;
    }
  }

  render() {
    const menuClass = classNames('fc-typeahead__menu', {
      '_visible': this.state.showMenu
    });

    return (
      <div className={ classNames('fc-typeahead', this.props.className) }>
        <FormField className="fc-typeahead__input-group" label={this.props.label}>
          {this.inputContent}
        </FormField>
        <div className={menuClass}>
          {this.menuContent}
        </div>
      </div>
    );
  }
}
