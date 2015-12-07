import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';

import _ from 'lodash';
import classNames from 'classnames';

import Menu from '../menu/menu';
import MenuItem from '../menu/menu-item';
import PilledInput from '../pilled-search/pilled-input';
import SearchOption from './search-option';

/**
 * LiveSearch is a search bar dynamic faceted search that exists on most of the
 * list pages. State for filters being created exist on the component, whereas
 * finalized filters are stored in Redux.
 */
export default class LiveSearch extends React.Component {
  constructor(props, context) {
    super(props, context);

    this.state = {
      optionsVisible: false,
      searchDisplay: props.state.searchValue,
      searchOptions: props.state.currentOptions,
      searchValue: props.state.searchValue,
      selectionIndex: -1
    };
  }

  static propTypes = {
    deleteSearchFilter: PropTypes.func.isRequired,
    goBack: PropTypes.func.isRequired,
    submitFilter: PropTypes.func.isRequired,
    state: PropTypes.object.isRequired
  };

  get searchOptions() {
    const options = _.transform(this.state.searchOptions, (result, option, idx) => {
      result.push(this.renderSearchOption(option, this.state.searchValue, idx, this.state.selectionIndex));
    });

    return (
      <Menu>
        {options}
        <MenuItem className='fc-live-search__go-back-item' onClick={this.props.goBack}>
          <i className='icon-back' />
          Back
        </MenuItem>
      </Menu>
    );
  }

  formatPill(pill, idx, props) {
    return (
      <div
        className="fc-pilled-input__pill"
        key={`pill-${idx}`}
        onClick={() => props.onPillClick(pill, idx)}>
        <i className='icon-filter' />
        {pill}
        <a onClick={() => props.onPillClose(pill, idx)}
          className="fc-pilled-input__pill-close">
          &times;
        </a>
      </div>
    );
  }

  renderSearchOption(option, searchTerm, idx, selectionIndex) {
    if (_.startsWith(option.displayTerm, searchTerm)) {
      const key = `search-option-${idx}`;
      const klass = classNames({
        'is-active': selectionIndex == idx,
        'is-first': idx == 0
      });

      return ( 
        <SearchOption
          className={klass}
          key={key}  
          option={option} 
          clickAction={this.props.submitSearch} />
      );
    }
  }

  componentWillReceiveProps(nextProps) {
    const isVisible = nextProps.state.searchValue !== '' 
      && nextProps.state.currentOptions.length > 0;

    this.setState({
      ...this.state,
      optionsVisible: isVisible,
      searchDisplay: nextProps.state.searchValue,
      searchOptions: nextProps.state.currentOptions,
      searchValue: nextProps.state.searchValue,
      selectionIndex: -1
    });
  }

  @autobind
  change({target}) {
    this.setState({
      ...this.state,
      searchDisplay: target.value,
      searchValue: target.value
    });
  }

  @autobind
  inputFocus() {
    if (!_.isEmpty(this.state.searchOptions)) {
      this.setState({
        ...this.state,
        optionsVisible: true
      });
    }
  }

  @autobind
  keyDown(event) {
    switch(event.keyCode) {
      case 40:
        // Down arrow
        event.preventDefault();
        if (!_.isEmpty(this.state.searchOptions)) {
          const newIdx = Math.min(
            this.state.selectionIndex + 1, 
            this.state.searchOptions.length - 1
          );

          this.setState({ 
            ...this.state, 
            optionsVisible: true,
            searchDisplay: this.state.searchOptions[newIdx].selectionValue,
            selectionIndex: newIdx
          });
        }
        break;
      case 38:
        // Up arrow
        event.preventDefault();
        if (!_.isEmpty(this.state.searchOptions)) {
          if (this.state.selectionIndex < 0) {
            this.setState({ ...this.state, optionsVisible: false });
          } else {
            const newIdx = this.state.selectionIndex - 1;
            const display = newIdx == -1
              ? this.state.searchValue
              : this.state.searchOptions[newIdx].selectionValue;

            this.setState({
              ...this.state,
              searchDisplay: display,
              selectionIndex: newIdx
            });
          }
        }
        break;
      case 13:
        // Enter
        event.preventDefault();
        this.props.submitFilter(this.state.searchDisplay);
        break;
      case 8:
        // Backspace
        if (_.isEmpty(this.state.searchValue) && !_.isEmpty(this.props.state.searches)) {
          this.props.deleteSearchFilter(this.props.state.searches.length - 1);
        }
        break;
    }
  }

  render() {
    return (
      <div className='fc-live-search fc-col-md-1-1'>
        <form>
          <PilledInput
            button={this.props.searchButton}
            onPillClose={(pill, idx) => this.props.deleteSearchFilter(idx)}
            onPillClick={(pill, idx) => this.props.deleteSearchFilter(idx)}
            formatPill={this.formatPill}
            placeholder={this.props.placeholder}
            onChange={this.change}
            onFocus={this.inputFocus}
            onKeyDown={this.keyDown}
            pills={this.props.state.searches}
            value={this.state.searchDisplay} />
        </form>
        <div>
          {this.state.optionsVisible && this.searchOptions}
        </div>
      </div>
    );
  }
}
