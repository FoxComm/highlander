import React, { PropTypes } from 'react';
import ReactDOM from 'react-dom';
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
import ShareSearch from '../share-search/share-search';
import { Button } from '../common/buttons';

import SearchTerm, { getInputMask } from '../../paragons/search-term';

const SEARCH_ALL = 'All';

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

    const { searchValue = '', query: pills } = search;
    const options = _.get(props, ['searches', 'searchOptions'], []);

    this.state = {
      availableOptions: options,
      inputMask: null,
      isFocused: false,
      isShareVisible: false,
      optionsVisible: false,
      pills: pills,
      searchDisplay: searchValue,
      searchPrepend: '',
      searchOptions: options,
      searchValue: searchValue,
      selectionIndex: -1,
      shouldSetFocus: false,
      searchMenuOpened: false,
      editingTab: null,
    };
  }

  static propTypes = {
    children: PropTypes.node,
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

    suggestAssociations: PropTypes.func,
    fetchAssociations: PropTypes.func,
    associateSearch: PropTypes.func,
    dissociateSearch: PropTypes.func,
    selectItem: PropTypes.func,
    deselectItem: PropTypes.func,
    setTerm: PropTypes.func,
  };

  static defaultProps = {
    singleSearch: false,
    isEditable: true,
    noGutter: false,
  };

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

  get header() {
    if (this.props.singleSearch) return;

    return (
      <div className="fc-live-search__header">
        {this.savedSearches}
      </div>
    );
  }

  get savedSearches() {
    if (this.props.singleSearch) {
      return;
    }

    const { searches } = this.props;

    let isLoading = searches.fetchingSearches;

    const tabs = _.map(searches.savedSearches, (search, idx) => {
      const selected = idx === searches.selectedSearch;
      const isEditable = search.isEditable;
      const isDirty = isEditable && searches.savedSearches[idx].isDirty;

      isLoading = isLoading || search.isUpdating || search.isSaving || search.isDeleting;

      const onEditName = title => {
        this.setState({
          editingTab: null,
        }, () => this.props.updateSearch(idx, { ...search, title: title }));
      };
      const onEditNameCancel = () => {
        this.setState({
          editingTab: null,
        });
      };

      return (
        <EditableTabView
          key={`saved-search-${search.id}-${search.title}`}
          defaultValue={search.title}
          draggable={isEditable}
          isDirty={isDirty}
          isEditable={isEditable}
          selected={selected}
          onClick={() => !selected && this.props.selectSavedSearch(idx)}
          editMode={this.state.editingTab === idx}
          onEditNameComplete={onEditName}
          onEditNameCancel={onEditNameCancel} />
      );
    });

    return <TabListView isLoading={isLoading}>{tabs}</TabListView>;
  }

  @autobind
  onDocumentClick(event) {
    const menuButton = ReactDOM.findDOMNode(this.refs.searchMenuButton);
    const isEditClick = menuButton && menuButton.contains(event.target);

    if (!isEditClick && this.state.searchMenuOpened) {
      this.setState({ searchMenuOpened: false });
    }
  }

  @autobind
  toggleSearchMenu() {
    this.setState({ searchMenuOpened: !this.state.searchMenuOpened });
  }

  get searchMenuOptions() {
    const search = this.currentSearch;
    const { selectedSearch } = this.props.searches;

    const copySearch = () => this.props.saveSearch({ ...search, title: `${search.title} - Copy` });
    const saveSearch = () => this.props.updateSearch(selectedSearch, search);
    const deleteSearch = () => this.props.deleteSearch(selectedSearch, search);

    const startEditName = () => {
      this.setState({
        editingTab: selectedSearch,
        searchMenuOpened: false
      });
    };

    const saveAction = this.currentSearch.isDirty ? [{ title: 'Save Search Update', action: saveSearch }] : [];

    return [
      ...saveAction,
      { title: 'Edit Name', action: startEditName },
      { title: 'Copy Search', action: copySearch },
      { title: 'Delete Search', action: deleteSearch }
    ];
  }

  get searchMenu() {
    const options = this.searchMenuOptions.map((opt, idx) => {
      const key = `${idx}-${_.kebabCase(opt.title)}`;

      return (
        <MenuItem isFirst={idx == 0} clickAction={opt.action} key={key}>
          {opt.title}
        </MenuItem>
      );
    });

    return <Menu position="right" isOpen={this.state.searchMenuOpened}>{options}</Menu>;
  }

  get searchMenuButton() {
    const hasMenu = !this.props.singleSearch && this.props.isEditable;

    if (hasMenu) {
      const disabled = this.props.searches.isSavingSearch || this.isDisabled;

      const buttonClass = classNames('fc-pilled-input__controls-item', {
        '_active': this.state.searchMenuOpened,
        '_disabled': disabled
      });

      return (
        <div className={buttonClass}>
          <div className="fc-pilled-input__icon-wrapper" onClick={() => !disabled && this.toggleSearchMenu()} ref="searchMenuButton">
            <i className="icon-chevron-down" />
          </div>
          {this.searchMenu}
        </div>
      );
    }
  }

  get searchButton() {
    const buttonClass = classNames('fc-pilled-input__icon-wrapper', {
      '_disabled': this.state.searchDisplay.length == 0 || this.props.searches.isSavingSearch || this.isDisabled
    });

    const clickAction = () => {
      if (this.state.searchDisplay.length > 0) {
        this.props.submitPhrase(this.state.searchDisplay);
      }
    };

    return (
      <div className="fc-pilled-input__controls-item">
        <div className={buttonClass} onClick={clickAction}>
          <i className="icon-search" />
        </div>
      </div>
    );
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

  componentDidMount() {
    document.addEventListener('click', this.onDocumentClick);

    this.props.submitFilters(this.currentSearch.query, true);
    this.props.fetchSearches();
  }

  componentDidUpdate() {
    if (this.state.shouldSetFocus && !this.isDisabled) {
      this.refs.input.focus();
    }
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
        pills: search.query,
        searchDisplay: search.searchValue,
        optionsVisible: isVisible,
        searchOptions: searchOptions,
      });
    }

    this.setState({
      searchValue: search.searchValue,
    });
  }

  componentWillUnmount() {
    document.removeEventListener('click', this.onDocumentClick);
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
    let searchDisplay = newSearchTerm;

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
  closeShareSearch() {
    this.setState({ isShareVisible: false });
  }

  get shareSearch() {
    if (this.props.singleSearch) {
      return null;
    }

    return (
      <div className="fc-col-md-1-1">
        <Button
          className="fc-live-search__share-button fc-right"
          onClick={this.openShareSearch}
          disabled={this.currentSearch.title === SEARCH_ALL}
          icon="external-link-2" />

        <ShareSearch
          search={this.currentSearch}
          fetchAssociations={this.props.fetchAssociations}
          suggestAssociations={this.props.suggestAssociations}
          associateSearch={this.props.associateSearch}
          dissociateSearch={this.props.dissociateSearch}
          selectItem={this.props.selectItem}
          deselectItem={this.props.deselectItem}
          setTerm={this.props.setTerm}
          closeAction={this.closeShareSearch}
          isVisible={this.state.isShareVisible}
          title={this.currentSearch.title} />
      </div>
    );
  }

  render() {
    const gridClass = classNames('fc-grid', 'fc-list-page-content', {
      'fc-grid-no-gutter': this.props.noGutter
    });
    const tableClass = classNames('fc-col-md-1-1', 'fc-live-search__table', {
      '_no-gutter': this.props.noGutter
    });

    return (
      <div className="fc-live-search">
        {this.header}
        <div className={gridClass}>
          {this.shareSearch}
          <div className="fc-col-md-1-1 fc-live-search__search-control">
            <form>
              <PilledInput
                controls={[this.searchButton, this.searchMenuButton]}
                className={classNames({'_active': this.state.isFocused, '_disabled': this.isDisabled})}
                onPillClose={(pill, idx) => !this.isDisabled && this.deleteFilter(idx)}
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
                  placeholder="filter or keyword search"
                  prepend={this.state.searchPrepend}
                  value={this.state.searchDisplay}
                  disabled={this.isDisabled}
                  ref="input" />
              </PilledInput>
            </form>

            <div>
              {this.searchOptions}
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
