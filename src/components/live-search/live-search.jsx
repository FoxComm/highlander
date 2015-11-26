import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';

import _ from 'lodash';
import classNames from 'classnames';

import Menu from '../menu/menu';
import MenuItem from '../menu/menu-item';
import SearchOption from './search-option';

/**
 * LiveSearch is a search bar dynamic faceted search that exists on most of the
 * list pages. State for filters being created exist on the component, whereas
 * finalized filters are stored in Redux.
 */
export default class LiveSearch extends React.Component {
  constructor(props, context) {
    super(props, context);
  }

  static propTypes = {
    updateSearch: PropTypes.func.isRequired,
    selectDown: PropTypes.func.isRequired,
    searchOptions: PropTypes.array,
    state: PropTypes.object.isRequired
  };


  @autobind
  onChange({target}) {
    this.props.updateSearch(target.value, this.props.searchOptions);
  } 

  @autobind
  inputFocus() {
    this.props.updateSearch(this.props.state.inputValue, this.props.searchOptions);
  }

  @autobind
  keyDown(event) {
    switch(event.keyCode) {
      case 40:
        // Down arrow
        event.preventDefault();
        this.props.selectDown();
        break;
      case 38:
        // Up arrow
        event.preventDefault();
        this.props.selectUp();
        break;
      case 13:
        // Enter
        event.preventDefault();
        this.props.submitFilter();
        break;
      case 8:
        // Backspace
        this.props.deleteSearchFilter(this.props.state.searches.length - 1);
        break;
    };
  }

  @autobind
  goBack() {
    this.props.goBack();
  }

  @autobind
  deleteSearchPill(idx) {
    this.props.deleteSearchFilter(idx);
  }

  get searchPills() {
    return this.props.state.searches.map((search, idx) => {
      return (
        <div className='fc-live-search-pill' key={`search-${idx}`}>
          <i className='icon-filter fc-live-search-pill-icon'></i>
          {search}
          <a className='fc-live-search-pill-close' onClick={() => this.deleteSearchPill(idx)}>
            &times;
          </a>
        </div>
      );
    });
  }

  get searchOptionsMenu() {
    const searchOptions = this.props.state.currentOptions.map((option, idx) => {
      const key = `${option.term}-${idx}`;
      const klass = classNames({
        'is-active': this.props.state.selectedIndex == idx,
        'is-first': idx == 0
      });

      return (
        <SearchOption
          className={klass}
          key={key}
          option={option}
          clickAction={this.props.updateSearch} />
      );
    });

    return (
      <Menu>
        {searchOptions}
        <MenuItem className='fc-search-option-back' onClick={this.goBack}>
          <i className='icon-back'></i>
          <span className='contents'>Back</span>
        </MenuItem>
      </Menu>
    );
  }

  render() {
    return (
      <div className="fc-col-md-1-1 fc-live-search">
        <div>
          <form>
            <div className='fc-live-search-meta-container'>
              <div className="fc-live-search-input-container">
                <div className='fc-live-search-pills'>
                  {this.searchPills}
                </div>
                <div className="fc-live-search-icon-wrapper">
                  <i className="icon-search"></i>
                </div>
                <div className="fc-live-search-input-wrapper">
                  <input 
                    className="fc-live-search-input-field"
                    type="text" 
                    placeholder="Add another filter or keyword search"
                    onChange={this.onChange}
                    onKeyDown={this.keyDown}
                    onFocus={this.inputFocus}
                    value={this.props.state.displayValue}
                  />
                </div>
              </div>
              <div className='fc-live-search-btn-container'>
                <button className='fc-btn fc-live-search-btn'>Save Search</button>
              </div>
            </div>
          </form>
        </div>
        { this.props.state.isVisible && this.searchOptionsMenu }
      </div>
    );
  }
}
