import React, { PropTypes } from 'react';
import Api from '../../lib/api';
import TypeaheadItems from './items';
import { FormField } from '../forms';
import { debounce, autobind } from 'core-decorators';

export default class Typeahead extends React.Component {

  static propTypes = {
    onItemSelected: PropTypes.func,
    fetchItems: PropTypes.func,
    component: PropTypes.func,
    items: PropTypes.array.isRequired,
    label: PropTypes.string,
    name: PropTypes.string,
    placeholder: PropTypes.string
  };

  static defaultProps = {
    name: 'typeahead'
  };

  constructor(props, context) {
    super(props, context);
    this.state = {
      showItems: false,
      updating: false
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
    this.setState({
      showItems: false
    });
    if (this.props.onItemSelected) {
      this.props.onItemSelected(item);
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

  render() {
    return (
      <div className="fc-typeahead">
        <FormField className="fc-typeahead-input-group" label={this.props.label}>
          <div className="fc-input-prepend"><i className="icon-search"></i></div>
          <input className="fc-input fc-typeahead-input"
                 type="text"
                 name={this.props.name}
                 placeholder={this.placeholder}
                 onChange={this.textChange}
                 onKeyUp={this.inputKeyUp}
          />
        </FormField>
        <TypeaheadItems
          onItemSelected={this.onItemSelected}
          component={this.props.component}
          showItems={this.state.showItems}
          updating={this.state.updating}
          items={this.props.items}
        />
      </div>
    );
  }
}
