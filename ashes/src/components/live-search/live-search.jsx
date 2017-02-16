// libs
import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';

import _ from 'lodash';
import classNames from 'classnames';
import { trackEvent } from 'lib/analytics';

// components
import MaskedInput from '../masked-input/masked-input';
import Menu from '../menu/menu';
import MenuItem from '../menu/menu-item';
import PilledInput from '../pilled-search/pilled-input';
import SearchOption from './search-option';
import TabListView from '../tabs/tabs';
import EditableTabView from '../tabs/editable-tab';
import DatePicker from '../datepicker/datepicker';
import ShareSearch from '../share-search/share-search';
import { Button } from '../common/buttons';
import ButtonWithMenu from '../common/button-with-menu';
import Alert from '../alerts/alert';

import SearchTerm, { getInputMask } from '../../paragons/search-term';

const SEARCH_MENU_ACTION_SHARE = 'share';
const SEARCH_MENU_ACTION_SAVE = 'save';
const SEARCH_MENU_ACTION_UPDATE = 'update';
const SEARCH_MENU_ACTION_DELETE = 'delete';
const SEARCH_MENU_ACTION_CLEAR = 'clear';
const SEARCH_MENU_ACTION_EDIT_NAME = 'edit_name';

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

    const { searchValue = '', query } = search;
    const options = _.get(props, ['searches', 'searchOptions'], []);

    this.state = {
      availableOptions: options,
      inputMask: null,
      isFocused: false,
      isShareVisible: false,
      optionsVisible: false,
      pills: this.queryToPills(query),
      searchDisplay: searchValue,
      searchPrepend: '',
      searchOptions: options,
      searchValue: searchValue,
      selectionIndex: -1,
      shouldSetFocus: false,
      editingTab: null,
    };
  }

  static propTypes = {
    children: PropTypes.node,
    entity: PropTypes.string.isRequired,
    placeholder: PropTypes.string,
    deleteSearch: PropTypes.func.isRequired,
    saveSearch: PropTypes.func,
    selectSavedSearch: PropTypes.func.isRequired,
    searches: PropTypes.object,
    singleSearch: PropTypes.bool,
    isEditable: PropTypes.bool,
    submitPhrase: PropTypes.func.isRequired,
    submitFilters: PropTypes.func.isRequired,
    updateSearch: PropTypes.func.isRequired,
    noGutter: PropTypes.bool,
    fetchSearches: PropTypes.func.isRequired,

    fetchAssociations: PropTypes.func,
    associateSearch: PropTypes.func,
    dissociateSearch: PropTypes.func,
    selectItem: PropTypes.func,
    deselectItem: PropTypes.func,
    setTerm: PropTypes.func,
  };

  static defaultProps = {
    placeholder: 'filter or keyword search',
    singleSearch: false,
    isEditable: true,
    noGutter: false,
  };

  componentDidMount() {
    this.props.submitFilters(this.currentSearch.query, true);

    if (!this.props.singleSearch) {
      this.props.fetchSearches();
    }
  }

  componentDidUpdate() {
    if (this.state.shouldSetFocus && !this.isDisabled) {
      this.refs.input.focus();
    }
  }

  queryToPills(query) {
    return _.filter(query, q => !q.hidden);
  }

  componentWillReceiveProps(nextProps) {
    const search = currentSearch(nextProps);
    const searchOptions = _.get(nextProps, ['searches', 'searchOptions'], []);
    const isVisible = this.state.isFocused && searchOptions.length > 0;

    const isRefreshed = _.get(search, ['results', 'refreshed'], false);
    if (!isRefreshed) {
      this.setState({
        searchPrepend: '',
        selectionIndex: -1,
        inputMask: null,
        pills: this.queryToPills(search.query),
        searchDisplay: search.searchValue,
        optionsVisible: isVisible,
        searchOptions: searchOptions,
      });
    }

    this.setState({
      searchValue: search.searchValue,
    });
  }

  get currentSearch() {
    return currentSearch(this.props);
  }

  get isDirty() {
    return this.currentSearch.isDirty;
  }

  get isDisabled() {
    return _.get(this.currentSearch, ['results', 'isFetching'], true) || this.currentSearch.isUpdating;
  }

  get searchOptions() {
    // Check to see if the date picker should be shown.
    let options = null;
    let goBack = null;

    if (this.state.optionsVisible) {
      if (this.state.searchOptions.length == 1 && this.state.searchOptions[0].type == 'date') {
        const clickAction = date => {
          const dateVal = date.toLocaleString('en-us', {
            month: '2-digit',
            day: '2-digit',
            year: 'numeric'
          });

          trackEvent('LiveSearch', 'click_datePicker', 'Click date picker');
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

          const handleClick = filter => {
            trackEvent('LiveSearch', `click_option [${option.displayTerm}]`);
            this.submitFilter(filter, true);
          };

          return [
            ...result,
            <SearchOption
              className={classNames({ '_active': selectedIdx == idx, '_first': idx == 0 })}
              key={`search-option-${option.displayTerm}`}
              option={option}
              clickAction={handleClick}
            />
          ];
        }, []);
      }

      const menuClass = classNames('fc-live-search__go-back _last', {
        '_active': this.state.selectionIndex == this.state.searchOptions.length
      });

      goBack = (
        <MenuItem className={menuClass} clickAction={this.goBack}>
          <i className="icon-back" />
          Back
        </MenuItem>
      );
    }

    return (
      <Menu position="left" isOpen={!!options}>
        {options}
        {!_.isEmpty(this.state.searchValue) && goBack}
      </Menu>
    );
  }

  get dropdownContent() {
    if (this.state.errorMessage) {
      return (
        <Alert type={Alert.WARNING}>
          {this.state.errorMessage}
        </Alert>
      );
    }

    return this.searchOptions;
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

    const { searches } = this.props;

    let isLoading = searches.fetchingSearches;

    const tabs = _.map(searches.savedSearches, (search, idx) => {
      const selected = idx === searches.selectedSearch;
      const isEditable = search.isEditable;
      const isDirty = isEditable && searches.savedSearches[idx].isDirty;

      isLoading = isLoading || search.isUpdating || search.isSaving || search.isDeleting;

      return (
        <EditableTabView
          key={`saved-search-${search.id}-${search.title}`}
          defaultValue={search.title}
          draggable={isEditable}
          isDirty={isDirty}
          isEditable={isEditable}
          selected={selected}
          onClick={() => !selected && !isLoading && this.props.selectSavedSearch(idx)}
          editMode={this.state.editingTab === idx}
          onEditNameComplete={title => this.handleEditName(search, title, idx)}
          onEditNameCancel={this.handleEditNameCancel} />
      );
    });

    return <TabListView isLoading={isLoading}>{tabs}</TabListView>;
  }

  get controls() {
    const { searchDisplay, pills } = this.state;
    const { searches, singleSearch } = this.props;

    const buttonDisabled = searchDisplay.length == 0 || searches.isSavingSearch || this.isDisabled;

    if (singleSearch) {
      return <Button icon="search" onClick={this.handleSearchClick} disabled={buttonDisabled} />;
    }

    let menuItems = [];

    const clearAction = pills.length ? [[SEARCH_MENU_ACTION_CLEAR, 'Clear All Filters']] : [];

    if (this.currentSearch.id) {
      const saveAction = this.currentSearch.isDirty ? [[SEARCH_MENU_ACTION_UPDATE, 'Update Search']] : [];
      menuItems = [
        [SEARCH_MENU_ACTION_SAVE, 'Save New Search'],
        ...saveAction,
        [SEARCH_MENU_ACTION_SHARE, 'Share Search'],
        [SEARCH_MENU_ACTION_EDIT_NAME, 'Edit Search Name'],
        ...clearAction,
        [SEARCH_MENU_ACTION_DELETE, 'Delete Search'],
      ];
    } else {
      menuItems = [
        [SEARCH_MENU_ACTION_SAVE, 'Save New Search'],
        ...clearAction,
      ];
    }

    const menuDisabled = searches.isSavingSearch || this.isDisabled;

    return (
      <ButtonWithMenu
        onPrimaryClick={this.handleSearchClick}
        icon="search"
        menuPosition="right"
        onSelect={this.handleMenu}
        items={menuItems}
        buttonDisabled={buttonDisabled}
        menuDisabled={menuDisabled}
      />
    );
  }

  @autobind
  handleEditName(search, title, searchIndex) {
    this.setState({
      editingTab: null,
    }, () => this.props.updateSearch(searchIndex, { ...search, title }));
  }

  @autobind
  handleEditNameCancel() {
    this.setState({
      editingTab: null,
    });
  }

  @autobind
  handleSearchClick(event) {
    event.preventDefault();

    if (this.state.searchDisplay.length > 0) {
      this.props.submitPhrase(this.state.searchDisplay);
    }
  }

  @autobind
  handleMenu(value) {
    const { selectedSearch } = this.props.searches;
    const search = this.currentSearch;

    switch (value) {
      case SEARCH_MENU_ACTION_CLEAR:
        this.props.submitFilters([]);
        break;
      case SEARCH_MENU_ACTION_UPDATE:
        this.props.updateSearch(selectedSearch, search);
        break;
      case SEARCH_MENU_ACTION_SAVE:
        this.props.saveSearch({ ...search, title: `${search.title} - Copy` }).then(() => {
          this.setState({ editingTab: this.props.searches.selectedSearch });
        });
        break;
      case SEARCH_MENU_ACTION_DELETE:
        this.props.deleteSearch(selectedSearch, search);
        break;
      case SEARCH_MENU_ACTION_SHARE:
        this.openShareSearch();
        break;
      case SEARCH_MENU_ACTION_EDIT_NAME:
        this.setState({ editingTab: selectedSearch });
        break;
    }
  }

  @autobind
  formatPill(pill, idx, props) {
    const icon = pill.term === '_all' ? 'icon-search' : 'icon-filter';

    return (
      <div
        className="fc-pilled-input__pill"
        key={`pill-${this.currentSearch.title}-${idx}`}
        onClick={() => props.onPillClick(pill, idx)}
        title={pill.display}>
        <i className={icon} />
        {pill.display}
        <a onClick={() => props.onPillClose(pill, idx)}
           className="fc-pilled-input__pill-close">
          &times;
        </a>
      </div>
    );
  }

  @autobind
  change({ target }) {
    this.submitFilter(target.value);
  }

  @autobind
  inputFocus() {
    const { isFocus, optionsVisible, searchOptions } = this.state;

    if (!isFocus && !optionsVisible && !_.isEmpty(searchOptions)) {
      this.setState({
        isFocused: true,
        optionsVisible: true,
        shouldSetFocus: false,
      });
    }
  }

  @autobind
  blur() {
    this.setState({
      isFocused: false,
      optionsVisible: false,
      shouldSetFocus: this.isDisabled,
    });
  }

  @autobind
  keyDown(event) {
    switch (event.keyCode) {
      case 40:
        // Down arrow
        event.preventDefault();
        if (!_.isEmpty(this.state.searchOptions) || !_.isEmpty(this.state.searchValue)) {
          trackEvent('LiveSearch', 'hit_down_arrow');
          // Allow the selection of go back when there is a search term.
          const maxLength = _.isEmpty(this.state.searchValue)
            ? this.state.searchOptions.length - 1
            : this.state.searchOptions.length;

          const newIdx = (this.state.selectionIndex + 1) > maxLength
            ? 0
            : this.state.selectionIndex + 1;

          let newSearchDisplay;
          if (newIdx < this.state.searchOptions.length) {
            newSearchDisplay = this.state.searchOptions[newIdx].selectionValue;
          } else {
            newSearchDisplay = this.state.searchValue;
          }

          this.setState({
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
          trackEvent('LiveSearch', 'hit_up_arrow');
          if (this.state.selectionIndex < 0) {
            this.setState({ optionsVisible: false });
          } else {
            const newIdx = this.state.selectionIndex - 1;
            const display = newIdx == -1
              ? this.state.searchValue
              : this.state.searchOptions[newIdx].selectionValue;

            this.setState({
              searchDisplay: display,
              selectionIndex: newIdx
            });
          }
        }
        break;
      case 13:
        // Enter
        trackEvent('LiveSearch', 'hit_enter');
        event.preventDefault();
        if (this.state.searchOptions.length != 1 && this.state.selectionIndex == -1) {
          this.props.submitPhrase(this.state.searchDisplay);
        } else if (this.state.selectionIndex < this.state.searchOptions.length) {
          this.submitFilter(this.state.searchDisplay, true);
        } else if (this.state.selectionIndex != -1) {
          this.goBack();
        }
        break;
      case 8:
        // Backspace
        if (_.isEmpty(this.state.searchValue) && !_.isEmpty(this.state.pills)) {
          trackEvent('LiveSearch', 'delete_pill_by_backspace');
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
    this.setState({ errorMessage: null });

    let newSearchTerm = searchTerm;
    let options = SearchTerm.potentialTerms(this.state.availableOptions, searchTerm);
    let inputMask = this.state.inputMask;
    let searchPrepend = this.state.searchPrepend;
    let searchDisplay = newSearchTerm;

    // Second, if there is only one term, see if we can turn it into a saved search.
    if (options.length == 1) {
      const option = options[0];

      if (tryFinal && option.selectTerm(searchTerm)) {
        newSearchTerm = '';
        options = SearchTerm.potentialTerms(this.state.availableOptions, '');
        const filter = option.toFilter(searchTerm);

        if (filter.value.type === 'string' && filter.value.value.length < 3) {
          this.setState({
            errorMessage: 'Please enter at least 3 characters.'
          });
          return;
        }

        this.props.submitFilters([
          ...this.state.pills,
          filter
        ]);
      } else if (option.children.length > 1) {
        options = option.children;
        inputMask = '';
        searchPrepend = '';
      } else {
        const newInputMask = getInputMask(option);
        if (!_.isEqual(inputMask, newInputMask)) {
          inputMask = newInputMask;
        }

        searchPrepend = option.type == 'currency' ? `${option.selectionValue}$` : option.selectionValue;
        searchDisplay = `${searchPrepend}${newSearchTerm.substr(searchPrepend.length)}`;
      }
    } else {
      inputMask = '';
      searchPrepend = '';
    }

    // Third, update the state.
    this.setState({
      inputMask: inputMask,
      searchOptions: options,
      searchDisplay: searchDisplay,
      searchPrepend: searchPrepend,
      searchValue: newSearchTerm,
      selectionIndex: -1
    });
  }

  @autobind
  openShareSearch() {
    this.setState({ isShareVisible: true });
  }

  @autobind
  handleCloseShareSearch() {
    this.setState({ isShareVisible: false });
  }

  get shareSearch() {
    if (this.props.singleSearch) {
      return null;
    }

    return (
      <ShareSearch
        entity={this.props.entity}
        isVisible={this.state.isShareVisible}
        title={this.currentSearch.title}
        onClose={this.handleCloseShareSearch}
        fetchAssociations={this.props.fetchAssociations}
        associateSearch={this.props.associateSearch}
        dissociateSearch={this.props.dissociateSearch}
      />
    );
  }

  @autobind
  handlePillClose(pill, idx) {
    if (!this.isDisabled) {
      trackEvent('LiveSearch', 'click_pill_close', 'Delete pill');
      this.deleteFilter(idx);
    }
  }

  shouldComponentUpdate(nextProps, nextState) {
    const search = currentSearch(this.props);
    const newSearch = currentSearch(nextProps);

    if (search.code != newSearch.code) {
      return true;
    }

    if (!_.eq(search.shares, newSearch.shares)) {
      // do not rerender entire live-search component on share state update
      return false;
    }

    return true;
  }

  render() {
    const gridClass = classNames('fc-list-page-content', {
      '_no-gutter': this.props.noGutter
    });
    const tableClass = classNames('fc-live-search__table', {
      '_no-gutter': this.props.noGutter
    });

    return (
      <div className="fc-live-search">
        {this.shareSearch}
        {this.header}
        <div className={gridClass}>
          <div className="fc-live-search__search-control">
            <form>
              <PilledInput
                controls={this.controls}
                className={classNames({'_active': this.state.isFocused, '_disabled': this.isDisabled})}
                onPillClose={this.handlePillClose}
                formatPill={this.formatPill}
                icon={null}
                pills={this.state.pills}>
                <MaskedInput
                  className="fc-pilled-input__input-field _no-fc-behavior"
                  mask={this.state.inputMask}
                  onChange={this.change}
                  onFocus={this.inputFocus}
                  onBlur={this.blur}
                  onKeyDown={this.keyDown}
                  placeholder={this.props.placeholder}
                  prepend={this.state.searchPrepend}
                  value={this.state.searchDisplay}
                  disabled={this.isDisabled}
                  ref="input" />
              </PilledInput>
            </form>

            <div>
              {this.dropdownContent}
            </div>
          </div>
        </div>
        <div className={gridClass}>
          <div className={tableClass}>
            {this.props.children}
          </div>
        </div>
      </div>
    );
  }
}
