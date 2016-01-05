import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';

import _ from 'lodash';
import classNames from 'classnames';

import Menu from '../menu/menu';
import MenuItem from '../menu/menu-item';
import PilledInput from '../pilled-search/pilled-input';
import SearchOption from './search-option';
import TabListView from '../tabs/tabs';
import EditableTabView from '../tabs/editable-tab';

import SearchTerm from '../../paragons/search-term';

function currentSearch(props) {
  return _.get(props, ['searches', 'savedSearches', props.searches.selectedSearch], []);
}

/**
 * LiveSearch is a search bar dynamic faceted search that exists on most of the
 * list pages. State for filters being created exist on the component, whereas
 * finalized filters are stored in Redux.
 */
export default class LiveSearch extends React.Component {
  constructor(props, context) {
    super(props, context);

    const search = _.defaults(currentSearch(props), {searchValue: '', currentOptions: [], searches: []});
    const {searchValue, currentOptions, searches: pills} = search;

    this.state = {
      availableOptions: currentOptions,
      isFocused: false,
      optionsVisible: false,
      pills: pills,
      searchDisplay: searchValue,
      searchOptions: currentOptions,
      searchValue: searchValue,
      selectionIndex: -1
    };
  }

  static propTypes = {
    children: PropTypes.node,
    cloneSearch: PropTypes.func.isRequired,
    editSearchNameStart: PropTypes.func,
    editSearchNameCancel: PropTypes.func,
    editSearchNameComplete: PropTypes.func,
    saveSearch: PropTypes.func,
    selectSavedSearch: PropTypes.func.isRequired,
    searches: PropTypes.object,
    submitFilters: PropTypes.func.isRequired,
  };

  get currentSearch() {
    return _.get(this.props, ['searches', 'savedSearches', this.props.searches.selectedSearch], []);
  };

  get isDirty() {
    return this.currentSearch.isDirty;
  }

  get isEditingName() {
    return this.currentSearch.isEditingName;
  }

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
          clickAction={(filter) => this.submitFilter(filter, true)} />
      ];
    }, []);

    const menuClass = classNames('fc-live-search__go-back _last', {
      '_active': this.state.selectionIndex == this.state.searchOptions.length
    });

    const goBack = (
      <MenuItem className={menuClass} clickAction={this.goBack}>
        <i className="icon-back" />
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
    const tabs = _.map(this.props.searches.savedSearches, (search, idx) => {
      const selected = idx === this.props.searches.selectedSearch;
      const isEditing = selected && this.isEditingName;
      const draggable = !isEditing && search.name !== 'All';
      const isDirty = this.props.searches.savedSearches[idx].isDirty;

      const startEdit = (event) => {
        event.preventDefault();
        event.stopPropagation();
        this.props.editSearchNameStart(idx);
      };

      return (
        <EditableTabView
          key={`saved-search-${search.name}`}
          defaultValue={search.name}
          draggable={draggable}
          isDirty={isDirty}
          isEditing={isEditing}
          selected={selected}
          cancelEdit={this.props.editSearchNameCancel}
          completeEdit={this.props.editSearchNameComplete}
          startEdit={startEdit}
          onClick={() => this.props.selectSavedSearch(idx)}
        />
      );
    });

    return <TabListView>{tabs}</TabListView>;
  }

  get searchButton() {
    const shouldSaveNew = this.currentSearch.name === 'All';
    const buttonContents = `${shouldSaveNew ? 'Save' : 'Update'} Search`;
    const clickAction = (event) => {
      event.preventDefault();
      if (shouldSaveNew) {
        this.props.cloneSearch();
      } else {
        this.props.saveSearch();
      }
    };

    return (
      <button className="fc-btn" onClick={clickAction}>
        {buttonContents}
      </button>
    );
  }

  formatPill(pill, idx, props) {
    return (
      <div
        className="fc-pilled-input__pill"
        key={`pill-${idx}`}
        onClick={() => props.onPillClick(pill, idx)}>
        <i className='icon-filter' />
        {pill.display}
        <a onClick={() => props.onPillClose(pill, idx)}
          className="fc-pilled-input__pill-close">
          &times;
        </a>
      </div>
    );
  }

  componentWillReceiveProps(nextProps) {
    const search = currentSearch(nextProps);
    const isVisible = this.state.isFocused && search.currentOptions.length > 0;

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
    this.submitFilter(target.value);
  }

  @autobind
  inputFocus() {
    const { isFocus, optionsVisible, searchOptions } = this.state;
    if (!isFocus && !optionsVisible && !_.isEmpty(searchOptions)) {
      this.setState({
        ...this.state,
        isFocused: true,
        optionsVisible: true
      });
    }
  }

  @autobind
  blur() {
    this.setState({
      ...this.state,
      isFocused: false,
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
          this.submitFilter(this.state.searchDisplay, true);
        } else if (this.state.selectionIndex != -1) {
          this.goBack();
        }
        break;
      case 8:
        // Backspace
        if (_.isEmpty(this.state.searchValue) && !_.isEmpty(this.state.pills)) {
          this.deleteFilter(this.state.pills.length - 1);
        }
        break;
    }
  }

  @autobind
  deleteFilter(idx) {
    const filters = [
      ...this.state.pills.slice(0, idx),
      ...this.state.pills.slice(idx + 1)
    ];
    this.props.submitFilters(filters);
  }

  @autobind
  goBack() {
    const searchValue = this.state.searchValue;
    const lastColonIdx = _.trim(searchValue, ': ').lastIndexOf(':');
    const newSearchTerm = lastColonIdx > 0 ? `${searchValue.slice(0, lastColonIdx - 1)} : ` : '';
    return this.submitFilter(newSearchTerm);
  }

  @autobind
  submitFilter(searchTerm, tryFinal = false) {
    // First, update the available terms.
    let newSearchTerm = searchTerm;
    let options = SearchTerm.potentialTerms(this.state.availableOptions, searchTerm);

    // Second, if there is only one term, see if we can turn it into a saved search.
    if (options.length == 1) {
      const option = options[0];

      if (tryFinal && option.selectTerm(searchTerm)) {
        newSearchTerm = '';
        options = SearchTerm.potentialTerms(this.state.availableOptions, '');

        this.props.submitFilters([
          ...this.state.pills,
          option.toFilter(searchTerm)
        ]);
      } else if (option.children.length > 1) {
        options = option.children;
      }
    }

    // Third, update the state.
    this.setState({
      ...this.state,
      searchOptions: options,
      searchDisplay: newSearchTerm,
      searchValue: newSearchTerm,
      selectionIndex: -1
    });
  }

  render() {
    return (
      <div className="fc-live-search">
        <div className="fc-live-search__header">
          {this.savedSearches}
        </div>
        <div className="fc-grid fc-list-page-content">
          <div className="fc-col-md-1-1 fc-live-search__search-control">
            <form>
              <PilledInput
                button={this.searchButton}
                className={classNames({'_active': this.state.isFocused})}
                onPillClose={(pill, idx) => this.deleteFilter(idx)}
                onPillClick={(pill, idx) => this.deleteFilter(idx)}
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
            {this.props.children}
          </div>
        </div>
      </div>
    );
  }
}
