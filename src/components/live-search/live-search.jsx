import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';

import _ from 'lodash';
import classNames from 'classnames';

import MaskedInput from '../masked-input/masked-input';
import Menu from '../menu/menu';
import MenuItem from '../menu/menu-item';
import PilledInput from '../pilled-search/pilled-input';
import SearchOption from './search-option';
import TabListView from '../tabs/tabs';
import EditableTabView from '../tabs/editable-tab';
import DatePicker from '../datepicker/datepicker';

import SearchTerm, { getInputMask } from '../../paragons/search-term';

function currentSearch(props) {
  return props.searches.currentSearch() || {};
}

/**
 * LiveSearch is a search bar dynamic faceted search that exists on most of the
 * list pages. State for filters being created exist on the component, whereas
 * finalized filters are stored in Redux.
 */
export default class LiveSearch extends React.Component {
  constructor(props, context) {
    super(props, context);

    const search = currentSearch(props);

    const {searchValue = '', query: pills} = search;
    const options = _.get(props, ['searches', 'searchOptions'], []);

    this.state = {
      availableOptions: options,
      inputMask: null,
      isFocused: false,
      optionsVisible: false,
      pills: pills,
      searchDisplay: searchValue,
      searchPrepend: '',
      searchOptions: options,
      searchValue: searchValue,
      selectionIndex: -1
    };
  }

  static propTypes = {
    children: PropTypes.node,
    deleteSearch: PropTypes.func.isRequired,
    saveSearch: PropTypes.func,
    selectSavedSearch: PropTypes.func.isRequired,
    searches: PropTypes.object,
    singleSearch: PropTypes.bool,
    submitPhrase: PropTypes.func.isRequired,
    submitFilters: PropTypes.func.isRequired,
    updateSearch: PropTypes.func.isRequired,
    noGutter: PropTypes.bool,
    fetchSearches: PropTypes.func.isRequired,
  };

  static defaultProps = {
    singleSearch: false,
    noGutter: false,
  };

  get currentSearch() {
    return currentSearch(this.props);
  }

  get isDirty() {
    return this.currentSearch.isDirty;
  }

  get searchOptions() {
    // Check to see if the date picker should be shown.
    let options = null;
    if (this.state.searchOptions.length == 1 && this.state.searchOptions[0].type == 'date') {
      const clickAction = date => {
        const dateVal = date.toLocaleString('en-us', {
          month: '2-digit',
          day: '2-digit',
          year: 'numeric'
        });

        this.submitFilter(`${this.state.searchValue}${dateVal}`, true);
      };


      options = (
        <DatePicker
          className="_in-menu"
          key="live-search-orders-datepicker"
          onClick={clickAction}
          showInput={false}
          showPicker={true} />
      );
    } else {
      const selectedIdx = this.state.selectionIndex;
      options = _.reduce(this.state.searchOptions, (result, option, idx) => {
        if (!option.matchesSearchTerm(this.state.searchValue)) {
          return result;
        }

        return [
          ...result,
          <SearchOption
            className={classNames({ '_active': selectedIdx == idx, '_first': idx == 0 })}
            key={`search-option-${option.displayTerm}`}
            option={option}
            clickAction={(filter) => this.submitFilter(filter, true)} />
        ];
      }, []);
    }

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

  get header() {
    if (this.props.singleSearch) return;

    return (
      <div className="fc-live-search__header">
        {this.savedSearches}
      </div>
    );
  }

  get savedSearches() {
    if (this.props.singleSearch) return;

    const tabs = _.map(this.props.searches.savedSearches, (search, idx) => {
      const selected = idx === this.props.searches.selectedSearch;
      const isEditable = search.isEditable;
      const isDirty = isEditable && this.props.searches.savedSearches[idx].isDirty;

      const copySearch = () => {
        this.props.saveSearch({ ...search, title: `${search.title} - Copy` });
      };
      const deleteSearch = () => this.props.deleteSearch(idx, search);
      const editName = title => {
        this.props.updateSearch(idx, { ...search, title: title });
      };
      const saveSearch = () => this.props.updateSearch(idx, search);

      return (
        <EditableTabView
          key={`saved-search-${search.title}`}
          defaultValue={search.title}
          draggable={isEditable}
          isDirty={isDirty}
          isEditable={isEditable}
          selected={selected}
          onClick={() => this.props.selectSavedSearch(idx)}
          onSaveUpdateComplete={saveSearch}
          onEditNameComplete={editName}
          onCopySearchComplete={copySearch}
          onDeleteSearchComplete={deleteSearch} />
      );
    });

    return <TabListView>{tabs}</TabListView>;
  }

  get searchButton() {
    if (this.props.singleSearch) return;

    const shouldSaveNew = this.currentSearch.title === 'All';
    const buttonContents = `${shouldSaveNew ? 'Save' : 'Update'} Search`;
    const clickAction = (event) => {
      event.preventDefault();
      if (shouldSaveNew) {
        this.props.saveSearch({
          ...this.currentSearch,
          title: `${this.currentSearch.title} - Copy`
        });
      } else {
        this.props.updateSearch(this.props.searches.selectedSearch, this.currentSearch);
      }
    };

    return (
      <button className="fc-btn" onClick={clickAction}>
        {buttonContents}
      </button>
    );
  }

  formatPill(pill, idx, props) {
    const display = pill.phrase ? pill.phrase : pill.display;
    const icon = pill.phrase ? 'icon-search' : 'icon-filter';

    return (
      <div
        className="fc-pilled-input__pill"
        key={`pill-${idx}`}
        onClick={() => props.onPillClick(pill, idx)}>
        <i className={icon} />
        {display}
        <a onClick={() => props.onPillClose(pill, idx)}
          className="fc-pilled-input__pill-close">
          &times;
        </a>
      </div>
    );
  }

  componentDidMount() {
    this.props.submitFilters(this.currentSearch.query, true);
    this.props.fetchSearches();
  }

  componentWillReceiveProps(nextProps) {
    const search = currentSearch(nextProps);
    const searchOptions = _.get(nextProps, ['searches', 'searchOptions'], []);
    const isVisible = this.state.isFocused && searchOptions.length > 0;

    this.setState({
      ...this.state,
      inputMask: null,
      optionsVisible: isVisible,
      pills: search.query,
      searchDisplay: search.searchValue,
      searchPrepend: '',
      searchOptions: searchOptions,
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
        if (this.state.searchOptions.length != 1 && this.state.selectionIndex == -1) {
          this.props.submitPhrase(this.state.searchDisplay);
          console.log('This might be a good time for a full-text search');
        } else if (this.state.selectionIndex < this.state.searchOptions.length) {
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
    let inputMask = this.state.inputMask;
    let searchPrepend = this.state.searchPrepend;

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
        inputMask = '';
        searchPrepend = '';
      } else {
        const newInputMask = getInputMask(option);
        if (!_.isEqual(inputMask, newInputMask)) {
          inputMask = newInputMask;
          searchPrepend = option.type == 'currency' ? `${searchTerm}$` : searchTerm;
        }
      }
    } else {
      inputMask = '';
      searchPrepend = '';
    }

    // Third, update the state.
    this.setState({
      inputMask: inputMask,
      searchOptions: options,
      searchDisplay: newSearchTerm,
      searchPrepend: searchPrepend,
      searchValue: newSearchTerm,
      selectionIndex: -1
    });
  }

  render() {
    const gridClass = classNames('fc-grid', 'fc-list-page-content', {
      'fc-grid-no-gutter': this.props.noGutter
    });
    const tableClass = classNames('fc-col-md-1-1', 'fc-live-search__table', {
      '_no-gutter': this.props.noGutter
    });

    let pills = [...this.state.pills];
    if (this.currentSearch.phrase) {
      pills.push({ phrase: this.currentSearch.phrase });
    }

    return (
      <div className="fc-live-search">
        {this.header}
        <div className={gridClass}>
          <div className="fc-col-md-1-1 fc-live-search__search-control">
            <form>
              <PilledInput
                button={this.searchButton}
                className={classNames({'_active': this.state.isFocused})}
                onPillClose={(pill, idx) => this.deleteFilter(idx)}
                onPillClick={(pill, idx) => this.deleteFilter(idx)}
                formatPill={this.formatPill}
                pills={pills}>
                <MaskedInput
                  className="fc-pilled-input__input-field _no-fc-behavior"
                  mask={this.state.inputMask}
                  onChange={this.change}
                  onFocus={this.inputFocus}
                  onBlur={this.blur}
                  onKeyDown={this.keyDown}
                  placeholder="Add another filter or keyword search"
                  prepend={this.state.searchPrepend}
                  value={this.state.searchDisplay} />
              </PilledInput>
            </form>
            <div>
              {this.state.optionsVisible && this.searchOptions}
            </div>
          </div>
        </div>
        <div className={gridClass}>
          <div className={tableClass} >
            {this.props.children}
          </div>
        </div>
      </div>
    );
  }
}
