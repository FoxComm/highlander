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

    const menuClass = classNames('fc-live-search__go-back-item is-last', {
      'is-active': this.state.selectionIndex == this.state.searchOptions.length
    });

    const goBack = (
      <MenuItem className={menuClass} onClick={this.props.goBack}>
        <i className='icon-back' />
        Back
      </MenuItem>
    );

    return (
      <Menu>
        {options}
        {!_.isEmpty(this.state.searchValue) && goBack}
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
          clickAction={this.props.submitFilter} />
      );
    }
  }

  componentWillReceiveProps(nextProps) {
    const isVisible = nextProps.state.currentOptions.length > 0 &&
      (nextProps.state.searches.length != this.props.state.searches.length ||
       nextProps.state.searchValue !== '');

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
  blur() {
    this.setState({
      ...this.state,
      optionsVisible: false
    });
  }

  @autobind
  keyDown(event) {
    switch(event.keyCode) {
      case 40:
        // Down arrow
        event.preventDefault();
        if (!_.isEmpty(this.state.searchOptions) || !_.isEmpty(this.state.searchValue)) {
          // Allow the selection of go back when there is a search term.
          const maxLength = _.isEmpty(this.state.searchValue)
            ? this.state.searchOptions.length - 1
            : this.state.searchOptions.length;

          const newIdx = Math.min(this.state.selectionIndex + 1, maxLength);

          let newSearchDisplay;
          if (newIdx < this.state.searchOptions.length) {
            newSearchDisplay = this.state.searchOptions[newIdx].selectionValue;
          } else {
            newSearchDisplay = this.state.searchValue;
          }

          this.setState({ 
            ...this.state, 
            optionsVisible: true,
            searchDisplay: newSearchDisplay,
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
        if (this.state.selectionIndex < this.state.searchOptions.length) {
          this.props.submitFilter(this.state.searchDisplay);
        } else if (this.state.selectionIndex != -1) {
          this.props.goBack();
        }
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
            button={<button className="fc-btn">Save Search</button>}
            onPillClose={(pill, idx) => this.props.deleteSearchFilter(idx)}
            onPillClick={(pill, idx) => this.props.deleteSearchFilter(idx)}
            formatPill={this.formatPill}
            placeholder="Add another filter or keyword search"
            onChange={this.change}
            onFocus={this.inputFocus}
            onBlur={this.blur}
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
