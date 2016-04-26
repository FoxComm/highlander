
/* @flow */

import _ from 'lodash';
import React, { PropTypes, Element, Component } from 'react';
import { autobind } from 'core-decorators';

import GenericDropdown from './generic-dropdown';
import DropdownItem from './dropdownItem';

import styles from './dropdown-search.css';

type Props = {
  name: string,
  className?: string,
  value: ?string|number,
  searchbarPlaceholder?: string,
  disabled?: bool,
  editable?: bool,
  changeable?: bool,
  primary?: bool,
  open?: bool,
  placeholder?: string,
  onChange: Function,
  renderNullTitle?: Function,
  fetchOptions: Function,
  renderOption?: Function,
};

type State = {
  token: string,
  results: Array<any>,
};

function doNothing(event: any) {
  event.stopPropagation();
  event.preventDefault();
}

export default class DropdownSearch extends Component {

  props: Props;

  state: State = {
    token: '',
    results: [],
  };

  componentDidMount() {
    this.fetchOptions(this.state.token);
  }

  fetchOptions(token: string) {
    this.props.fetchOptions(this.state.token).then(data => {
      this.setState({results: data});
    });
  }

  @autobind
  onTokenChange(event: any) {
    const token = _.get(event, 'target.value', '');
    this.setState({ token }, () => {
      this.fetchOptions(token);
    });
  }

  @autobind
  searchBar(): Element {
    return (
      <div styleName="searchbar" onClick={doNothing} >
        <div styleName="searchbar-wrapper" >
          <div className="fc-form-field" styleName="searchbar-input-wrapper" >
            <input
              type="text"
              placeholder={this.props.searchbarPlaceholder}
              styleName="searchbar-input"
              value={this.state.token}
              onChange={this.onTokenChange}
            />
          </div>
          <div styleName="searchbar-icon-wrapper">
            <i className="icon-search"></i>
          </div>
        </div>
      </div>
    );
  }

  get results(): Array<any> {
    const results = this.state.results;

    if (_.isEmpty(results)) {
      return [];
    }

    return results;
  }

  get searchResults(): any {
    const { renderOption } = this.props;
    return _.map(this.results, (result) => {
      if (renderOption) {
        return renderOption(result);
      }

      const id = _.get(result, 'id', '');
      const name = _.get(result, 'name', '');

      return (
        <DropdownItem value={id} key={`${id}-${name}`}>
          {name}
        </DropdownItem>
      );
    });
  }

  render() {
    const {
      name, placeholder, value, primary, editable, open, disabled, onChange, renderNullTitle, className
    } = this.props;

    return (
      <GenericDropdown
        name={name}
        value={value}
        placeholder={placeholder}
        className={className}
        listClassName="fc-searchable-dropdown__item-list"
        primary={primary}
        editable={editable}
        disabled={disabled}
        open={open}
        renderNullTitle={renderNullTitle}
        onChange={onChange}
        renderPrepend={this.searchBar}>
        { this.searchResults }
      </GenericDropdown>
    );
  }

}
