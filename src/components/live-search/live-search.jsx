import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';

import _ from 'lodash';
import classNames from 'classnames';

import Menu from '../menu/menu';
import MenuItem from '../menu/menu-item';
import PilledInput from '../pilled-search/pilled-input';
import SearchOption from './search-option';
import TabListView from '../tabs/tabs';
import TabView from '../tabs/tab';
import TableView from '../table/tableview';
import TableRow from '../table/row';
import TableCell from '../table/cell';

const currentSearch = (searchState) => {
  return searchState.savedSearches[searchState.selectedSearch];
};

/**
 * LiveSearch is a search bar dynamic faceted search that exists on most of the
 * list pages. State for filters being created exist on the component, whereas
 * finalized filters are stored in Redux.
 */
export default class LiveSearch extends React.Component {
  constructor(props, context) {
    super(props, context);

    const search = currentSearch(props.searches);
    this.state = {
      optionsVisible: false,
      pills: search.searches,
      searchDisplay: search.searchValue,
      searchOptions: search.currentOptions,
      searchValue: search.searchValue,
      selectionIndex: -1
    };
  }

  static propTypes = {
    deleteSearchFilter: PropTypes.func.isRequired,
    goBack: PropTypes.func.isRequired,
    selectSavedSearch: PropTypes.func.isRequired,
    submitFilter: PropTypes.func.isRequired,
    state: PropTypes.object.isRequired,
    columns: PropTypes.array.isRequired,
    data: PropTypes.node.isRequired,
    renderRow: PropTypes.func,
    setState: PropTypes.func
  };

  get searchOptions() {
    const selectedIdx = this.state.selectionIndex;
    const options = _.reduce(this.state.searchOptions, (result, option, idx) => {
      if (!option.matchesSearchTerm(this.state.searchValue)) {
        return result;
      }

      return [
        ...result,
        <SearchOption
          className={classNames({ '_active': selectedIdx == idx, '_first': idx == 0 })}
          key={`search-option-${idx}`}
          option={option}
          clickAction={this.props.submitFilter} />
      ];
    }, []);

    const menuClass = classNames('fc-live-search__go-back _last', {
      '_active': this.state.selectionIndex == this.state.searchOptions.length
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

  get savedSearches() {
    const tabs = _.keys(this.props.searches.savedSearches).map(search => {
      const draggable = search !== 'All';
      const selected = search === this.props.searches.selectedSearch;

      return (
        <TabView 
          draggable={draggable}
          selected={selected}
          onClick={() => this.props.selectSavedSearch(search)}
          >
          {search}
        </TabView>
      );
    });

    return <TabListView>{tabs}</TabListView>;
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

  componentWillReceiveProps(nextProps) {
    const search = currentSearch(nextProps.searches);
    const isVisible = search.currentOptions.length > 0 &&
      (search.searches.length != this.state.pills.length || search.searchValue !== '');

    this.setState({
      ...this.state,
      optionsVisible: isVisible,
      pills: search.searches,
      searchDisplay: search.searchValue,
      searchOptions: search.currentOptions,
      searchValue: search.searchValue,
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
        if (_.isEmpty(this.state.searchValue) && !_.isEmpty(this.state.pills)) {
          this.props.deleteSearchFilter(this.state.pills.length - 1);
        }
        break;
    }
  }

  render() {
    return (
      <div className='fc-live-search'>
        <div className="fc-live-search__header">
          {this.savedSearches}
        </div>
        <div className="fc-grid fc-list-page-content">
          <div className='fc-col-md-1-1 fc-live-search__search-control'>
            <form>
              <PilledInput
                button={this.props.searchButton}
                onPillClose={(pill, idx) => this.props.deleteSearchFilter(idx)}
                onPillClick={(pill, idx) => this.props.deleteSearchFilter(idx)}
                formatPill={this.formatPill}
                placeholder="Add another filter or keyword search"
                onChange={this.change}
                onFocus={this.inputFocus}
                onBlur={this.blur}
                onKeyDown={this.keyDown}
                pills={this.state.pills}
                value={this.state.searchDisplay} />
            </form>
            <div>
              {this.state.optionsVisible && this.searchOptions}
            </div>
          </div>
          <div className="fc-col-md-1-1">
            <TableView
              columns={this.props.columns}
              data={this.props.data}
              renderRow={this.props.renderRow}
              setState={this.props.setState}
            />
          </div>
        </div>
      </div>
    );
  }
}
